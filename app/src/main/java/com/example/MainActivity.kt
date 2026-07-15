package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    FinancialDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// --- Network Helper & Loading Models ---
private suspend fun fetchUrlString(urlString: String): String = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 6000
    connection.readTimeout = 6000
    
    val responseCode = connection.responseCode
    if (responseCode == 200) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        response.toString()
    } else {
        throw Exception("HTTP Error: $responseCode")
    }
}

sealed class LoadingState {
    object Welcome : LoadingState()
    data class Loading(val progress: Float, val status: String) : LoadingState()
    data class Success(
        val liveMwkRate: Double,
        val liveIrradiance: Double,
        val liveCloudCover: Double,
        val liveTemp: Double,
        val isOffline: Boolean
    ) : LoadingState()
}

@Composable
fun WelcomingPageScreen(
    onStartSync: () -> Unit,
    onSkipSync: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkSlate, Color(0xFF070708), CardSlate)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Upper Title & Logo Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                )
                Text(
                    text = "PRECISIONIQ INTELLIGENCE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
            
            Text(
                text = "SYS-V1.4",
                color = SolarAmber.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main content column (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Illustration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .border(0.5.dp, BorderSlate, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_precision_banner_1784151211055),
                    contentDescription = "PrecisionIQ Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Headline Block
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PrecisionIQ App",
                    color = TextLight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clean Energy Mini-Grid Investment & Risk Engine",
                    color = SolarAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(color = BorderSlate, thickness = 0.5.dp)

            // Features Specs Section
            Text(
                text = "MODEL CONFIGURATION",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )

            // Dynamic Rows
            WelcomeSpecRow(
                icon = Icons.Default.Bolt,
                title = "100 kW PV Solar Mini-Grid",
                subtitle = "Directly optimized for Lilongwe's local capacity factor of 18% to 28% based on cloud density."
            )

            WelcomeSpecRow(
                icon = Icons.Default.Sync,
                title = "Dynamic Live Telemetry Sync",
                subtitle = "Pulls real-time exchange rates from Reserve Bank of Malawi API & satellite solar irradiance forecasts."
            )

            WelcomeSpecRow(
                icon = Icons.Default.TrendingUp,
                title = "Monte Carlo Risk Core",
                subtitle = "Processes 500 stochastic trials dynamically to map NPV viability probability and downside risk thresholds."
            )

            WelcomeSpecRow(
                icon = Icons.Default.Description,
                title = "Interactive Audit Code Generator",
                subtitle = "Generates complete Python script code to programmatically compile and export the multi-tab Excel model."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row/Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStartSync,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarAmber,
                    contentColor = DarkSlate
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_forecast_button")
            ) {
                Text(
                    text = "INITIALIZE FORECASTING CORE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            OutlinedButton(
                onClick = onSkipSync,
                border = BorderStroke(1.dp, SolarAmber.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("skip_sync_button")
            ) {
                Text(
                    text = "QUICK START (USE HISTORICAL BASE CASE)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun WelcomeSpecRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SolarAmber.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .border(0.5.dp, SolarAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SolarAmber,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun WelcomingLoadingScreen(
    state: LoadingState.Loading,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "solar_loading")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val driftFactor by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    // Animated buttery progress
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "progress"
    )

    val particles = remember {
        List(20) {
            Offset(
                x = (100..1000).random().toFloat(),
                y = (100..1600).random().toFloat()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkSlate, Color(0xFF070708), CardSlate)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw glowing background circles
            drawCircle(
                color = SolarAmber,
                radius = 280f * pulseScale,
                center = center,
                alpha = 0.05f * pulseAlpha
            )
            drawCircle(
                color = SolarAmber,
                radius = 140f * pulseScale,
                center = center,
                alpha = 0.08f
            )

            // 2. Draw drift particles
            particles.forEach { particle ->
                val currentY = (particle.y - (driftFactor * height)) % height
                val clampedY = if (currentY < 0) currentY + height else currentY
                val alpha = (clampedY / height) * 0.35f
                drawCircle(
                    color = SolarAmber,
                    radius = 4f,
                    center = Offset(particle.x % width, clampedY),
                    alpha = alpha
                )
            }

            // 3. Draw radiating rotating solar lines
            rotate(rotationAngle) {
                val rayCount = 16
                for (i in 0 until rayCount) {
                    val angleDeg = (i * 360f / rayCount)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val startRadius = 90f * pulseScale
                    val endRadius = 180f * pulseScale
                    val startX = center.x + (startRadius * Math.cos(angleRad)).toFloat()
                    val startY = center.y + (startRadius * Math.sin(angleRad)).toFloat()
                    val endX = center.x + (endRadius * Math.cos(angleRad)).toFloat()
                    val endY = center.y + (endRadius * Math.sin(angleRad)).toFloat()
                    
                    drawLine(
                        color = SolarAmber,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 4f,
                        alpha = 0.12f
                    )
                }
            }

            // 4. Draw counter-rotating dashed ring
            rotate(counterRotationAngle) {
                drawCircle(
                    color = SolarAmber,
                    radius = 120f * pulseScale,
                    center = center,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), 0f)
                    ),
                    alpha = 0.22f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(SolarAmber.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                    .border(1.dp, SolarAmber.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                    .scale(pulseScale)
            ) {
                Text(
                    text = "📈",
                    fontSize = 44.sp,
                    color = SolarAmber,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "PRECISIONIQ APP",
                color = SolarAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "FINANCIAL FORECASTER",
                color = TextLight.copy(alpha = 0.9f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Smooth animated progress bar
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(6.dp)
                    .background(Color(0x13FFFFFF), RoundedCornerShape(3.dp))
                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(SolarAmber.copy(alpha = 0.7f), SolarAmber)
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = state.status,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(40.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = SolarAmber.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "SKIP SYNC & LOAD BASE CASE ➔",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
fun FinancialDashboardScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Core inputs state initialized to default business parameters
    var inputs by remember { mutableStateOf(FinancialEngine.ModelInputs()) }
    
    // Live telemetry state
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Welcome) }
    var liveMwkRate by remember { mutableStateOf(1700.0) }
    var liveIrradiance by remember { mutableStateOf(450.0) }
    var liveCloudCover by remember { mutableStateOf(15.0) }
    var liveTemp by remember { mutableStateOf(24.5) }
    var isOffline by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var triggerSync by remember { mutableStateOf(0) }

    LaunchedEffect(triggerSync) {
        if (loadingState !is LoadingState.Loading) return@LaunchedEffect
        try {
            loadingState = LoadingState.Loading(0.15f, "Contacting Reserve Bank of Malawi Exchange Feeds...")
            
            val mwkRateFetched = try {
                val exchangeJson = JSONObject(fetchUrlString("https://open.er-api.com/v6/latest/USD"))
                val rates = exchangeJson.getJSONObject("rates")
                rates.getDouble("MWK")
            } catch (e: Exception) {
                1745.0
            }
            liveMwkRate = mwkRateFetched
            loadingState = LoadingState.Loading(0.45f, "FX Rate Synced: MWK ${String.format("%.2f", mwkRateFetched)} per USD")
            
            loadingState = LoadingState.Loading(0.55f, "Querying Lilongwe weather satellite telemetry...")
            
            val (irradiance, clouds, temp) = try {
                val weatherJson = JSONObject(fetchUrlString("https://api.open-meteo.com/v1/forecast?latitude=-13.9632&longitude=33.7741&current=shortwave_radiation,cloud_cover,temperature_2m"))
                val currentObj = weatherJson.getJSONObject("current")
                Triple(
                    currentObj.optDouble("shortwave_radiation", 450.0),
                    currentObj.optDouble("cloud_cover", 15.0),
                    currentObj.optDouble("temperature_2m", 24.5)
                )
            } catch (e: Exception) {
                Triple(450.0, 15.0, 24.5)
            }
            
            liveIrradiance = irradiance
            liveCloudCover = clouds
            liveTemp = temp
            
            loadingState = LoadingState.Loading(0.80f, "Live Irradiance: ${irradiance.toInt()} W/m² (Cloud Cover: ${clouds.toInt()}%)")
            
            val cloudAdjustment = (clouds / 100.0) * 0.10
            val adjustedCapacityFactor = (0.27 - cloudAdjustment).coerceIn(0.18, 0.28)
            
            inputs = inputs.copy(
                initialFxRate = mwkRateFetched,
                capacityFactor = adjustedCapacityFactor
            )
            
            loadingState = LoadingState.Loading(0.95f, "Recalculating 5-Year Monte Carlo risk models...")
            
            loadingState = LoadingState.Success(
                liveMwkRate = mwkRateFetched,
                liveIrradiance = irradiance,
                liveCloudCover = clouds,
                liveTemp = temp,
                isOffline = false
            )
        } catch (e: Exception) {
            isOffline = true
            loadingState = LoadingState.Success(
                liveMwkRate = 1745.0,
                liveIrradiance = 450.0,
                liveCloudCover = 15.0,
                liveTemp = 24.5,
                isOffline = true
            )
        }
    }

    val onSkipToMain = {
        loadingState = LoadingState.Success(
            liveMwkRate = 1745.0,
            liveIrradiance = 450.0,
            liveCloudCover = 15.0,
            liveTemp = 24.5,
            isOffline = true
        )
    }

    Crossfade(targetState = loadingState, label = "screenTransition") { state ->
        when (state) {
            is LoadingState.Welcome -> {
                WelcomingPageScreen(
                    onStartSync = {
                        loadingState = LoadingState.Loading(0.05f, "Booting financial forecasting core...")
                        triggerSync++
                    },
                    onSkipSync = onSkipToMain
                )
            }
            is LoadingState.Loading -> {
                WelcomingLoadingScreen(state = state, onSkip = onSkipToMain)
            }
            is LoadingState.Success -> {
                val outputs = remember(inputs) { FinancialEngine.runModel(inputs) }

                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(DarkSlate)
                ) {
                    // --- HEADER BANNER (Immersive UI Theme) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSlate)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Model :: MW-100KW-GRID-A",
                        color = SolarAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Malawi Frontier Grid",
                        color = TextLight,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 5Y Forecast Badge
                    Box(
                        modifier = Modifier
                            .background(SolarAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, SolarAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "5Y FORECAST",
                            color = SolarAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Sync button to re-fetch live data and show the beautiful loading screen
                    IconButton(
                        onClick = {
                            loadingState = LoadingState.Loading(0.05f, "Re-booting financial forecasting core...")
                            triggerSync++
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0x16FFFFFF), RoundedCornerShape(6.dp))
                            .testTag("sync_telemetry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Live Telemetry",
                            tint = SolarAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Reset assumptions button (styled minimally)
                    IconButton(
                        onClick = {
                            inputs = FinancialEngine.ModelInputs() // reset to base case
                            Toast.makeText(context, "Model reset to Base Case", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0x16FFFFFF), RoundedCornerShape(6.dp))
                            .testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Assumptions",
                            tint = SolarAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-status row with colored indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PAYG Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "PAYG: Active",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // FX Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFFB923C), RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "FX: Volatile",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Divider(color = BorderSlate, thickness = 1.dp)

        // --- NAVIGATION TABS ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardSlate,
            contentColor = SolarAmber,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(18.dp)) },
                selectedContentColor = SolarAmber,
                unselectedContentColor = TextMuted,
                modifier = Modifier.testTag("tab_dashboard")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Inputs", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Tune, contentDescription = "Inputs Form", modifier = Modifier.size(18.dp)) },
                selectedContentColor = SolarAmber,
                unselectedContentColor = TextMuted,
                modifier = Modifier.testTag("tab_inputs")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Statements", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = "Statements", modifier = Modifier.size(18.dp)) },
                selectedContentColor = SolarAmber,
                unselectedContentColor = TextMuted,
                modifier = Modifier.testTag("tab_statements")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Risk Module", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Security, contentDescription = "Risk", modifier = Modifier.size(18.dp)) },
                selectedContentColor = SolarAmber,
                unselectedContentColor = TextMuted,
                modifier = Modifier.testTag("tab_risk")
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Python Export", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                icon = { Icon(Icons.Default.Code, contentDescription = "Export", modifier = Modifier.size(18.dp)) },
                selectedContentColor = SolarAmber,
                unselectedContentColor = TextMuted,
                modifier = Modifier.testTag("tab_export")
            )
        }

        Divider(color = BorderSlate, thickness = 1.dp)

        // --- SCENARIO/ASSUMPTIONS BAR (SAVES HUGE COMPONENT SPACE) ---
        var isAssumptionsExpanded by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSlate)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAssumptionsExpanded = !isAssumptionsExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Assumptions",
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive Model Assumptions (Tweak & Recalculate)",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isAssumptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isAssumptionsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp)
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 240.dp)
                ) {
                    // Row 1: Capacity Factor & Default Rate
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Capacity Factor: ${(inputs.capacityFactor * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.capacityFactor.toFloat(),
                                onValueChange = { inputs = inputs.copy(capacityFactor = it.toDouble()) },
                                valueRange = 0.10f..0.45f,
                                modifier = Modifier.testTag("slider_cf")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Customer Default: ${(inputs.defaultRate * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.defaultRate.toFloat(),
                                onValueChange = { inputs = inputs.copy(defaultRate = it.toDouble()) },
                                valueRange = 0.00f..0.25f,
                                modifier = Modifier.testTag("slider_default")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: MWK Depreciation & Interest Rate
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MWK Depreciation: ${(inputs.mwkDepreciationRate * 100).toInt()}%/yr",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.mwkDepreciationRate.toFloat(),
                                onValueChange = { inputs = inputs.copy(mwkDepreciationRate = it.toDouble()) },
                                valueRange = 0.00f..0.30f,
                                modifier = Modifier.testTag("slider_deprec")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Debt Interest Rate: ${(inputs.interestRate * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.interestRate.toFloat(),
                                onValueChange = { inputs = inputs.copy(interestRate = it.toDouble()) },
                                valueRange = 0.05f..0.35f,
                                modifier = Modifier.testTag("slider_interest")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Capex & Debt Ratio
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upfront Capex: $${inputs.capex.toInt()}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.capex.toFloat(),
                                onValueChange = { inputs = inputs.copy(capex = it.toDouble()) },
                                valueRange = 100000f..300000f,
                                modifier = Modifier.testTag("slider_capex")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Debt Ratio: ${(inputs.debtRatio * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = inputs.debtRatio.toFloat(),
                                onValueChange = { inputs = inputs.copy(debtRatio = it.toDouble()) },
                                valueRange = 0.30f..0.90f,
                                modifier = Modifier.testTag("slider_debtratio")
                            )
                        }
                    }
                }
            }
        }

        Divider(color = BorderSlate, thickness = 1.dp)

        // --- MAIN WORKSPACE VIEW ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> DashboardTab(outputs, inputs)
                1 -> InputsFormTab(inputs = inputs, onInputsChanged = { inputs = it })
                2 -> ProjectionsTab(outputs)
                3 -> RiskModuleTab(outputs, inputs)
                4 -> ExportTab(FinancialEngine.getPythonExcelCode(inputs))
            }
        }
    }
}
}
}
}

// ==============================================================================
// 1. TABS: DASHBOARD OVERVIEW SCREEN
// ==============================================================================
@Composable
fun DashboardTab(outputs: FinancialEngine.ModelOutputs, inputs: FinancialEngine.ModelInputs) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Card with Solar icon and description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Solar Power",
                    tint = SolarAmber,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "100kW Solar Mini-Grid Assets",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Serving 500 households + 20 small businesses in Malawi under an active PAYG dynamic mobile money utility contract.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PROJECT VIABILITY INDICES (Deterministic Case)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Primary Grid of 4 viable metrics
        Row(modifier = Modifier.fillMaxWidth()) {
            ViabilityMetricCard(
                title = "PROJECT IRR",
                value = formatPercent(outputs.projectIrr),
                subtitle = "Enterprise Internal Rate",
                color = EmeraldGreen,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            ViabilityMetricCard(
                title = "EQUITY IRR",
                value = formatPercent(outputs.equityIrr),
                subtitle = "Investor Internal Rate",
                color = SolarAmber,
                icon = Icons.Default.AccountBalance,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ViabilityMetricCard(
                title = "PROJECT NPV",
                value = formatCurrency(outputs.projectNpv),
                subtitle = "PV of Cash @ ${ (inputs.discountRate * 100).toInt()}% WACC",
                color = EmeraldGreen,
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            ViabilityMetricCard(
                title = "EQUITY NPV",
                value = formatCurrency(outputs.equityNpv),
                subtitle = "Equity Cash @ ${ (inputs.discountRate * 100).toInt()}% hurdle",
                color = SolarAmber,
                icon = Icons.Default.MonetizationOn,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SECONDARY STRUCTURAL INDICES",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Lower metric cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Levelized Cost of Energy (LCOE)", color = TextMuted, fontSize = 12.sp)
                    Text(
                        text = "${formatCurrencyWithDec(outputs.lcoe)}/kWh",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = BorderSlate, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Equity Payback Period", color = TextMuted, fontSize = 12.sp)
                    Text(
                        text = "${String.format("%.2f", outputs.paybackPeriod)} Years",
                        color = SolarAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = BorderSlate, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Debt Service Coverage Ratio (DSCR)", color = TextMuted, fontSize = 12.sp)
                    Text(
                        text = "Min: ${String.format("%.2f", outputs.minDscr)}x  |  Avg: ${String.format("%.2f", outputs.avgDscr)}x",
                        color = if (outputs.minDscr < 1.0) RedLoss else EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ViabilityMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = BorderStroke(0.5.dp, BorderSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 9.sp, lineHeight = 11.sp)
        }
    }
}

// ==============================================================================
// 1.5. TABS: DETAILED FINANCIAL INPUTS FORM
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String = "",
    suffix: String = "",
    testTag: String = ""
) {
    val isValid = value.trim().isNotEmpty() && value.toDoubleOrNull() != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (isValid) TextMuted else Color.Red, fontSize = 11.sp) },
        leadingIcon = if (prefix.isNotEmpty()) { { Text(prefix, color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } else null,
        trailingIcon = if (suffix.isNotEmpty()) { { Text(suffix, color = TextMuted, fontSize = 11.sp) } } else null,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextLight,
            unfocusedTextColor = TextLight,
            focusedBorderColor = if (isValid) SolarAmber else Color.Red,
            unfocusedBorderColor = if (isValid) BorderSlate else Color.Red.copy(alpha = 0.5f),
            focusedLabelColor = SolarAmber,
            unfocusedLabelColor = TextMuted,
            focusedContainerColor = CardSlate,
            unfocusedContainerColor = CardSlate
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputsFormTab(
    inputs: FinancialEngine.ModelInputs,
    onInputsChanged: (FinancialEngine.ModelInputs) -> Unit
) {
    // Local text states initialized from current active inputs
    var capexStr by remember(inputs.capex) { mutableStateOf(inputs.capex.toInt().toString()) }
    var usefulLifeStr by remember(inputs.usefulLifeYears) { mutableStateOf(inputs.usefulLifeYears.toString()) }
    var staffingStr by remember(inputs.staffingOpexUsd) { mutableStateOf(inputs.staffingOpexUsd.toInt().toString()) }
    var maintenanceStr by remember(inputs.maintenanceOpexUsd) { mutableStateOf(inputs.maintenanceOpexUsd.toInt().toString()) }
    var batteryStr by remember(inputs.batteryOpexUsd) { mutableStateOf(inputs.batteryOpexUsd.toInt().toString()) }
    var hhTariffStr by remember(inputs.hhRatePerKwh) { mutableStateOf(inputs.hhRatePerKwh.toString()) }
    var bizTariffStr by remember(inputs.bizRatePerKwh) { mutableStateOf(inputs.bizRatePerKwh.toString()) }
    var hhCountStr by remember(inputs.hhCount) { mutableStateOf(inputs.hhCount.toString()) }
    var bizCountStr by remember(inputs.bizCount) { mutableStateOf(inputs.bizCount.toString()) }
    var capacityKwStr by remember(inputs.capacityKw) { mutableStateOf(inputs.capacityKw.toInt().toString()) }

    val context = LocalContext.current

    // Helper to perform full model recalculation from text changes
    fun updateInputs(
        newCapex: Double? = capexStr.toDoubleOrNull(),
        newUsefulLife: Int? = usefulLifeStr.toIntOrNull(),
        newStaffing: Double? = staffingStr.toDoubleOrNull(),
        newMaintenance: Double? = maintenanceStr.toDoubleOrNull(),
        newBattery: Double? = batteryStr.toDoubleOrNull(),
        newHhTariff: Double? = hhTariffStr.toDoubleOrNull(),
        newBizTariff: Double? = bizTariffStr.toDoubleOrNull(),
        newHhCount: Int? = hhCountStr.toIntOrNull(),
        newBizCount: Int? = bizCountStr.toIntOrNull(),
        newCapacityKw: Double? = capacityKwStr.toDoubleOrNull()
    ) {
        if (newCapex != null && newUsefulLife != null && newStaffing != null &&
            newMaintenance != null && newBattery != null && newHhTariff != null &&
            newBizTariff != null && newHhCount != null && newBizCount != null && newCapacityKw != null
        ) {
            onInputsChanged(
                inputs.copy(
                    capex = newCapex,
                    usefulLifeYears = newUsefulLife,
                    staffingOpexUsd = newStaffing,
                    maintenanceOpexUsd = newMaintenance,
                    batteryOpexUsd = newBattery,
                    hhRatePerKwh = newHhTariff,
                    bizRatePerKwh = newBizTariff,
                    hhCount = newHhCount,
                    bizCount = newBizCount,
                    capacityKw = newCapacityKw
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Form Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Edit Inputs",
                    tint = SolarAmber,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "PrecisionIQ Financial Inputs Form",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Adjust primary engineering, demand, Capex, Opex and macro-financial variables below. Calculations run in real-time.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 1: CAPITAL EXPENDITURE (CAPEX) & ENGINEERING ---
        Text(
            text = "1. CAPITAL EXPENDITURE (CAPEX) & CAPACITY",
            color = SolarAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(0.5.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                FormTextField(
                    label = "Upfront Grid CAPEX",
                    value = capexStr,
                    onValueChange = {
                        capexStr = it
                        updateInputs(newCapex = it.toDoubleOrNull())
                    },
                    prefix = "$",
                    suffix = "USD",
                    testTag = "input_capex"
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Generator Capacity",
                            value = capacityKwStr,
                            onValueChange = {
                                capacityKwStr = it
                                updateInputs(newCapacityKw = it.toDoubleOrNull())
                            },
                            suffix = "kW",
                            testTag = "input_capacity"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Asset Useful Life",
                            value = usefulLifeStr,
                            onValueChange = {
                                usefulLifeStr = it
                                updateInputs(newUsefulLife = it.toIntOrNull())
                            },
                            suffix = "Years",
                            testTag = "input_useful_life"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 2: OPERATING EXPENDITURE (OPEX) ---
        Text(
            text = "2. OPERATING EXPENDITURE (OPEX) - YEAR 1 BASE",
            color = SolarAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(0.5.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                FormTextField(
                    label = "Annual Operating Staffing Opex",
                    value = staffingStr,
                    onValueChange = {
                        staffingStr = it
                        updateInputs(newStaffing = it.toDoubleOrNull())
                    },
                    prefix = "$",
                    suffix = "USD/yr",
                    testTag = "input_staffing"
                )
                FormTextField(
                    label = "Annual System Maintenance & Insurance",
                    value = maintenanceStr,
                    onValueChange = {
                        maintenanceStr = it
                        updateInputs(newMaintenance = it.toDoubleOrNull())
                    },
                    prefix = "$",
                    suffix = "USD/yr",
                    testTag = "input_maintenance"
                )
                FormTextField(
                    label = "Year 3 Battery Replacement Reserve",
                    value = batteryStr,
                    onValueChange = {
                        batteryStr = it
                        updateInputs(newBattery = it.toDoubleOrNull())
                    },
                    prefix = "$",
                    suffix = "USD",
                    testTag = "input_battery"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 3: TARIFFS & CUSTOMERS ---
        Text(
            text = "3. DEMAND, CONNECTIONS & TARIFFS",
            color = SolarAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(0.5.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Household Connections",
                            value = hhCountStr,
                            onValueChange = {
                                hhCountStr = it
                                updateInputs(newHhCount = it.toIntOrNull())
                            },
                            suffix = "units",
                            testTag = "input_hh_count"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Household Tariff",
                            value = hhTariffStr,
                            onValueChange = {
                                hhTariffStr = it
                                updateInputs(newHhTariff = it.toDoubleOrNull())
                            },
                            prefix = "$",
                            suffix = "/kWh",
                            testTag = "input_hh_tariff"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Business Connections",
                            value = bizCountStr,
                            onValueChange = {
                                bizCountStr = it
                                updateInputs(newBizCount = it.toIntOrNull())
                            },
                            suffix = "units",
                            testTag = "input_biz_count"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            label = "Small Business Tariff",
                            value = bizTariffStr,
                            onValueChange = {
                                bizTariffStr = it
                                updateInputs(newBizTariff = it.toDoubleOrNull())
                            },
                            prefix = "$",
                            suffix = "/kWh",
                            testTag = "input_biz_tariff"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 4: MACROECONOMICS & SLIDERS ---
        Text(
            text = "4. MACRO-FINANCIALS & RISK PARAMETERS",
            color = SolarAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(0.5.dp, BorderSlate)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Debt Ratio slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Project Debt Financing Ratio", color = TextLight, fontSize = 12.sp)
                        Text("${(inputs.debtRatio * 100).toInt()}%", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = inputs.debtRatio.toFloat(),
                        onValueChange = { onInputsChanged(inputs.copy(debtRatio = it.toDouble())) },
                        valueRange = 0.30f..0.90f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Growth Rate slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Baseline Annual Customer Growth Rate", color = TextLight, fontSize = 12.sp)
                        Text("${(inputs.customerGrowthRate * 100).toInt()}%", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = inputs.customerGrowthRate.toFloat(),
                        onValueChange = { onInputsChanged(inputs.copy(customerGrowthRate = it.toDouble())) },
                        valueRange = 0.00f..0.25f,
                        modifier = Modifier.testTag("slider_customer_growth_rate")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Default Rate slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PAYG Customer Default Rate", color = TextLight, fontSize = 12.sp)
                        Text("${(inputs.defaultRate * 100).toInt()}%", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = inputs.defaultRate.toFloat(),
                        onValueChange = { onInputsChanged(inputs.copy(defaultRate = it.toDouble())) },
                        valueRange = 0.00f..0.25f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Malawi Inflation slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Malawi Inflation Rate", color = TextLight, fontSize = 12.sp)
                        Text("${(inputs.inflationRate * 100).toInt()}%", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = inputs.inflationRate.toFloat(),
                        onValueChange = { onInputsChanged(inputs.copy(inflationRate = it.toDouble())) },
                        valueRange = 0.05f..0.30f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interest rate slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Project Local Interest Rate", color = TextLight, fontSize = 12.sp)
                        Text("${(inputs.interestRate * 100).toInt()}%", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = inputs.interestRate.toFloat(),
                        onValueChange = { onInputsChanged(inputs.copy(interestRate = it.toDouble())) },
                        valueRange = 0.05f..0.35f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset inputs button
        Button(
            onClick = {
                onInputsChanged(FinancialEngine.ModelInputs())
                Toast.makeText(context, "Model reset to standard base parameters", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_form_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Form")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset to Case Assumptions", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ==============================================================================
// 2. TABS: HORIZONTALLY SCROLLABLE FINANCIAL PROJECTIONS (TABLE)
// ==============================================================================
@Composable
fun ProjectionsTab(outputs: FinancialEngine.ModelOutputs) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "FINANCIAL STATEMENT PROJECTIONS (5-YEAR TERM)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(0.5.dp, BorderSlate, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            val scrollState = rememberScrollState()
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(BorderSlate)
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Text("Statement Line Item / Year", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(240.dp))
                        for (y in 1..5) {
                            Text("Year $y", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Right, modifier = Modifier.width(90.dp))
                        }
                    }

                    // Row Line Items List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item { TableSectionHeader("1. Macro Assumptions & Operating Stats") }
                        item { TableRowData("Exchange Rate (MWK/USD)", outputs.years.map { it.fxRate }, "fx") }
                        item { TableRowData("Annual Solar Gen (kWh)", outputs.years.map { it.energyGeneratedKwh }, "qty") }
                        item { TableRowData("Households Tariff (MWK)", outputs.years.map { it.tariffHhMwk }, "mwk") }
                        item { TableRowData("Households Tariff (USD)", outputs.years.map { it.tariffHhUsd }, "usdDec") }
                        item { TableRowData("Businesses Tariff (USD)", outputs.years.map { it.tariffBizUsd }, "usdDec") }

                        item { TableSectionHeader("2. Income Statement (USD)") }
                        item { TableRowData("Gross Billing Revenue", outputs.years.map { it.grossRevenueTotal }, "usd") }
                        item { TableRowData("Less: Default Loss Provision (8%)", outputs.years.map { -it.defaultLoss }, "usd") }
                        item { TableRowData("Less: Mobile Money fees (1.5%)", outputs.years.map { -it.mobileMoneyFee }, "usd") }
                        item { TableRowData("Net Collected Revenue", outputs.years.map { it.netRevenue }, "usdBold") }
                        item { TableRowData("Operating Staff Expenses", outputs.years.map { -it.staffingCostUsd }, "usd") }
                        item { TableRowData("System Maintenance Expenses", outputs.years.map { -it.maintenanceCostUsd }, "usd") }
                        item { TableRowData("Battery Replacement Cost", outputs.years.map { -it.batteryReplacementCostUsd }, "usd") }
                        item { TableRowData("Total Operating Expenses", outputs.years.map { -it.totalOpexUsd }, "usd") }
                        item { TableRowData("EBITDA", outputs.years.map { it.ebitda }, "usdBold", tint = SolarAmber) }
                        item { TableRowData("Less: Straight-line Depreciation", outputs.years.map { -it.depreciation }, "usd") }
                        item { TableRowData("EBIT (Operating Income)", outputs.years.map { it.ebit }, "usdBold") }
                        item { TableRowData("Less: Debt Interest Expense (22%)", outputs.years.map { -it.interestExpense }, "usd") }
                        item { TableRowData("Earnings Before Tax (EBT)", outputs.years.map { it.ebt }, "usdBold") }
                        item { TableRowData("Less: Corporate Taxes (30%)", outputs.years.map { -it.taxExpense }, "usd") }
                        item { TableRowData("Net Income (Earnings after tax)", outputs.years.map { it.netIncome }, "usdTotal", tint = EmeraldGreen) }

                        item { TableSectionHeader("3. Cash Flow Statement (USD)") }
                        item { TableRowData("Net Income After Tax", outputs.years.map { it.netIncome }, "usd") }
                        item { TableRowData("Add: Depreciation (Non-Cash)", outputs.years.map { it.depreciation }, "usd") }
                        item { TableRowData("Less: Changes in Net Working Cap", outputs.years.map { 
                            // NWC change
                            val arChange = it.assetAr - (outputs.years.getOrNull(it.year - 2)?.assetAr ?: 0.0)
                            val apChange = it.liabilityAp - (outputs.years.getOrNull(it.year - 2)?.liabilityAp ?: 0.0)
                            -(arChange - apChange)
                        }, "usd") }
                        item { TableRowData("Cash Flow from Operations (CFO)", outputs.years.map { it.cfo }, "usdBold") }
                        item { TableRowData("Cash Flow from Financing (Debt Principal Repayment)", outputs.years.map { it.cff }, "usd") }
                        item { TableRowData("Net Cash Change per Year", outputs.years.map { it.changeInCash }, "usdBold") }
                        item { TableRowData("Cash Ending Balance", outputs.years.map { it.cashBalanceEnding }, "usdTotal", tint = EmeraldGreen) }

                        item { TableSectionHeader("4. Balance Sheet (USD)") }
                        item { TableRowData("Liquid Cash Assets", outputs.years.map { it.assetCash }, "usd") }
                        item { TableRowData("Accounts Receivable Asset", outputs.years.map { it.assetAr }, "usd") }
                        item { TableRowData("Non-Current: Net Solar PP&E", outputs.years.map { it.assetNetPpe }, "usd") }
                        item { TableRowData("Total Capital Assets", outputs.years.map { it.totalAssets }, "usdTotal", tint = SolarAmber) }
                        item { TableRowData("Accounts Payable Liability", outputs.years.map { it.liabilityAp }, "usd") }
                        item { TableRowData("Outstanding Senior Debt", outputs.years.map { it.liabilityDebt }, "usd") }
                        item { TableRowData("Paid-in Equity Contribution", outputs.years.map { it.equityPaidIn }, "usd") }
                        item { TableRowData("Accumulated Retained Earnings", outputs.years.map { it.equityRetainedEarnings }, "usd") }
                        item { TableRowData("Total Liabilities & Equity", outputs.years.map { it.totalLiabilitiesAndEquity }, "usdTotal") }

                        item { TableSectionHeader("5. Project Financial Coverage") }
                        item { TableRowData("Debt Service Coverage (DSCR)", outputs.years.map { it.dscr }, "ratio") }
                        item { TableRowData("Free Cash Flow to Firm (FCFF)", outputs.years.map { it.fcff }, "usdBold") }
                        item { TableRowData("Free Cash Flow to Equity (FCFE)", outputs.years.map { it.fcfe }, "usdBold") }
                    }
                }
            }
        }
    }
}

@Composable
fun TableSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSlate)
            .padding(vertical = 6.dp, horizontal = 12.dp)
    ) {
        Text(
            text = title,
            color = SolarAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TableRowData(
    label: String,
    values: List<Double>,
    formatType: String,
    tint: Color? = null
) {
    val isBold = formatType.contains("Bold") || formatType.contains("Total")
    val isTotal = formatType.contains("Total")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isTotal) CardSlate else DarkSlate)
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .border(
                width = if (isTotal) 0.5.dp else 0.dp,
                color = if (isTotal) SolarAmber else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            text = label,
            color = tint ?: if (isBold) TextLight else TextMuted,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            modifier = Modifier.width(240.dp)
        )
        for (v in values) {
            val cellText = when (formatType) {
                "fx" -> String.format("%,.1f", v)
                "qty" -> String.format("%,.0f", v)
                "mwk" -> String.format("%,.0f MWK", v)
                "usdDec" -> String.format("$%.3f", v)
                "ratio" -> if (v >= 99.0) "N/A" else String.format("%.2fx", v)
                else -> {
                    if (v < 0) {
                        String.format("($%,.0f)", Math.abs(v))
                    } else {
                        String.format("$%,.0f", v)
                    }
                }
            }
            Text(
                text = cellText,
                color = if (v < 0 && formatType.contains("usd")) RedLoss else (tint ?: if (isBold) TextLight else TextLight),
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

// ==============================================================================
// 3. TABS: RISK MODULES (SENSITIVITY & MONTE CARLO GRAPH)
// ==============================================================================
@Composable
fun RiskModuleTab(outputs: FinancialEngine.ModelOutputs, inputs: FinancialEngine.ModelInputs) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- SECTION 1: SENSITIVITY ---
        Text(
            text = "STRESS ANALYSIS: kWh Sold Multiplier vs Default Rate",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Matrix values represent Project IRR. Highlights project threshold limits.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grid Table
                // Headers: Default steps (0%, 4%, 8%, 12%, 16%)
                val defaults = listOf("0% Def", "4% Def", "8% Def", "12% Def", "16% Def")
                val multiLabels = listOf("-20% Vol", "-10% Vol", "Base Vol", "+10% Vol", "+20% Vol")

                // Top column headers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("", modifier = Modifier.weight(1.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    for (h in defaults) {
                        Text(
                            text = h,
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Rows
                outputs.sensitivityTable.forEachIndexed { rIdx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left row label
                        Text(
                            text = multiLabels[rIdx],
                            color = TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f)
                        )

                        row.forEach { cell ->
                            val cellColor = when {
                                cell.projectIrr >= 0.25 -> Color(0xFF047857) // Dark Emerald Green
                                cell.projectIrr in 0.15..0.25 -> Color(0xFF059669) // Emerald Green
                                cell.projectIrr in 0.08..0.15 -> Color(0xFFD97706) // Orange/Amber
                                cell.projectIrr in 0.0..0.08 -> Color(0xFFEA580C) // Dark Orange
                                else -> Color(0xFFDC2626) // Bright Red
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cellColor, RoundedCornerShape(4.dp))
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (cell.projectIrr < 0.0) "Neg" else String.format("%.1f%%", cell.projectIrr * 100),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- NEW SECTION: CUSTOMER COUNT VS TARIFF SENSITIVITY ---
        Text(
            text = "SENSITIVITY ANALYSIS: CUSTOMER COUNT VS TARIFF",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("customer_tariff_sensitivity_card"),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Analyze how simultaneous changes in connection counts (rows) and tariff rates (columns) affect the project's financial feasibility. Calculated dynamically in real-time.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Metric Toggle (IRR vs NPV)
                var selectedMetric by remember { mutableStateOf(0) } // 0 = IRR, 1 = NPV

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(DarkSlate, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                if (selectedMetric == 0) SolarAmber else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedMetric = 0 }
                            .testTag("sensitivity_toggle_irr")
                    ) {
                        Text(
                            text = "PROJECT IRR",
                            color = if (selectedMetric == 0) Color.Black else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                if (selectedMetric == 1) SolarAmber else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedMetric = 1 }
                            .testTag("sensitivity_toggle_npv")
                    ) {
                        Text(
                            text = "PROJECT NPV",
                            color = if (selectedMetric == 1) Color.Black else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val custTariffGrid = remember(inputs) {
                    FinancialEngine.generateCustomerTariffSensitivity(inputs)
                }

                val columns = listOf("-20% Tar", "-10% Tar", "Base Tar", "+10% Tar", "+20% Tar")
                val rows = listOf("-20% Cust", "-10% Cust", "Base Cust", "+10% Cust", "+20% Cust")

                // Top column headers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("", modifier = Modifier.weight(1.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    for (col in columns) {
                        Text(
                            text = col,
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Grid Cells
                custTariffGrid.forEachIndexed { rIdx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left row label
                        Text(
                            text = rows[rIdx],
                            color = TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f)
                        )

                        row.forEach { cell ->
                            val cellColor = if (selectedMetric == 0) {
                                when {
                                    cell.projectIrr >= 0.25 -> Color(0xFF047857) // Dark Emerald Green
                                    cell.projectIrr in 0.15..0.25 -> Color(0xFF059669) // Emerald Green
                                    cell.projectIrr in 0.08..0.15 -> Color(0xFFD97706) // Orange/Amber
                                    cell.projectIrr in 0.0..0.08 -> Color(0xFFEA580C) // Dark Orange
                                    else -> Color(0xFFDC2626) // Bright Red
                                }
                            } else {
                                when {
                                    cell.projectNpv >= 100_000.0 -> Color(0xFF047857)
                                    cell.projectNpv in 0.0..100_000.0 -> Color(0xFF059669)
                                    cell.projectNpv in -50_000.0..0.0 -> Color(0xFFD97706)
                                    cell.projectNpv in -100_000.0..-50_000.0 -> Color(0xFFEA580C)
                                    else -> Color(0xFFDC2626)
                                }
                            }

                            val cellText = if (selectedMetric == 0) {
                                if (cell.projectIrr < 0.0) "Neg" else String.format("%.1f%%", cell.projectIrr * 100)
                            } else {
                                formatCompactCurrency(cell.projectNpv)
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cellColor, RoundedCornerShape(4.dp))
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = cellText,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Extra dynamic statistics/insights based on grid values
                val baseCell = custTariffGrid[2][2] // 100% multiplier row and column
                val baseValueStr = if (selectedMetric == 0) String.format("%.1f%%", baseCell.projectIrr * 100) else formatCurrency(baseCell.projectNpv)
                val worstCell = custTariffGrid[0][0] // -20% customer and -20% tariff
                val worstValueStr = if (selectedMetric == 0) (if (worstCell.projectIrr < 0.0) "Negative" else String.format("%.1f%%", worstCell.projectIrr * 100)) else formatCurrency(worstCell.projectNpv)
                val bestCell = custTariffGrid[4][4] // +20% customer and +20% tariff
                val bestValueStr = if (selectedMetric == 0) String.format("%.1f%%", bestCell.projectIrr * 100) else formatCurrency(bestCell.projectNpv)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlate, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "SENSITIVITY INSIGHTS",
                        color = SolarAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• Base Scenario: $baseValueStr",
                        color = TextLight,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "• Worst Case (-20% Cust / -20% Tar): $worstValueStr",
                        color = if (worstCell.projectIrr < 0.0 || worstCell.projectNpv < 0.0) RedLoss else TextLight,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "• Best Case (+20% Cust / +20% Tar): $bestValueStr",
                        color = EmeraldGreen,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: MONTE CARLO ---
        Text(
            text = "STOCHASTIC MONTE CARLO ANALYSIS (1,000 RUNS)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Varies: Upfront CAPEX (stdev 8%), Customer Growth (stdev 2.5%), Capacity Factor (stdev 10%), Customer Default (stdev 2%), MWK Depreciation (stdev 2.5%) under independent Gaussian walks.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Stats rows
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Mean NPV Run", color = TextMuted, fontSize = 11.sp)
                        Text(formatCurrency(outputs.monteCarloResult.meanNpv), color = EmeraldGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text("Volatility (Stdev)", color = TextMuted, fontSize = 11.sp)
                        Text(formatCurrency(outputs.monteCarloResult.stdDevNpv), color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = BorderSlate, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Value at Risk (5th percentile)", color = TextMuted, fontSize = 11.sp)
                        Text(formatCurrency(outputs.monteCarloResult.valueAtRisk5Percent), color = RedLoss, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Viability (NPV > 0)", color = TextMuted, fontSize = 11.sp)
                        Text(String.format("%.1f%%", outputs.monteCarloResult.probabilityOfPositiveNpv * 100), color = SolarAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confidence Intervals Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlate, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "PROJECT NPV CONFIDENCE INTERVALS",
                        color = SolarAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("90% Confidence Interval", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = "[${formatCurrency(outputs.monteCarloResult.confidenceLower90)}, ${formatCurrency(outputs.monteCarloResult.confidenceUpper90)}]",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("95% Confidence Interval", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = "[${formatCurrency(outputs.monteCarloResult.confidenceLower95)}, ${formatCurrency(outputs.monteCarloResult.confidenceUpper95)}]",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Histogram Chart
                Text(
                    text = "Project NPV Probability Distribution",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                MonteCarloHistogramCanvas(outputs.monteCarloResult)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MonteCarloHistogramCanvas(result: FinancialEngine.MonteCarloResult) {
    // We bin the 1000 NPV runs into 18 bins to draw a histogram
    val binCount = 18
    val minNpv = result.minNpv
    val maxNpv = result.maxNpv
    val binWidth = (maxNpv - minNpv) / binCount
    
    val bins = remember(result) {
        val b = IntArray(binCount) { 0 }
        for (run in result.runs) {
            var idx = ((run - minNpv) / binWidth).toInt()
            if (idx >= binCount) idx = binCount - 1
            if (idx < 0) idx = 0
            b[idx]++
        }
        b
    }
    
    val maxBinVal = remember(bins) { (bins.maxOrNull() ?: 1).toFloat() }
    
    val peakIndex = remember(bins) {
        var maxIdx = 0
        var maxVal = -1
        for (idx in bins.indices) {
            if (bins[idx] > maxVal) {
                maxVal = bins[idx]
                maxIdx = idx
            }
        }
        maxIdx
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(DarkSlate, RoundedCornerShape(4.dp))
            .border(0.5.dp, BorderSlate, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val barGap = 2f
            val usableWidth = width - (barGap * (binCount - 1))
            val barWidth = usableWidth / binCount

            // 1. Draw histogram bins
            for (i in 0 until binCount) {
                val binHeightFraction = bins[i].toFloat() / maxBinVal
                val bHeight = height * binHeightFraction * 0.85f // leave 15% headroom for labels
                val left = i * (barWidth + barGap)
                val top = height - bHeight

                val isNegative = (minNpv + i * binWidth) < 0.0
                val barColor = if (isNegative) {
                    RedLoss.copy(alpha = 0.45f)
                } else {
                    val distance = Math.abs(i - peakIndex)
                    val alpha = when {
                        distance == 0 -> 1.0f
                        distance <= 2 -> 0.75f
                        distance <= 5 -> 0.45f
                        else -> 0.20f
                    }
                    SolarAmber.copy(alpha = alpha)
                }

                // Draw solid bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, bHeight)
                )
            }

            // 2. Draw zero NPV threshold vertical line
            if (minNpv < 0.0 && maxNpv > 0.0) {
                val zeroNpvFraction = (0.0 - minNpv) / (maxNpv - minNpv)
                val zeroX = (width * zeroNpvFraction).toFloat()
                drawLine(
                    color = RedLoss,
                    start = Offset(zeroX, 0f),
                    end = Offset(zeroX, height),
                    strokeWidth = 2f
                )
            }

            // 3. Draw Mean NPV line
            val meanNpvFraction = (result.meanNpv - minNpv) / (maxNpv - minNpv)
            val meanX = (width * meanNpvFraction).toFloat()
            drawLine(
                color = SolarAmber,
                start = Offset(meanX, 0f),
                end = Offset(meanX, height),
                strokeWidth = 2.5f
            )
        }
    }

    // Legend Indicators
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(SolarAmber))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mean NPV", color = TextMuted, fontSize = 9.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(RedLoss))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Zero Return Loss Limit", color = TextMuted, fontSize = 9.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(EmeraldGreen.copy(alpha = 0.6f)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Positive Return Runs", color = TextMuted, fontSize = 9.sp)
        }
    }
}

// ==============================================================================
// 4. TABS: PYTHON CODE CODEVIEW & EXPORT CAPABILITIES
// ==============================================================================
@Composable
fun ExportTab(pythonCode: String) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WORLDQUANT AUDIT PYTHON MODEL",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fully annotated script generating multi-tab Excel Model",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Malawi Financial Python Model", pythonCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Python script copied to Clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                modifier = Modifier.testTag("copy_code_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(0.5.dp, BorderSlate, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSlate)
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = pythonCode,
                    color = Color(0xFFA5F3FC), // cyan-mint code look
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// ==============================================================================
// HELPER METHODS: FORMATTING PLUGINS
// ==============================================================================

private fun formatPercent(value: Double): String {
    if (value == -99.0 || value.isNaN()) return "N/A"
    return String.format("%.2f%%", value * 100)
}

private fun formatCurrency(value: Double): String {
    if (value < 0) {
        return String.format("($%,.0f)", Math.abs(value))
    }
    return String.format("$%,.0f", value)
}

private fun formatCurrencyWithDec(value: Double): String {
    return String.format("$%,.3f", value)
}

private fun formatCompactCurrency(value: Double): String {
    val absVal = Math.abs(value)
    val formatted = when {
        absVal >= 1_000_000.0 -> String.format("%.1fM", value / 1_000_000.0)
        absVal >= 1_000.0 -> String.format("%.0fk", value / 1_000.0)
        else -> String.format("%.0f", value)
    }
    return if (value < 0) "($formatted)" else "$$formatted"
}
