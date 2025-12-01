package com.example.brave_sailors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme

@Composable
fun MenuScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OuterDeepBlue)
    ) {
        //Above bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(OuterDeepBlue)
        ) {
            BarPattern()
        }

        //Central area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CenterBg),
            contentAlignment = Alignment.Center
        ) {
            GridBackground(Modifier.matchParentSize())

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MenuButton(
                    text = "Game options",
                    icon = Icons.Default.VideogameAsset,
                    onClick = { /* TODO */ }
                )
                MenuButton(
                    text = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = { /* TODO */ }
                )
                MenuButton(
                    text = "Instructions",
                    icon = Icons.Default.HelpOutline,
                    onClick = { /* TODO */ }
                )
            }
        }

        //Behind bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(OuterDeepBlue)
        ) {
            BarPattern()
        }
    }
}

@Composable
fun MenuButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val cornerSize = 12.dp
    val buttonShape = CutCornerShape(cornerSize)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(buttonShape) // Apply the shape
            .background(DeepBlue) // We set the background color
            .clickable { onClick() }
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerSizePx = cornerSize.toPx()

                //Function used to realize the white gradient
                val brush = Brush.verticalGradient(
                    0.0f to Color.Transparent, // Trasparente all'inizio (in alto)
                    (cornerSizePx / size.height) to TextWhite, // Bianco dopo l'angolo
                    (size.height - cornerSizePx) / size.height to TextWhite, // Bianco prima dell'angolo
                    1.0f to Color.Transparent // Trasparente alla fine (in basso)
                )

                //Draw the outline of the shape making use of the gradient
                drawOutline(
                    outline = buttonShape.createOutline(size, layoutDirection, this),
                    brush = brush,
                    style = Stroke(width = strokeWidth)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = TextWhite,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    Brave_SailorsTheme {
        MenuScreen()
    }
}
