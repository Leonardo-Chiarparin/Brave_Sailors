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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import com.example.brave_sailors.data.local.database.entity.User
import com.example.brave_sailors.data.remote.api.Flag
import com.example.brave_sailors.model.ProfileViewModel
import com.example.brave_sailors.ui.components.Footer
import com.example.brave_sailors.ui.components.GridBackground
import com.example.brave_sailors.ui.components.Radar
import com.example.brave_sailors.ui.components.Title
import com.example.brave_sailors.ui.theme.DarkGrey
import com.example.brave_sailors.ui.theme.DeepBlue
import com.example.brave_sailors.ui.theme.Orange
import com.example.brave_sailors.ui.utils.BackPress
import com.example.brave_sailors.ui.utils.RememberScaleConversion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun IntroScreen(innerPadding: PaddingValues = PaddingValues(0.dp), viewModel: ProfileViewModel, onFinished: () -> Unit) {
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
            Modal(viewModel, onFinished)
        }
    }
}

@Composable
private fun Modal(viewModel: ProfileViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    val ovalShape = GenericShape { size, _ ->
        addOval(Rect(0f, 0f, size.width, size.height))
    }

    // -- SCALE ( used for applying conversions )
    val scale = RememberScaleConversion()

    val maxWidth = scale.dp(648f) // 648px, etc.

    // Handling the Footer's textState ( . connect to server )
    // The order of such sentences is:
    // 0) . initialize social gaming network ( ? )
    // 1) . connect to server
    // 2) . retrieve data from server
    // 3) . start up audio ( if we decide to implement such functionality )
    // 4) . check consent and permissions
    // 5) . starting

    var statusText by remember { mutableStateOf(". connect to server") }
    val radarDuration = 4500L * 2L // two times the duration taken by the radar to complete a turn

    BackPress { false }

    LaunchedEffect(Unit) {
        val timerJob = async {
            delay(radarDuration / 8)
            statusText = ". retrieve data from server"
            delay(radarDuration / 4)
            statusText = ". check consent and permissions"
            delay(radarDuration / 8)
            statusText = ". starting"
            delay(radarDuration / 2)
        }

        val imagesJob = async {
            val imageLoader = context.imageLoader

            val loadedUser = viewModel.userState
                .filterNotNull()
                .first()

            val loadedFlags = viewModel.flagList
                .filter { it.isNotEmpty() }
                .first()

            if (!loadedUser.profilePictureUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(loadedUser.profilePictureUrl)
                    .build()
                imageLoader.execute(request)
            }

            val userFlag = loadedFlags.find { it.code == loadedUser.countryCode }
            if (userFlag != null) {
                val request = ImageRequest.Builder(context)
                    .data(userFlag.flagUrl)
                    .build()
                imageLoader.execute(request)
            }

            Pair(loadedUser, loadedFlags)
        }

        val result = awaitAll(timerJob, imagesJob)

        val (finalUser, finalFlags) = result[1] as Pair<*, *>
        val safeUser = finalUser as? User
        val safeFlags = finalFlags as? List<Flag>

        launch {
            if (safeUser != null && safeFlags != null) {
                val imageLoader = context.imageLoader
                safeFlags.forEach { flag ->
                    if (flag.code != safeUser.countryCode) {
                        val request = ImageRequest.Builder(context)
                            .data(flag.flagUrl)
                            .build()
                        imageLoader.enqueue(request)
                    }
                }
            }
        }

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(),
        contentAlignment = Alignment.TopCenter
    ) {
        GridBackground(Modifier.matchParentSize(), color = DarkGrey,14f)

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
                                    Color.Black                                    ),
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

                Spacer(modifier = Modifier.height(scale.dp(70f)))

                Text(
                    text = "Prepare to engage in strategic naval operations, where every decision shapes the tide of battle.",
                    color = Orange,
                    fontFamily = FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = scale.sp(26f),
                    textAlign = TextAlign.Center,
                    lineHeight = scale.sp(36f),
                    letterSpacing = scale.sp(2f),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Footer(modifier = Modifier.align(Alignment.BottomCenter), statusText)
        }
    }
}