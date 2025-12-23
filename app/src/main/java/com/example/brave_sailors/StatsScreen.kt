package com.example.brave_sailors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.theme.*

@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    // Static Data (Mock based on Firebase structure)
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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // 1. Overview Cards (Wins / Losses)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "VICTORIES",
                        value = wins.toString(),
                        color = Orange
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "DEFEATS",
                        value = losses.toString(),
                        color = LightGrey
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Win Rate Bar
                Text("WIN RATE", color = LightGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                StatProgressBar(percentage = winRate, color = Orange)
                Text("$winRate%", color = White, fontSize = 14.sp, modifier = Modifier.align(Alignment.End))

                Spacer(modifier = Modifier.height(32.dp))

                // 3. Detailed Stats List
                Text("COMBAT DATA", color = Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))

                StatRowItem(icon = Icons.Default.ShowChart, label = "Total Games Played", value = totalGames.toString())
                StatRowItem(icon = Icons.Default.GpsFixed, label = "Accuracy", value = "$accuracy%")
                StatRowItem(icon = Icons.Default.BarChart, label = "Total Shots Fired", value = totalShots.toString())
                StatRowItem(icon = Icons.Default.Water, label = "Ships Sunk", value = shipsSunk.toString())

                Spacer(modifier = Modifier.height(32.dp))
            }

            HeaderTab(text = "STATISTICS")
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

// --- COMPONENTS ---

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier.height(100.dp),
        color = Blue,
        shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(label, color = White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatProgressBar(percentage: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Blue)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percentage / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
    }
}

@Composable
fun StatRowItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Blue,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(label, color = White, fontSize = 14.sp, modifier = Modifier.weight(1f))

        Text(value, color = LightGrey, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = LightGrey.copy(alpha = 0.1f))
}