package com.example.brave_sailors

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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


val ButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF8008), Color(0xFFFFC837)) // Arancione -> Oro
)
val ActiveChipGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)) // Azzurro brillante
)

@Composable
fun GameOptionsScreen(
    onStartBattle: () -> Unit = {}
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    var gameMode by remember { mutableStateOf("Fleet Battle") }
    var difficulty by remember { mutableStateOf("Normal") }
    var gridSize by remember { mutableStateOf("10 x 10") }
    var timeLimit by remember { mutableStateOf("No Limit") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanGradient) // Sfondo gradiente invece di tinta piatta
    ) {
        // ───── TOP BAR ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f)) // Leggermente scuro per contrasto
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }

        // ───── CONTENT ─────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {

            GridBackground(
                modifier = Modifier.matchParentSize(),
                color = Color.Cyan.copy(alpha = 0.05f),
                dimension = 40f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(100.dp))

                // Contenitore delle opzioni con spaziature consistenti
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    OptionPanel(
                        title = "GAME MODE",
                        icon = Icons.Default.Flag,
                        options = listOf("Fleet Battle", "Classic"),
                        selected = gameMode,
                        onSelect = { gameMode = it }
                    )

                    OptionPanel(
                        title = "DIFFICULTY",
                        icon = Icons.Default.Psychology,
                        options = listOf("Easy", "Normal", "Hard"),
                        selected = difficulty,
                        onSelect = { difficulty = it }
                    )

                    OptionPanel(
                        title = "GRID SIZE",
                        icon = Icons.Default.GridOn,
                        options = listOf("8 x 8", "10 x 10", "12 x 12"),
                        selected = gridSize,
                        onSelect = { gridSize = it }
                    )

                    OptionPanel(
                        title = "TIME LIMIT",
                        icon = Icons.Default.Timer,
                        options = listOf("No Limit", "5 min", "10 min"),
                        selected = timeLimit,
                        onSelect = { timeLimit = it }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                StartBattleButton(onClick = onStartBattle)

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ───── TAB ─────
            // La tab fluttua sopra
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp) // Leggero overlap
                    .zIndex(2f)
            ) {
                Tab(
                    paddingH = 110f,
                    paddingV = 32f,
                    text = "GAME OPTIONS"
                )
            }
        }

        // ───── BOTTOM BAR ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            BarPattern(color = White.copy(alpha = 0.05f))
        }
    }
}

@Composable
fun OptionPanel(
    title: String,
    icon: ImageVector,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        ) {
            // Icona con un leggero alone
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Orange.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp).offset(1.dp, 1.dp)
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = Color(0xFFB0BEC5), // Un grigio-azzurro chiaro
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                OptionChip(
                    text = option,
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "textAnim"
    )

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = shape,
                spotColor = if(selected) Color.Cyan else Color.Transparent
            )
            .clip(shape)
            .background(
                if (selected) ActiveChipGradient else Brush.linearGradient(
                    listOf(Color.White.copy(0.05f), Color.White.copy(0.1f))
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(0.5f) else Color.White.copy(0.1f),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Rimuove il ripple di default per un look custom
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun StartBattleButton(
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(10.dp, shape, spotColor = Orange)
            .clip(shape)
            .background(ButtonGradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
                .background(Color.White.copy(alpha = 0.15f))
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "START BATTLE",
                color = Color(0xFF3E2723),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Flag,
                contentDescription = null,
                tint = Color(0xFF3E2723),
                modifier = Modifier.size(20.dp)
            )
        }
    }

}

@Composable
fun BarPattern(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Configurazione delle strisce
        val strokeWidth = 2.dp.toPx()
        val spacing = 12.dp.toPx() // Distanza tra le linee

        // Calcolo per coprire l'intera area con linee diagonali
        // Si parte da 0 fino a width + height per coprire l'angolo in basso a destra
        var currentX = 0f

        while (currentX < canvasWidth + canvasHeight) {
            drawLine(
                color = color,
                start = Offset(x = currentX, y = 0f),
                end = Offset(x = currentX - canvasHeight, y = canvasHeight),
                strokeWidth = strokeWidth
            )
            currentX += spacing
        }
    }
}