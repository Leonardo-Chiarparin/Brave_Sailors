package com.example.brave_sailors.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.brave_sailors.model.MatchType

object MatchStateStorage {
    private const val PREFS_NAME = "active_match_prefs"

    fun saveState(context: Context, matchId: String, opponent: String, type: MatchType, isP1Turn: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString("match_id", matchId)
            putString("opponent", opponent)
            putString("type", type.name)
            putBoolean("p1_turn", isP1Turn)
        }
    }

    fun updateTurn(context: Context, isP1Turn: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean("p1_turn", isP1Turn)
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            clear()
        }
    }

    data class SavedState(
        val matchId: String,
        val opponent: String,
        val type: MatchType,
        val isP1Turn: Boolean
    )

    fun getState(context: Context): SavedState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString("match_id", null) ?: return null
        val opponent = prefs.getString("opponent", "Unknown") ?: "Unknown"
        val typeStr = prefs.getString("type", null) ?: return null
        val type = try { MatchType.valueOf(typeStr) } catch(e: Exception) { return null }
        val p1Turn = prefs.getBoolean("p1_turn", true)

        return SavedState(id, opponent, type, p1Turn)
    }
}

