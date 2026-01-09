package com.example.brave_sailors.data.repository

import android.util.Log
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.FriendDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.MatchDao
import com.example.brave_sailors.data.local.database.entity.MatchResult
import com.example.brave_sailors.data.local.database.entity.MoveLog
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val api: SailorApi,
    private val userDao: UserDao,
    private val fleetDao: FleetDao,
    private val friendDao: FriendDao,
    private val matchDao: MatchDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val database = FirebaseDatabase.getInstance()
    // [ LOGIC ]: Scope for background database operations within listeners
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // [ LOGIC ]: Returns a flow of the current user from local database
    fun getUserFlow() = userDao.getUserFlow()

    /**
     * Registers a new user, updates Firebase profile, and initializes cloud/local data.
     */
    suspend fun registerUser(email: String, pass: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user ?: throw Exception("Error while creating user.")

                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }

                firebaseUser.updateProfile(profileUpdates).await()
                firebaseUser.sendEmailVerification().await()

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

                userDao.deleteAllUsers()
                userDao.insertUser(newUser)
                syncLocalToCloud(newUser.id)

                return@withContext Result.success(
                    "Registration complete! Check your email ($email) to verify the account."
                )

            } catch (e: FirebaseAuthUserCollisionException) {
                return@withContext Result.failure(Exception("This email is already associated with another account."))
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Error during registration: ${e.message}"))
            }
        }

    /**
     * Synchronizes current user's profile and fleet data to Firebase.
     */
    suspend fun syncLocalToCloud(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUserById(userId) ?: return@withContext
                val userRef = database.getReference("users").child(user.id)

                if (user.wins == 0 && user.losses == 0 && user.currentXp == 0) {
                    Log.w("UserRepository", "Prevented sync of empty local data for user: ${user.id}")
                    return@withContext
                }
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

                userRef.updateChildren(userData).await()
                Log.d("UserRepository", "Cloud Sync completed for ${user.id}")

            } catch (e: Exception) {
                Log.e("UserRepository", "Sync failed: ${e.message}")
            }
        }
    }

    /**
     * Sends a password reset email if account eligibility is confirmed.
     */
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

    /**
     * Checks if email belongs to a password-based user account.
     */
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

    /**
     * Logs in the user, parses profile/fleet, and initializes the live friends name observer.
     */
    suspend fun loginUser(email: String, pass: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user ?: throw Exception("Auth failed: User not found.")
                val uid = firebaseUser.uid

                val userRef = database.getReference("users").child(uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    throw Exception("User profile not found in database.")
                }

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
                    lastUpdated = info.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis(),
                    level = progression.child("level").getValue(Int::class.java) ?: 1,
                    currentXp = progression.child("currentXp").getValue(Int::class.java) ?: 0,
                    lastWinTimestamp = progression.child("lastWinTimestamp").getValue(Long::class.java) ?: 0L,
                    totalScore = ranking.child("totalScore").getValue(Long::class.java) ?: 0,
                    wins = stats.child("wins").getValue(Int::class.java) ?: 0,
                    losses = stats.child("losses").getValue(Int::class.java) ?: 0,
                    shipsDestroyed = stats.child("shipsDestroyed").getValue(Int::class.java) ?: 0,
                    totalShotsFired = stats.child("totalShotsFired").getValue(Long::class.java) ?: 0,
                    totalShotsHit = stats.child("totalShotsHit").getValue(Long::class.java) ?: 0,
                    profilePictureUrl = info.child("profilePictureUrl").getValue(String::class.java),
                    sessionToken = null
                )

                // [ LOGIC ]: Sync fleet from cloud to local
                val fleetSnapshot = snapshot.child("fleet")
                val fleetList = mutableListOf<SavedShip>()
                for (shipSnap in fleetSnapshot.children) {
                    val ship = SavedShip(
                        userId = uid,
                        shipId = (shipSnap.child("shipId").value as? Number)?.toInt() ?: 0,
                        size = (shipSnap.child("size").value as? Number)?.toInt() ?: 1,
                        row = (shipSnap.child("row").value as? Number)?.toInt() ?: 0,
                        col = (shipSnap.child("col").value as? Number)?.toInt() ?: 0,
                        isHorizontal = shipSnap.child("isHorizontal").getValue(Boolean::class.java) ?: true
                    )
                    fleetList.add(ship)
                }

                userDao.deleteAllUsers()
                userDao.insertUser(fetchedUser)

                if (fleetList.isNotEmpty()) {
                    fleetDao.saveFleet(fleetList)
                }

                // [ LOGIC ]: Start observing friends changes in real-time to populate local DB
                observeFriendsLive(uid)

                return@withContext Result.success("Login successful! Data synchronized.")
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Login error: ${e.message}"))
            }
        }

    /**
     * Attaches live listeners to each friend's profile to update names locally
     * as soon as they change their username on their own account.
     */
    fun observeFriendsLive(currentUserId: String) {
        val friendsRef = database.getReference("users").child(currentUserId).child("friends")

        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (friendSnap in snapshot.children) {
                    val friendId = friendSnap.key ?: continue
                    val status = friendSnap.child("status").getValue(String::class.java) ?: "ACCEPTED"
                    val timestamp = friendSnap.child("timestamp").getValue(Long::class.java) ?: 0L

                    // [ LOGIC ]: Listen to the friend's official profile name node to catch updates
                    database.getReference("users")
                        .child(friendId)
                        .child("profile/info/name")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(nameSnapshot: DataSnapshot) {
                                val currentName = nameSnapshot.getValue(String::class.java) ?: "Unknown Sailor"

                                repositoryScope.launch {
                                    val updatedFriend = FriendEntity(
                                        id = friendId,
                                        name = currentName,
                                        status = status.uppercase(),
                                        timestamp = timestamp
                                    )
                                    // [ LOGIC ]: Room INSERT handles the sync between Firebase and Local DB
                                    friendDao.insertFriend(updatedFriend)
                                    Log.d("UserRepository", "Friend data synced/updated live: $currentName")
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("UserRepository", "Friend name listener cancelled: ${error.message}")
                            }
                        })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepository", "Friends list listener cancelled: ${error.message}")
            }
        })
    }

    /**
     * Deletes the account and associated data from cloud and local storage.
     */
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

    /**
     * Establishes a bidirectional friendship between the sender and the target.
     */
    suspend fun sendFriendRequest(sender: User, targetId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetRef = database.getReference("users").child(targetId)
            val snapshot = targetRef.get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Player's ID '$targetId' does not exist."))
            }

            val targetName = snapshot.child("profile/info/name").getValue(String::class.java) ?: "Unknown Sailor"
            val timestamp = System.currentTimeMillis()

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

    /**
     * Observes and returns the real-time leaderboard ranking from Firebase.
     */
    fun getLeaderboard(): Flow<List<RankingPlayer>> = callbackFlow {
        val query = database.getReference("users")
            .orderByChild("profile/ranking/totalScore")
            .limitToLast(25)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<RankingPlayer>()

                for (child in snapshot.children) {
                    val uid = child.key ?: ""

                    val name = child.child("profile/info/name").getValue(String::class.java)
                        ?: child.child("name").getValue(String::class.java)
                        ?: "Unknown Sailor"

                    val countryCode = child.child("profile/info/countryCode").getValue(String::class.java) ?: "IT"
                    val avatarUrl = child.child("profile/info/profilePictureUrl").getValue(String::class.java)
                    val scoreLong = child.child("profile/ranking/totalScore").getValue(Long::class.java) ?: 0L

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

    /**
     * Updates XP and match-related statistics for the specified user.
     */
    private suspend fun updateUserStatistics(userId: String, isVictory: Boolean, shotsFired: Long, shotsHit: Long) {
        val user = userDao.getUserById(userId) ?: return

        val xpEarned = if (isVictory) 10 else 0
        val newXp = user.currentXp + xpEarned

        val updatedUser = user.copy(
            wins = if (isVictory) user.wins + 1 else user.wins,
            losses = if (!isVictory) user.losses + 1 else user.losses,

            currentXp = newXp,
            totalScore = newXp.toLong(),

            totalShotsFired = user.totalShotsFired + shotsFired,
            totalShotsHit = user.totalShotsHit + shotsHit,
            lastUpdated = System.currentTimeMillis(),
            lastWinTimestamp = if (isVictory) System.currentTimeMillis() else user.lastWinTimestamp
        )

        userDao.updateUser(updatedUser)
        syncLocalToCloud(userId)
    }

    /**
     * Records match history and statistics locally and triggers cloud synchronization.
     */
    suspend fun saveMatchRecord(
        userId: String,
        opponentName: String,
        isVictory: Boolean,
        difficulty: String,
        moves: List<MoveLog>
    ) {
        withContext(Dispatchers.IO) {
            val userMoves = moves.filter { it.playerId == userId }
            val hits = userMoves.count { it.result == "HIT" || it.result == "SUNK" }
            val misses = userMoves.count { it.result == "MISS" }
            val total = userMoves.size
            val accuracy = if (total > 0) (hits.toFloat() / total) * 100f else 0f

            val matchResult = MatchResult(
                player1Id = userId,
                opponentName = opponentName,
                isVictory = isVictory,
                difficulty = difficulty,
                totalMoves = total,
                totalHits = hits,
                totalMisses = misses,
                accuracy = accuracy
            )

            matchDao.saveFullMatch(matchResult, moves)
            updateUserStatistics(userId, isVictory, total.toLong(), hits.toLong())
        }
    }

    /**
     * Uploads the player's fleet configuration to the active match node in Firebase.
     */
    suspend fun uploadFleetToMatch(matchId: String, userId: String) = withContext(Dispatchers.IO) {
        try {
            val localFleet = fleetDao.getUserFleet(userId)
            val fleetRef = database.getReference("matches").child(matchId).child("fleets").child(userId)

            val fleetData = localFleet.map { ship ->
                mapOf(
                    "shipId" to ship.shipId,
                    "size" to ship.size,
                    "row" to ship.row,
                    "col" to ship.col,
                    "isHorizontal" to ship.isHorizontal
                )
            }

            fleetRef.setValue(fleetData).await()
            Log.d("UserRepository", "Fleet uploaded to match $matchId for user $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to upload fleet: ${e.message}")
            throw e
        }
    }

    /**
     * Listens for the opponent's fleet to become available in the cloud match node.
     */
    fun listenForOpponentFleet(matchId: String, opponentId: String): Flow<List<SavedShip>> = callbackFlow {
        val opponentFleetRef = database.getReference("matches")
            .child(matchId)
            .child("fleets")
            .child(opponentId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val ships = mutableListOf<SavedShip>()
                    for (shipSnap in snapshot.children) {
                        val ship = SavedShip(
                            userId = opponentId,
                            shipId = (shipSnap.child("shipId").value as? Number)?.toInt() ?: 0,
                            size = (shipSnap.child("size").value as? Number)?.toInt() ?: 1,
                            row = (shipSnap.child("row").value as? Number)?.toInt() ?: 0,
                            col = (shipSnap.child("col").value as? Number)?.toInt() ?: 0,
                            isHorizontal = shipSnap.child("isHorizontal").getValue(Boolean::class.java) ?: true
                        )
                        ships.add(ship)
                    }
                    trySend(ships)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        opponentFleetRef.addValueEventListener(listener)
        awaitClose { opponentFleetRef.removeEventListener(listener) }
    }
}