package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Tab
import com.example.brave_sailors.ui.theme.*

// Riutilizziamo lo stesso gradiente per coerenza visuale
val OceanGradientStats = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
)

// Gradiente per le card statistiche
val StatCardGradient = Brush.linearGradient(
    colors = listOf(Color.White.copy(0.1f), Color.White.copy(0.05f))
)

// Gradiente per la barra di progresso (Arancione -> Giallo)
val ProgressGradient = Brush.horizontalGradient(
    colors = listOf(Orange, Color(0xFFFFD700))
)

@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    // Static Data
    val totalGames = 42
    val wins = 28
    val losses = 14
    val winRate = 66 // %
    val totalShots = 1250
    val hits = 450
    val accuracy = 36 // %
    val shipsSunk = 115

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(OceanGradientStats)
    ) {
        // --- TOP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f))
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
            GridBackground(
                Modifier.matchParentSize(),
                color = Color.Cyan.copy(alpha = 0.05f),
                dimension = 40f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp)) // Spazio per il Tab fluttuante

                // 1. Overview Cards (Wins / Losses)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "VICTORIES",
                        value = wins.toString(),
                        textColor = Color(0xFFFFD700) // Oro
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "DEFEATS",
                        value = losses.toString(),
                        textColor = Color(0xFFEF5350) // Rosso chiaro
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. Win Rate Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "WIN RATE",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$winRate%",
                            color = Orange,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StatProgressBar(percentage = winRate)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 3. Detailed Stats List
                Text(
                    "COMBAT DATA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Container per la lista dati per dare un fondo unito
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    StatRowItem(
                        icon = Icons.Default.ShowChart,
                        label = "Total Games Played",
                        value = totalGames.toString(),
                        showDivider = true
                    )
                    StatRowItem(
                        icon = Icons.Default.GpsFixed,
                        label = "Accuracy",
                        value = "$accuracy%",
                        showDivider = true
                    )
                    StatRowItem(
                        icon = Icons.Default.BarChart,
                        label = "Total Shots Fired",
                        value = totalShots.toString(),
                        showDivider = true
                    )
                    StatRowItem(
                        icon = Icons.Default.Water,
                        label = "Ships Sunk",
                        value = shipsSunk.toString(),
                        showDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Tab Fluttuante
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp)
                    .zIndex(2f)
            ) {
                Tab(
                    paddingH = 100f,
                    paddingV = 28f,
                    text = "STATISTICS"
                )
            }
        }

        // --- BOTTOM BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}

// --- COMPONENTS ---

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    textColor: Color
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(110.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(StatCardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = textColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatProgressBar(percentage: Int) {
    // Sfondo della barra (Track)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
    ) {
        // Parte piena (Progress)
        Box(
            modifier = Modifier
                .fillMaxWidth(percentage / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(ProgressGradient)
        )
    }
}

@Composable
fun StatRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona in un cerchio
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Orange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = Orange,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (showDivider) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    }
}