package com.example.brave_sailors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.brave_sailors.ui.components.Footer
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Radar
import com.example.brave_sailors.ui.components.Title
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun IntroScreen(innerPadding: PaddingValues = PaddingValues(0.dp), onFinished: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(innerPadding)
    ) {
        // Center area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DeepBlue),
            contentAlignment = Alignment.Center
        ) {
            Modal(onFinished = onFinished)
        }
    }
}

@Composable
private fun Modal(onFinished: () -> Unit) {
    val ovalShape = GenericShape { size, _ ->
        addOval(Rect(0f, 0f, size.width, size.height))
    }

    // -- SCALE ( used for applying conversions ) --
    val scale = RememberScaleConversion()
    val maxWidth = scale.dp(648f)

    // Handling footer text state
    var statusText by remember { mutableStateOf(". initializing system...") }
    val radarDuration = 4500L

    LaunchedEffect(Unit) {
        // Message sequence (console style)
        statusText = ". scanning frequencies..."
        delay(radarDuration / 3) // 1.5s

        statusText = ". establishing connection..."
        delay(radarDuration / 3) // 1.5s

        statusText = ". retrieving data..."
        delay(radarDuration / 3) // 1.5s

        // Finally, start the app / navigation
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.TopCenter
    ) {
        GridBackground(Modifier.matchParentSize(), color = DarkGrey, 14f)

        Box(
            modifier = Modifier
                .widthIn(max = maxWidth),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image with radial shadow
                Box(
                    modifier = Modifier
                        .width(scale.dp(668f))
                        .height(scale.dp(472f))
                        .background(Color.Transparent)
                        .drawWithContent {
                            drawContent()
                            val shadowRadius = max(size.width, size.height) / 1.5f
                            val shadowBrush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black
                                ),
                                center = center,
                                radius = shadowRadius
                            )
                            drawOval(
                                brush = shadowBrush,
                                topLeft = Offset.Zero,
                                size = size
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_intro),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(ovalShape)
                    )
                }

                Spacer(modifier = Modifier.height(scale.dp(96f)))

                Title()

                Spacer(modifier = Modifier.height(scale.dp(242f)))

                Radar(
                    modifier = Modifier
                        .size(scale.dp(144f))
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            // The Footer receives the dynamic status text and displays it at the bottom left
            Footer(
                modifier = Modifier.align(Alignment.BottomCenter),
                statusText = statusText
            )
        }
    }
}