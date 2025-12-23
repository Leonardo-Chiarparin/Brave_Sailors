package com.example.brave_sailors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.*

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(DeepBlue)
    ) {
        // --- TOP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp.dp * 0.12f))
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, top = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Orange
                )
            }
        }

        // --- CONTENT ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkBlue),
            contentAlignment = Alignment.TopCenter
        ) {
            GridBackground(Modifier.matchParentSize(), color = LightGrey.copy(alpha = 0.1f), dimension = 40f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RANK", color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("PLAYER", color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("SCORE", color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Player List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(staticLeaderboardData) { index, item ->
                        LeaderboardItem(rank = index + 1, name = item.first, score = item.second)
                    }
                }
            }
            HeaderTab(text = "LEADERBOARD")
        }

        // --- BOTTOM BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp.dp * 0.12f))
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }
    }
}

val staticLeaderboardData = listOf(
    "BlackBeard" to 15420,
    "RedRackham" to 12350,
    "JackSparrow" to 11000,
    "AnneBonny" to 9800,
    "Hook" to 8540,
    "Silver" to 7200,
    "Drake" to 6500,
    "Morgan" to 5400
)

@Composable
fun LeaderboardItem(rank: Int, name: String, score: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(70.dp), // Slightly increased height to accommodate the avatar
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        color = Blue,
        border = BorderStroke(1.dp, TransparentGrey)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Rank + Avatar + Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Rank
                Text(
                    text = "#$rank",
                    color = if (rank <= 3) Orange else White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(35.dp) // Reduced slightly to make space
                )

                // 2. Static Avatar (Circle with Icon)
                Box(
                    modifier = Modifier
                        .size(40.dp) // Avatar size
                        .clip(CircleShape)
                        .background(DarkBlue), // Dark background for contrast
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = LightGrey,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 3. Name
                Text(
                    text = name,
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right Group: Score
            Text(
                text = score.toString(),
                color = Orange,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}