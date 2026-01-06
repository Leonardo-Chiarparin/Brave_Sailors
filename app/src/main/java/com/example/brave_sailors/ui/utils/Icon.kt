package com.example.brave_sailors.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.brave_sailors.ui.theme.LightGrey
import com.example.brave_sailors.ui.theme.White

// -- DATA STRUCTURES ( regarding the game modes ) --
// [ TO - DO ]: Add the remaining ones according to the application's purpose
enum class OpponentType {
    Computer,   // Single Player
    Human,      // Local Multiplayer
    Search      // Lobby
}

data class Mode(
    val title: String,
    val sub: String,
    val button: String,
    val opponent: OpponentType
)

val gameModes = listOf(
    Mode(
        title = "Single player",
        sub = "vs. computer",
        button = "Train skills",
        opponent = OpponentType.Computer
    ),
    Mode(
        title = "Player 1 vs. player 2",
        sub = "on one device",
        button = "Start game",
        opponent = OpponentType.Human
    ),
    Mode(
        title = "Play with friends",
        sub = "online vs. kith",
        button = "Lobby",
        opponent = OpponentType.Search
    )
)

@Composable
fun GameModeIcons() {
    val scale = RememberScaleConversion()

    val barSize = scale.dp(90f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barSize)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = White,
            modifier = Modifier
                .size(barSize)
                .align(Alignment.CenterStart)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.Center),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Public,
                    null,
                    tint = LightGrey.copy(alpha = 0.75f),
                    modifier = Modifier.size(scale.dp(66f)).offset(y = scale.dp(8f))
                )

                Icon(
                    Icons.Default.Flag,
                    null,
                    tint = White,
                    modifier = Modifier.size(scale.dp(46f)).offset(y = scale.dp(-12f))
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = White,
            modifier = Modifier
                .size(barSize)
                .align(Alignment.CenterEnd)
        )
    }
}