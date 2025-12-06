package com.example.brave_sailors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.brave_sailors.ui.theme.*

// --- CHANGED: Using a 'Flag' class to hold both the ID and the name ---
data class Flag(val name: String, val resourceId: Int)

// --- CHANGED: List of Flag objects ---
// Make sure you have these resources in res/drawable (e.g., flag_italy.png)
val availableFlags = listOf(
    Flag("Italy", R.drawable.flag_italy),
    Flag("Spain", R.drawable.flag_spain),
    Flag("France", R.drawable.flag_france),
    // Add more flags here...
    // Flag("Germany", R.drawable.flag_germany),
)

@Composable
fun ProfileScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val barHeight = screenHeight * 0.12f

    // --- CHANGED: States to manage the dialog and the selected flag ---
    var showFlagDialog by remember { mutableStateOf(false) }
    var selectedFlag by remember { mutableStateOf(availableFlags.first()) }

    // --- NEW: Show the custom dialog when the state is true ---
    if (showFlagDialog) {
        FlagSelectionDialog(
            onDismiss = { showFlagDialog = false },
            onFlagSelected = { flag ->
                selectedFlag = flag
                showFlagDialog = false
            }
        )
    }

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
                    // --- CHANGED: The box opens the dialog and shows the flag image ---
                    ProfileCustomizationItem(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showFlagDialog = true }, // Open the dialog on click
                        title = "Flag"
                    ) {
                        Image(
                            painter = painterResource(id = selectedFlag.resourceId),
                            contentDescription = selectedFlag.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp), // Add padding to prevent touching the edges
                            contentScale = ContentScale.Crop
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
                        title = "Starting line",
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
fun FlagSelectionDialog(onDismiss: () -> Unit, onFlagSelected: (Flag) -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Allows custom width
    ) {
        // We use the same shape and color as the "Portrait" / "Flag" buttons
        val dialogShape = CutCornerShape(12.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp) // Same as the main column's padding
                .drawBehind {
                    // We draw the same gradient border as the buttons
                    val strokeWidth = 1.dp.toPx()
                    val cornerSizePx = 12.dp.toPx()
                    val brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        (cornerSizePx / size.height) to TextWhite,
                        (size.height - cornerSizePx) / size.height to TextWhite,
                        1.0f to Color.Transparent
                    )
                    drawOutline(
                        outline = dialogShape.createOutline(size, layoutDirection, this),
                        brush = brush,
                        style = Stroke(width = strokeWidth)
                    )
                },
            shape = dialogShape,
            color = DeepBlue // Same background as the buttons
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dialog Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SELECT FLAG", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                }

                // Scrollable list of flags
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableFlags) { flag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFlagSelected(flag) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = flag.resourceId),
                                contentDescription = flag.name,
                                modifier = Modifier
                                    .size(width = 60.dp, height = 40.dp) // Fixed size for the flag
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f)), // Optional small border
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = flag.name.uppercase(),
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ProfileCustomizationItem(modifier: Modifier = Modifier, title: String, content: @Composable () -> Unit) {
    val buttonShape = CutCornerShape(12.dp)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextWhite, fontSize = 14.sp, textAlign = TextAlign.Center)
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
            Text(title, color = TextWhite, fontSize = 14.sp, textAlign = TextAlign.Center)
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


@Preview
@Composable
fun FlagSelectionDialogPreview() {
    Brave_SailorsTheme {
        // Dark background to simulate how it appears over the app
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            FlagSelectionDialog(onDismiss = {}, onFlagSelected = {})
        }
    }
}
