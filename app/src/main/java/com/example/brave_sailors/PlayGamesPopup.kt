package com.example.brave_sailors

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay

@Composable
fun PlayGamesWelcomePopup(
    name: String,
    photoUrl: String? = null,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val scale = RememberScaleConversion()

    // Animation
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "popupAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else -200f,
        label = "popupOffset"
    )

    LaunchedEffect(visible) {
        if (visible) {
            delay(3000) // popup duration
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .offset(y = offsetY.dp)
            .alpha(alpha),
        contentAlignment = Alignment.TopCenter
    ) {

        Row(
            modifier = Modifier
                .background(Color(0xFF34A853), shape = CutCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Avatar
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PG", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    "Accesso Play Games",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.sp(16f)
                )

                Text(
                    name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = scale.sp(18f)
                )
            }
        }
    }
}