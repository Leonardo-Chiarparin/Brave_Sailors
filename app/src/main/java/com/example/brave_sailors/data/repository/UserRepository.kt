package com.example.brave_sailors.data.repository

import android.util.Log
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.FriendDao
import com.example.brave_sailors.data.local.database.UserDao

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.brave_sailors.data.local.database.entity.FriendEntity
import com.example.brave_sailors.data.local.database.entity.RankingPlayer
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.SailorApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val api: SailorApi, // Remote API client
    private val userDao: UserDao, // Room DAO for user data
    private val fleetDao: FleetDao, // Room DAO for saved fleet data
    private val friendDao: FriendDao, // Room DAO for friends
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Firebase Auth instance
) {

    private val database = FirebaseDatabase.getInstance() // Firebase Realtime Database entry point

    // Exposes the Room Flow to the ViewModel so UI can react to local DB changes.
    fun getUserFlow() = userDao.getUserFlow()

    // Registers a new user with email/password in Firebase Auth.
    suspend fun registerUser(email: String, pass: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1) Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user ?: throw Exception("Error while creating user.")

                // 2) Update Firebase profile display name
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }

                firebaseUser.updateProfile(profileUpdates).await()

                // 3) Send email verification
                firebaseUser.sendEmailVerification().await()

                // 4) Create local user entity with defaults
                val newUser = User(
                    id = firebaseUser.uid,
                    email = email,
                    name = name,
                    profilePictureUrl = null,
                    googleName = "",
                    googlePhotoUrl = null,
                    countryCode = "IT",
                    lastUpdated = System.currentTimeMillis(),
                    level = 1,
                    currentXp = 0
                )

                // 5) Enforce "single user stored locally" model: wipe and insert
                userDao.deleteAllUsers()
                userDao.insertUser(newUser)

                // 6) Push local state to the cloud
                syncLocalToCloud(newUser.id)

                return@withContext Result.success(
                    "Registration complete! Check your email ($email) to verify the account."
                )

            } catch (e: FirebaseAuthUserCollisionException) {
                // Firebase throws this when the email already exists
                return@withContext Result.failure(Exception("This email is already associated with another account."))
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Error during registration: ${e.message}"))
            }
        }

    // Uploads local Room data (user profile + fleet) to Firebase Realtime Database.
    suspend fun syncLocalToCloud(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Load local user; if missing, nothing to sync
                val user = userDao.getUserById(userId) ?: return@withContext

                // Cloud reference: /users/{uid}
                val userRef = database.getReference("users").child(user.id)

                // Build a JSON-like map structure matching the desired Realtime DB schema.
                val userData = mapOf(
                    "profile" to mapOf(
                        "info" to mapOf(
                            "email" to user.email,
                            "name" to user.name,
                            "googleName" to user.googleName,
                            "profilePictureUrl" to (user.profilePictureUrl ?: ""),
                            "countryCode" to (user.countryCode ?: "IT"),
                            "lastUpdated" to user.lastUpdated
                        ),
                        "progression" to mapOf(
                            "level" to user.level,
                            "currentXp" to user.currentXp,
                            "lastWinTimestamp" to user.lastWinTimestamp
                        ),
                        "ranking" to mapOf(
                            "totalScore" to user.totalScore
                        ),
                        "statistics" to mapOf(
                            "wins" to user.wins,
                            "losses" to user.losses,
                            "shipsDestroyed" to user.shipsDestroyed,
                            "totalShotsFired" to user.totalShotsFired,
                            "totalShotsHit" to user.totalShotsHit
                        )
                    ),
                    // Fleet is stored as a list of ship maps under /users/{uid}/fleet
                    "fleet" to fleetDao.getUserFleet(userId).map { ship ->
                        mapOf(
                            "shipId" to ship.shipId,
                            "size" to ship.size,
                            "row" to ship.row,
                            "col" to ship.col,
                            "isHorizontal" to ship.isHorizontal
                        )
                    },
                    "lastSyncTimestamp" to System.currentTimeMillis()
                )

                // Merge the data into the cloud record
                userRef.updateChildren(userData).await()
                Log.d("UserRepository", "Cloud Sync completed for ${user.id}")

            } catch (e: Exception) {
                Log.e("UserRepository", "Sync failed: ${e.message}")
            }
        }
    }

    // Send a Firebase password reset email
    suspend fun resetPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanEmail = email.trim().lowercase()

            val isEligible = checkEmailEligibility(cleanEmail)
            if (!isEligible) {
                return@withContext Result.failure(
                    Exception("The email provided does not match any registered password-based account.")
                )
            }

            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success("Recovery email sent successfully! Please check your inbox.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Checks whether the email corresponds to a password-based account.
    private suspend fun checkEmailEligibility(email: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val query = database.getReference("users")
                .orderByChild("profile/info/email")
                .equalTo(email)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        continuation.resume(false)
                        return
                    }

                    for (child in snapshot.children) {
                        val googleName =
                            child.child("profile/info/googleName").getValue(String::class.java)

                        if (!googleName.isNullOrEmpty()) {
                            continuation.resume(false)
                        } else {
                            continuation.resume(true)
                        }
                        return
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }

    // Logs in using Firebase Auth (email/password), then pulls the full profile + fleet
    suspend fun loginUser(email: String, pass: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1) Firebase Auth login
                val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user ?: throw Exception("Auth failed: User not found.")
                val uid = firebaseUser.uid

                // 2) Fetch full user record from Realtime Database
                val userRef = database.getReference("users").child(uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    throw Exception("User profile not found in database.")
                }

                // 3) Parse JSON snapshot into a local User entity
                val profile = snapshot.child("profile")
                val info = profile.child("info")
                val progression = profile.child("progression")
                val stats = profile.child("statistics")
                val ranking = profile.child("ranking")

                val fetchedUser = User(
                    id = uid,
                    email = info.child("email").getValue(String::class.java) ?: email,
                    name = info.child("name").getValue(String::class.java) ?: "Sailor",
                    googleName = info.child("googleName").getValue(String::class.java) ?: "",
                    googlePhotoUrl = info.child("profilePictureUrl").getValue(String::class.java),
                    countryCode = info.child("countryCode").getValue(String::class.java) ?: "IT",
                    lastUpdated = info.child("lastUpdated").getValue(Long::class.java)
                        ?: System.currentTimeMillis(),

                    level = progression.child("level").getValue(Int::class.java) ?: 1,
                    currentXp = progression.child("currentXp").getValue(Int::class.java) ?: 0,
                    lastWinTimestamp = progression.child("lastWinTimestamp")
                        .getValue(Long::class.java) ?: 0L,

                    totalScore = ranking.child("totalScore").getValue(Long::class.java) ?: 0,

                    wins = stats.child("wins").getValue(Int::class.java) ?: 0,
                    losses = stats.child("losses").getValue(Int::class.java) ?: 0,
                    shipsDestroyed = stats.child("shipsDestroyed").getValue(Int::class.java) ?: 0,
                    totalShotsFired = stats.child("totalShotsFired").getValue(Long::class.java) ?: 0,
                    totalShotsHit = stats.child("totalShotsHit").getValue(Long::class.java) ?: 0,

                    profilePictureUrl = info.child("profilePictureUrl").getValue(String::class.java),
                    sessionToken = null
                )

                // 4) Parse fleet snapshot into Room entity SavedShip
                val fleetSnapshot = snapshot.child("fleet")
                val fleetList = mutableListOf<SavedShip>()

                for (shipSnap in fleetSnapshot.children) {
                    val ship = SavedShip(
                        userId = uid,
                        shipId = (shipSnap.child("shipId").value as? Number)?.toInt() ?: 0,
                        size = (shipSnap.child("size").value as? Number)?.toInt() ?: 1,
                        row = (shipSnap.child("row").value as? Number)?.toInt() ?: 0,
                        col = (shipSnap.child("col").value as? Number)?.toInt() ?: 0,
                        isHorizontal = shipSnap.child("isHorizontal").getValue(Boolean::class.java)
                            ?: true
                    )
                    fleetList.add(ship)
                }

                // 5) Overwrite local state
                userDao.deleteAllUsers()
                userDao.insertUser(fetchedUser)

                if (fleetList.isNotEmpty()) {
                    fleetDao.saveFleet(fleetList)
                }

                return@withContext Result.success("Login successful! Data synchronized.")
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Login error: ${e.message}"))
            }
        }

    // Deletes the account across Firebase DB, Auth, and Local Room DB
    suspend fun deleteAccount(user: User): Result<String> {
        return try {
            val userRef = database.getReference("users").child(user.id)
            userRef.removeValue().await()
            Log.d("UserRepository", "Firebase DB data deleted for user: ${user.id}")

            val firebaseUser = auth.currentUser
            firebaseUser?.delete()?.await() ?: Log.w("UserRepository", "No Auth user found to delete")

            fleetDao.clearUserFleet(user.id)
            userDao.delete(user)
            auth.signOut()

            Result.success("Account deleted successfully both locally and remotely.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Delete failed: ${e.message}")
            Result.failure(e)
        }
    }

    // UPDATED: Now adds the friend directly with status "accepted" immediately.
    // It creates a bidirectional link (Sender -> Target AND Target -> Sender).
    suspend fun sendFriendRequest(sender: User, targetId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Check if the target ID exists in Firebase
            val targetRef = database.getReference("users").child(targetId)
            val snapshot = targetRef.get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Player's ID '$targetId' does not exist."))
            }

            // 2. Get target details for the sender's list
            val targetName = snapshot.child("profile/info/name").getValue(String::class.java) ?: "Unknown Sailor"
            val timestamp = System.currentTimeMillis()

            // 3. Write to Firebase (Sender's friend list -> Target ID)
            // Status is "accepted" immediately.
            val friendDataForSender = mapOf(
                "name" to targetName,
                "status" to "accepted",
                "timestamp" to timestamp
            )

            database.getReference("users")
                .child(sender.id)
                .child("friends")
                .child(targetId)
                .setValue(friendDataForSender)
                .await()

            // 4. Write to Firebase (Target's friend list -> Sender ID) - Reciprocal
            val friendDataForTarget = mapOf(
                "name" to sender.name,
                "status" to "accepted",
                "timestamp" to timestamp
            )

            database.getReference("users")
                .child(targetId)
                .child("friends")
                .child(sender.id)
                .setValue(friendDataForTarget)
                .await()

            // 5. Save the friend locally in Room so it appears in the UI immediately
            val localFriend = FriendEntity(
                id = targetId,
                name = targetName,
                status = "ACCEPTED",
                timestamp = timestamp
            )
            friendDao.insertFriend(localFriend)

            Result.success("Friend '$targetName' added successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getLeaderboard(): Flow<List<RankingPlayer>> = callbackFlow {
        // 1. Point to the "users" node
        // 2. Sort upon the "totalScore" parameter
        // 3. Retrieve the last 25 (the higher amount of points is on the bottom)
        val query = database.getReference("users")
            .orderByChild("profile/ranking/totalScore")
            .limitToLast(25)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<RankingPlayer>()

                // Firebase returns the data in ascending order (0 -> 100 -> 1000)
                for (child in snapshot.children) {
                    val uid = child.key ?: ""

                    val name = child.child("profile/info/name").getValue(String::class.java)
                        ?: child.child("name").getValue(String::class.java) // Fallback root
                        ?: "Unknown Sailor"

                    val countryCode = child.child("profile/info/countryCode").getValue(String::class.java) ?: "IT"
                    val avatarUrl = child.child("profile/info/profilePictureUrl").getValue(String::class.java)
                    val scoreLong = child.child("profile/ranking/totalScore").getValue(Long::class.java) ?: 0L

                    // Adjust the format removing commas (es. 10.000)
                    val formattedScore = String.format("%,d", scoreLong).replace(',', '.')

                    tempList.add(
                        RankingPlayer(
                            rank = 0,
                            id = uid,
                            name = name,
                            score = formattedScore,
                            countryCode = countryCode,
                            avatarUrl = avatarUrl
                        )
                    )
                }

                val finalList = tempList.reversed().mapIndexed { index, player ->
                    player.copy(rank = index + 1)
                }

                trySend(finalList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
}