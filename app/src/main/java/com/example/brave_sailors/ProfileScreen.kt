package com.example.brave_sailors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.brave_sailors.ui.theme.Brave_SailorsTheme

@Composable
fun ProfileScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OuterDeepBlue)
    ) {
        // Upper bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(OuterDeepBlue)
        ) {
            BarPattern()
        }

        // Center area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CenterBg),
            contentAlignment = Alignment.TopCenter
        ) {
            GridBackground(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp)) // Space for HeaderTab

                // Username + Change Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Username field
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DeepBlue,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(BorderOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EttoreCantile", color = TextWhite, fontSize = 16.sp)
                        }
                    }
                    // Change name button
                    Button(
                        onClick = { /*TODO*/ },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderGrey)
                    ) {
                        Text("Change name", color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Portrait and Flag selectors
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileCustomizationItem(modifier = Modifier.weight(1f), title = "Portrait") {
                        Image(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Portrait",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            colorFilter = ColorFilter.tint(TextWhite)
                        )
                    }
                    ProfileCustomizationItem(modifier = Modifier.weight(1f), title = "Flag") {
                        // Placeholder for flag
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics and Leaderboards
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Statistics",
                        icon = Icons.Default.BarChart
                    )
                    ProfileActionButton(
                        modifier = Modifier.weight(1f),
                        title = "LeaderBoard",
                        icon = Icons.Default.EmojiEvents
                    )
                    ProfileActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Starting line-up",
                        icon = Icons.Default.ViewModule
                    )
                }

            }

            Box(modifier = Modifier.zIndex(2f)) {
                HeaderTab(text = "PROFILE")
            }
        }

        // Lower bar
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
fun ProfileCustomizationItem(modifier: Modifier = Modifier, title: String, content: @Composable () -> Unit) {
    val buttonShape = CutCornerShape(12.dp)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextWhite, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val cornerSizePx = 12.dp.toPx()
                    val brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        (cornerSizePx / size.height) to TextWhite,
                        (size.height - cornerSizePx) / size.height to TextWhite,
                        1.0f to Color.Transparent
                    )
                    drawOutline(
                        outline = buttonShape.createOutline(size, layoutDirection, this),
                        brush = brush,
                        style = Stroke(width = strokeWidth)
                    )
                },
            shape = buttonShape,
            color = DeepBlue
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
fun ProfileActionButton(modifier: Modifier = Modifier, title: String, icon: ImageVector) {
    val buttonShape = CutCornerShape(12.dp)
    Surface(
        modifier = modifier
            .clickable { }
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerSizePx = 12.dp.toPx()
                val brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    (cornerSizePx / size.height) to TextWhite,
                    (size.height - cornerSizePx) / size.height to TextWhite,
                    1.0f to Color.Transparent
                )
                drawOutline(
                    outline = buttonShape.createOutline(size, layoutDirection, this),
                    brush = brush,
                    style = Stroke(width = strokeWidth)
                )
            },
        shape = buttonShape,
        color = DeepBlue,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = TextWhite, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = TextWhite, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    Brave_SailorsTheme {
        ProfileScreen()
    }
}
