package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.brave_sailors.BarPattern
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Tab
import com.example.brave_sailors.ui.theme.*

@Composable
fun MenuScreen(
    onGameOptions: () -> Unit = {},
    onSettings: () -> Unit = {},
    onInstructions: () -> Unit = {}
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlue)
    ) {

        // ───────── TOP BAR ─────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }

        // ───────── CONTENT ─────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkBlue),
            contentAlignment = Alignment.TopCenter
        ) {
            GridBackground(
                modifier = Modifier.matchParentSize(),
                color = LightGrey.copy(alpha = 0.1f),
                dimension = 40f
            )

            // ───── MAIN CONTENT ─────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(96.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    MenuButton(
                        text = "GAME OPTIONS",
                        icon = Icons.Default.VideogameAsset,
                        onClick = onGameOptions
                    )

                    MenuButton(
                        text = "SETTINGS",
                        icon = Icons.Default.Settings,
                        onClick = onSettings
                    )

                    MenuButton(
                        text = "INSTRUCTIONS",
                        icon = Icons.Default.HelpOutline,
                        onClick = onInstructions
                    )
                }
            }

            // ───────── TAB ─────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
            ) {
                Tab(
                    paddingH = 130f,
                    paddingV = 32f,
                    text = "MENU"
                )
            }
        }

        // ───────── BOTTOM BAR ─────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }
    }
}


@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cornerSize = 14.dp
    val shape = CutCornerShape(cornerSize)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(shape)
            .background(Blue)
            .clickable { onClick() }
            .drawBehind {
                val strokeWidth = 1.2.dp.toPx()
                val cornerPx = cornerSize.toPx()

                // Bordo HUD con gradiente verticale
                val brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    (cornerPx / size.height) to White,
                    (size.height - cornerPx) / size.height to White,
                    1.0f to Color.Transparent
                )

                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    brush = brush,
                    style = Stroke(width = strokeWidth)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Orange,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = text,
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}
