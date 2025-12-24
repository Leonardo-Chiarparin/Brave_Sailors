package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Tab // Assicurati di avere questo componente dal Profilo
import com.example.brave_sailors.ui.theme.*

// Riutilizziamo il gradiente definito per il profilo per coerenza
val OceanGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
)

val CardGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE).copy(alpha = 0.3f))
)

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(OceanGradient) // Nuovo sfondo
    ) {
        // --- TOP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f)) // Sostituisce BarPattern
        ) {
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
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Sfondo griglia leggero
            GridBackground(
                modifier = Modifier.matchParentSize(),
                color = Color.Cyan.copy(alpha = 0.05f),
                dimension = 40f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp), // Padding leggermente ridotto per più spazio
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp)) // Spazio per la Tab

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RANK", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("PLAYER", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("SCORE", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Player List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(staticLeaderboardData) { index, item ->
                        LeaderboardItem(rank = index + 1, name = item.first, score = item.second)
                    }
                }
            }

            // --- TAB (Floating Header) ---
            // Sostituisce HeaderTab ed è posizionato sopra (zIndex)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .zIndex(2f)
            ) {
                Tab(
                    paddingH = 100f,
                    paddingV = 28f,
                    text = "LEADERBOARD"
                )
            }
        }

        // --- BOTTOM BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f)) // Sostituisce BarPattern
        )
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
    val shape = RoundedCornerShape(16.dp)

    // Definiamo colori speciali per i primi 3
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Oro
        2 -> Color(0xFFC0C0C0) // Argento
        3 -> Color(0xFFCD7F32) // Bronzo
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(CardGradient) // Sfondo gradiente per la card
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Rank + Avatar + Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Rank
                Text(
                    text = "#$rank",
                    color = rankColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.width(40.dp)
                )

                // 2. Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, rankColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 3. Name
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right Group: Score
            Text(
                text = score.toString(),
                color = Orange,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}