
package com.example.brave_sailors.data.repository

import android.util.Log
import com.example.brave_sailors.data.local.database.FleetDao
import com.example.brave_sailors.data.local.database.UserDao
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.SailorApi
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val api: SailorApi,
    private val userDao: UserDao,
    private val fleetDao: FleetDao
) {

    private val database = FirebaseDatabase.getInstance()

    /**
     * Synchronizes all local user data and fleet layout to the Firebase Cloud.
     * Includes statistics, progression, and ranking scores.
     */
    suspend fun syncLocalToCloud(userOrId: Any) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Identify the user
                val user = when (userOrId) {
                    is User -> userOrId
                    is String -> userDao.getUserById(userOrId)
                    else -> null
                }

                if (user == null) {
                    Log.e("UserRepository", "Sync failed: User not found")
                    return@withContext
                }

                val userId = user.id
                val userRef = database.getReference("users").child(userId)

                // 2. Map all fields from the User entity (including your new stats)
                val userData = mapOf(
                    "info" to mapOf(
                        "id" to user.id,
                        "name" to user.name,
                        "email" to user.email,
                        "countryCode" to (user.countryCode ?: ""),
                        "profilePictureUrl" to (user.profilePictureUrl ?: ""),
                        "lastUpdated" to user.lastUpdated,
                        "sessionToken" to (user.sessionToken ?: "")
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
                )

                // 3. Fetch local fleet to include in the sync
                val fleetList = fleetDao.getUserFleet(userId)
                val fleetData = fleetList.map { ship ->
                    mapOf(
                        "shipId" to ship.shipId,
                        "size" to ship.size,
                        "row" to ship.row,
                        "col" to ship.col,
                        "isHorizontal" to ship.isHorizontal
                    )
                }

                // 4. Create the final atomic update map
                val updates = hashMapOf<String, Any>(
                    "profile" to userData,
                    "fleet" to fleetData,
                    "lastSyncTimestamp" to System.currentTimeMillis()
                )

                // 5. Push to Firebase
                userRef.updateChildren(updates).await()
                Log.d("UserRepository", "Full cloud sync successful for user: $userId")

            } catch (e: Exception) {
                Log.e("UserRepository", "Sync failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}