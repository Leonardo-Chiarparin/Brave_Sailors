package com.example.brave_sailors.data.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.FriendDao
import com.example.brave_sailors.data.local.database.MatchDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.FriendEntity
import com.example.brave_sailors.data.local.database.entity.MatchResult
import com.example.brave_sailors.data.local.database.entity.MoveLog
import com.example.brave_sailors.data.local.database.entity.RankingPlayer
import com.example.brave_sailors.data.local.database.entity.SavedShip
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.SailorApi
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val api: SailorApi, // Remote API client
    private val userDao: UserDao, // Room DAO for user data
    private val fleetDao: FleetDao, // Room DAO for saved fleet data
    private val friendDao: FriendDao, // Room DAO for friends
    private val matchDao: MatchDao, // Room DAO for matches
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Firebase Auth instance
) {
    private var currentFriendListener: ValueEventListener? = null
    private var currentFriendRef: com.google.firebase.database.DatabaseReference? = null

    private val database = FirebaseDatabase.getInstance() // Firebase Realtime Database entry point

    // [ LOGIC ]: Scope for background database operations within listeners
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Exposes the Room Flow to the ViewModel so UI can react to local DB changes.
    fun getUserFlow() = userDao.getUserFlow()

    /**
     * Registers a new user, updates Firebase profile, and initializes cloud/local data.
     */
    suspend fun registerUser(email: String, pass: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val firebaseUser = auth.currentUser
                val localUser = userDao.getCurrentUser()

                if (firebaseUser != null) {
                    val credential = EmailAuthProvider.getCredential(email, pass)

                    firebaseUser.linkWithCredential(credential).await()

                    firebaseUser.sendEmailVerification().await()

                    val updatedUser = localUser?.copy(
                        registerEmail = email,
                        password = pass,
                        lastUpdated = System.currentTimeMillis()
                    )

                    if (updatedUser != null) {
                        userDao.insertUser(updatedUser)
                        syncLocalToCloud(updatedUser.id)
                    }

                    return@withContext Result.success(
                        "Account linked successfully! Check your email ($email) to verify."
                    )
                }
                else if (localUser != null) {
                    return@withContext Result.failure(
                        Exception("Security session expired. Please go to Settings -> Access -> Logout, then Login again via Google to refresh your session.")
                    )
                }

                else {
                    val result = auth.createUserWithEmailAndPassword(email, pass).await()
                    val newUser = result.user ?: throw Exception("Creation failed")

                    newUser.sendEmailVerification().await()

                    val userEntity = User(
                        id = newUser.uid,
                        registerEmail = email,
                        password = pass,
                        name = "Sailor",
                        countryCode = "IT",
                        lastUpdated = System.currentTimeMillis()
                    )

                    userDao.insertUser(userEntity)
                    syncLocalToCloud(newUser.uid)

                    return@withContext Result.success("New account created! Check your email.")
                }

            } catch (e: FirebaseAuthUserCollisionException) {
                return@withContext Result.failure(Exception("This email is already associated with another Brave Sailors account."))
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                return@withContext Result.failure(Exception("For security, please Logout and Login again via Google, then try registering immediately."))
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(Exception("Error: ${e.message}"))
            }
        }

    /**
     * Synchronizes current user's profile and fleet data to Firebase.
     */
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
                            "googlePhotoUrl" to user.googlePhotoUrl,
                            "profilePictureUrl" to user.profilePictureUrl,
                            "countryCode" to user.countryCode,
                            "lastUpdated" to user.lastUpdated,
                            "registerEmail" to user.registerEmail,
                            "password" to user.password
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
                    "sessionToken" to user.sessionToken,
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

            Result.success("Recovery email sent successfully! Please check the inbox.")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if email ( registerEmail ) belongs to a password-based user account.
     */
    private suspend fun checkEmailEligibility(email: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val query = database.getReference("users")
                .orderByChild("profile/info/registerEmail")
                .equalTo(email)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists())
                        continuation.resume(true)
                    else
                        continuation.resume(false)
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
                // 1) Firebase Auth login
                val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = authResult.user?.uid ?: throw Exception("Authentication failed.")

                withContext(kotlinx.coroutines.NonCancellable) {
                    clearAllLocalData()

                    val newToken = UUID.randomUUID().toString()

                    // 2) Fetch full user record from Realtime Database
                    val userRef = database.getReference("users").child(uid)
                    val infoRef = userRef.child("profile").child("info")

                    userRef.child("sessionToken").setValue(newToken).await()
                    infoRef.child("registerEmail").setValue(email).await()
                    infoRef.child("password").setValue(pass).await()

                    val snapshot = userRef.get().await()

                    if (!snapshot.exists()) {
                        throw Exception("User could not be found in the database.")
                    }

                    // 3) Parse JSON snapshot into a local User entity
                    val profile = snapshot.child("profile")
                    val info = profile.child("info")
                    val progression = profile.child("progression")
                    val stats = profile.child("statistics")
                    val ranking = profile.child("ranking")

                    val fetchedUser = User(
                        id = uid,
                        name = info.child("name").getValue(String::class.java) ?: "Sailor",
                        profilePictureUrl = info.child("profilePictureUrl").getValue(String::class.java)
                            ?: "",

                        email = info.child("email").getValue(String::class.java) ?: "",
                        googleName = info.child("googleName").getValue(String::class.java) ?: "",
                        googlePhotoUrl = info.child("googlePhotoUrl").getValue(String::class.java)
                            ?: "",

                        registerEmail = info.child("registerEmail").getValue(String::class.java)
                            ?: email,
                        password = info.child("password").getValue(String::class.java) ?: pass,

                        aiAvatarPath = "ic_ai_avatar_placeholder",

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
                        totalShotsFired = stats.child("totalShotsFired").getValue(Long::class.java)
                            ?: 0,
                        totalShotsHit = stats.child("totalShotsHit").getValue(Long::class.java) ?: 0,

                        sessionToken = newToken
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
                    userDao.insertUser(fetchedUser)

                    if (fleetList.isNotEmpty()) {
                        fleetDao.saveFleet(fleetList)
                    }

                    // [ LOGIC ]: Start observing friends changes in real-time to populate local DB
                    observeFriendsLive(uid)
                }

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
        if (currentFriendListener != null && currentFriendRef?.key == "friends" && currentFriendRef?.parent?.key == currentUserId)
            return

        removeFriendListener()

        val friendsRef = database.getReference("users").child(currentUserId).child("friends")
        currentFriendRef = friendsRef

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeFriendIds = snapshot.children.mapNotNull { it.key }

                repositoryScope.launch {
                    if (activeFriendIds.isEmpty())
                        friendDao.deleteAllFriends()
                    else
                        friendDao.deleteFriendsNotIn(activeFriendIds)

                    for (friendSnap in snapshot.children) {
                        val friendId = friendSnap.key ?: continue
                        val status = friendSnap.child("status").getValue(String::class.java) ?: "ACCEPTED"
                        val timestamp = friendSnap.child("timestamp").getValue(Long::class.java) ?: 0L

                        // [ LOGIC ]: Listen to the friend's official profile node to catch updates
                        database.getReference("users")
                            .child(friendId)
                            .child("profile")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(profileSnapshot: DataSnapshot) {
                                    if (!profileSnapshot.exists()) return

                                    val info = profileSnapshot.child("info")
                                    val stats = profileSnapshot.child("statistics")

                                    val name = info.child("name").getValue(String::class.java) ?: "Unknown"
                                    val country = info.child("countryCode").getValue(String::class.java) ?: "IT"
                                    val wins = stats.child("wins").getValue(Long::class.java)?.toInt() ?: 0
                                    val losses = stats.child("losses").getValue(Long::class.java)?.toInt() ?: 0

                                    repositoryScope.launch {
                                        val updatedFriend = FriendEntity(
                                            id = friendId,
                                            name = name,
                                            status = status.uppercase(),
                                            timestamp = timestamp,
                                            countryCode = country,
                                            wins = wins,
                                            losses = losses
                                        )

                                        // [ LOGIC ]: Room INSERT handles the sync between Firebase and Local DB
                                        friendDao.insertFriend(updatedFriend)
                                        Log.d("UserRepository", "Friend data synced/updated live.")
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("UserRepository", "Friend name listener cancelled: ${error.message}")
                                }
                            })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {  }
        }

        currentFriendListener = listener
        friendsRef.addValueEventListener(listener)
    }

    fun removeFriendListener() {
        if (currentFriendListener != null && currentFriendRef != null) {
            currentFriendRef?.removeEventListener(currentFriendListener!!)
            currentFriendListener = null
            currentFriendRef = null
        }
    }

    /**
     * Deletes the account and associated data from cloud and local storage.
     */
    suspend fun deleteAccount(user: User): Result<String> {
        // Check for integrity
        if (user.registerEmail.isNullOrEmpty() || user.password.isNullOrEmpty()) {
            return Result.failure(Exception("The account has not been registered yet, therefore it cannot be deleted."))
        }

        return try {
            val firebaseUser = auth.currentUser ?: return Result.failure(Exception("No user is currently logged in."))
            val userRef = database.getReference("users").child(user.id)

            // 1. Attempt re-authentication using the LOCALLY stored password
            val credential = EmailAuthProvider.getCredential(user.registerEmail, user.password)

            // 2. This will throw an exception if the password has been changed externally
            firebaseUser.reauthenticate(credential).await()

            // --- Authenticated successfully. Proceed with deletion. ---

            // Remove friend references (Cloud)
            val friendsSnapshot = userRef.child("friends").get().await()
            if (friendsSnapshot.exists()) {
                for (friend in friendsSnapshot.children) {
                    val friendId = friend.key ?: continue
                    try {
                        database.getReference("users")
                            .child(friendId).child("friends").child(user.id)
                            .removeValue()
                    } catch (e: Exception) { Log.e("UserRepo", "Error deleting friend ref: $friendId") }
                }
            }

            // Remove User Data (Cloud)
            userRef.removeValue().await()

            // Delete Auth Account (Cloud)
            firebaseUser.delete().await()

            // Clear Local Data
            clearAllLocalData()
            auth.signOut()

            Result.success("Account deleted successfully.")

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // [ NOTE ]: User reset password via email, but app has the old one locally.
            // ERROR MESSAGE: Tells the user the credentials don't match anymore.
            return Result.failure(
                Exception("The password have been changed externally. Please \"Access\" once more to verify your identity, then retry.")
            )

        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            // [ NOTE ]: Session is too old for sensitive actions.
            // ERROR MESSAGE: Tells the user to refresh the session.
            return Result.failure(
                Exception("For security reasons, this action requires a recent sign-in. Please \"Access\" once more to refresh the session.")
            )

        } catch (e: Exception) {
            // Generic fallback
            e.printStackTrace()
            return Result.failure(Exception("Delete failed: ${e.message}"))
        }
    }

    /**
     * Establishes a bidirectional friendship between the sender and the target.
     */
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

    /**
     * Observes and returns the real-time leaderboard ranking from Firebase.
     */
    fun getLeaderboard(): Flow<List<RankingPlayer>> = callbackFlow {
        // 1. Point to the "users" node
        // 2. Sort upon the "totalScore" parameter
        // 3. Retrieve the last 25 (the higher amount of points is on the bottom)
        val query = database.getReference("users")
            .orderByChild("profile/ranking/totalScore")
            .limitToLast(25)

        val listener = object : ValueEventListener {
            @SuppressLint("DefaultLocale")
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

    /**
     * Updates XP and match-related statistics for the specified user.
     */
    private suspend fun updateUserStatistics(userId: String, isVictory: Boolean, shotsFired: Long, shotsHit: Long, shipsSunk: Int) {
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

            shipsDestroyed = user.shipsDestroyed + shipsSunk,

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

            val shipsSunkInMatch = userMoves.count { it.result == "SUNK" }

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

            updateUserStatistics(userId, isVictory, total.toLong(), hits.toLong(), shipsSunkInMatch)
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

    suspend fun clearAllLocalData() {
        fleetDao.deleteAllFleets()
        friendDao.deleteAllFriends()

        matchDao.deleteAllMatches()
        matchDao.deleteAllMoves()

        userDao.deleteAllUsers()
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