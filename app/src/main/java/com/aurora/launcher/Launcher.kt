package com.aurora.launcher

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val COLS = 4
private val IconShape = RoundedCornerShape(percent = 24)

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Frosted-glass look: translucent gradient fill plus a thin light edge. */
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f * strength),
                Color.White.copy(alpha = 0.08f * strength)
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.04f))
        ),
        shape
    )

fun startApp(context: Context, view: View, app: AppEntry, bounds: Rect?) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(app.packageName, app.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
    try {
        val options = if (bounds != null) {
            val loc = IntArray(2)
            view.getLocationInWindow(loc)
            ActivityOptions.makeScaleUpAnimation(
                view,
                (bounds.left - loc[0]).toInt(),
                (bounds.top - loc[1]).toInt(),
                bounds.width.toInt(),
                bounds.height.toInt()
            )
        } else {
            ActivityOptions.makeBasic()
        }
        context.startActivity(intent, options.toBundle())
    } catch (_: Exception) {
        // App was removed or cannot be started; ignore.
    }
}

// ---------------------------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------------------------

@Composable
fun LauncherApp(homePress: Int) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        LauncherRoot(homePress)
    }
}

@Composable
private fun LauncherRoot(homePress: Int) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val cfg = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    // ---- installed apps ----
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                reload++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(reload) { apps = AppRepo.load(context) }

    val dockPkgs = remember { AppRepo.dockPackages(context) }
    val dock = remember(apps) {
        dockPkgs.mapNotNull { p -> apps.firstOrNull { it.packageName == p } }
    }
    val gridApps = remember(apps, dock) {
        apps.filter { a -> dock.none { it.key == a.key } }
    }

    // ---- paging (rows depend on screen height; first page has room for the clock card) ----
    val available = cfg.screenHeightDp - 290
    val rows = (available / 100).coerceIn(3, 7)
    val rowsFirst = ((available - 175) / 100).coerceIn(2, 7)
    val pages = remember(gridApps, rows, rowsFirst) {
        val first = gridApps.take(COLS * rowsFirst)
        val rest = gridApps.drop(COLS * rowsFirst).chunked(COLS * rows)
        listOf(first) + rest
    }
    val pagesState = rememberUpdatedState(pages)
    val pagerState = rememberPagerState(pageCount = { pagesState.value.size })

    // ---- app drawer state: 0 = closed, 1 = fully open ----
    val progress = remember { Animatable(0f) }
    val drawerVisible by remember { derivedStateOf { progress.value > 0.001f } }
    val screenH = with(density) { cfg.screenHeightDp.dp.toPx() }
    val dragDistance = screenH * 0.55f
    var focusToken by remember { mutableIntStateOf(0) }

    fun snap(value: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            progress.snapTo(value.coerceIn(0f, 1f))
        }
    }

    fun settleTo(target: Float) {
        scope.launch {
            progress.animateTo(target, spring(dampingRatio = 0.88f, stiffness = 280f))
        }
    }

    val launchApp = remember(context, view) {
        { app: AppEntry, bounds: Rect? -> startApp(context, view, app, bounds) }
    }

    LaunchedEffect(homePress) {
        if (homePress > 0) {
            progress.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 280f))
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler {
        if (drawerVisible) settleTo(0f) else scope.launch { pagerState.animateScrollToPage(0) }
    }

    val dockIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        dockIn.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 120f))
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF07070D))) {
        AuroraBackground(scroll = { pagerState.currentPage + pagerState.currentPageOffsetFraction })

        // Darkens the wallpaper while the drawer opens.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.6f * progress.value.coerceIn(0f, 1f)))
                }
        )

        // ---------------- Home screen ----------------
        Column(
            Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { dy -> snap(progress.value - dy / dragDistance) },
                    onDragStopped = { velocity ->
                        val open = velocity < -900f || (velocity < 900f && progress.value > 0.4f)
                        settleTo(if (open) 1f else 0f)
                    }
                )
                .graphicsLayer {
                    val p = progress.value
                    val s = 1f - 0.12f * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - p * 1.3f).coerceIn(0f, 1f)
                }
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val a = abs(offset).coerceIn(0f, 1f)
                                alpha = 1f - a * 0.7f
                                val s = 1f - a * 0.10f
                                scaleX = s
                                scaleY = s
                                rotationY = offset * -16f
                                cameraDistance = 14f * density.density
                            }
                    ) {
                        HomePage(
                            apps = pages.getOrElse(page) { emptyList() },
                            showClock = page == 0,
                            onLaunch = launchApp
                        )
                    }
                }
            }

            PageDots(pagerState)
            Spacer(Modifier.height(10.dp))
            SearchPill {
                focusToken++
                settleTo(1f)
            }
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier
                    .graphicsLayer { translationY = (1f - dockIn.value) * 260f }
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(34.dp), 1.2f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dock.forEachIndexed { i, app ->
                    AppIcon(app, index = i, showLabel = false, animateIn = true, onLaunch = launchApp)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(10.dp))
        }

        // ---------------- App drawer ----------------
        if (drawerVisible) {
            AppDrawer(
                apps = apps,
                progress = progress,
                focusToken = focusToken,
                dragDistance = dragDistance,
                scope = scope,
                onLaunch = { app, bounds ->
                    launchApp(app, bounds)
                    scope.launch {
                        delay(450)
                        progress.snapTo(0f)
                    }
                },
                onClose = { settleTo(0f) }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Home page (clock card + icon grid)
// ---------------------------------------------------------------------------------------------

@Composable
private fun HomePage(apps: List<AppEntry>, showClock: Boolean, onLaunch: (AppEntry, Rect?) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        if (showClock) {
            ClockCard(Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        apps.chunked(COLS).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until COLS) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        val app = row.getOrNull(col)
                        if (app != null) {
                            AppIcon(
                                app = app,
                                index = rowIndex * COLS + col,
                                showLabel = true,
                                animateIn = true,
                                onLaunch = onLaunch
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Icon with bounce, long-press menu and zoom-launch
// ---------------------------------------------------------------------------------------------

@Composable
private fun AppIcon(
    app: AppEntry,
    index: Int,
    showLabel: Boolean,
    animateIn: Boolean,
    onLaunch: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }

    val press by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) {
            delay(index * 24L)
            enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }
    }

    Column(
        modifier
            .graphicsLayer {
                val s = press * enter.value
                scaleX = s
                scaleY = s
                alpha = enter.value.coerceIn(0f, 1f)
            }
            .pointerInput(app) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onLaunch(app, bounds) },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    }
                )
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(60.dp)
                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                    .shadow(
                        elevation = 8.dp,
                        shape = IconShape,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("App info") },
                    onClick = {
                        menu = false
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = {
                        menu = false
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = app.label,
                modifier = Modifier.width(76.dp),
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 6f))
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Clock card
// ---------------------------------------------------------------------------------------------

@Composable
private fun ClockCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val time = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Good night"
    }

    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(30.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Text(greeting, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.Bottom) {
            time.forEachIndexed { i, ch ->
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "digit$i"
                ) { c ->
                    Text(c.toString(), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light)
                }
            }
        }
        Text(date, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------------------------
// Page dots and search pill
// ---------------------------------------------------------------------------------------------

@Composable
private fun PageDots(state: PagerState) {
    if (state.pageCount <= 1) {
        Spacer(Modifier.height(14.dp))
        return
    }
    Row(
        Modifier.fillMaxWidth().height(14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(state.pageCount) { i ->
            val active = state.currentPage == i
            val w: Dp by animateDpAsState(
                targetValue = if (active) 18.dp else 7.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "dotWidth"
            )
            val a by animateFloatAsState(if (active) 1f else 0.4f, label = "dotAlpha")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(7.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = a))
            )
        }
    }
}

@Composable
private fun SearchGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawCircle(
            color = color,
            radius = s * 0.34f,
            center = Offset(s * 0.42f, s * 0.42f),
            style = Stroke(width = s * 0.12f)
        )
        drawLine(
            color = color,
            start = Offset(s * 0.68f, s * 0.68f),
            end = Offset(s * 0.94f, s * 0.94f),
            strokeWidth = s * 0.12f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "pill"
    )
    Row(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(RoundedCornerShape(50), 0.9f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 22.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text("Search", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------------------------
// App drawer (swipe up from home)
// ---------------------------------------------------------------------------------------------

@Composable
private fun AppDrawer(
    apps: List<AppEntry>,
    progress: Animatable<Float, AnimationVector1D>,
    focusToken: Int,
    dragDistance: Float,
    scope: CoroutineScope,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focusToken) {
        if (focusToken > 0) {
            delay(280)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    // Dragging down at the top of the list pulls the drawer closed.
    val nested = remember(dragDistance) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && progress.value < 0.999f && source == NestedScrollSource.UserInput) {
                    val next = (progress.value - available.y / dragDistance).coerceAtMost(1f)
                    scope.launch(start = CoroutineStart.UNDISPATCHED) { progress.snapTo(next) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    val next = (progress.value - available.y / dragDistance).coerceIn(0f, 1f)
                    scope.launch(start = CoroutineStart.UNDISPATCHED) { progress.snapTo(next) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (progress.value > 0.001f && progress.value < 0.999f) {
                    val open = progress.value > 0.6f && available.y < 1200f
                    progress.animateTo(if (open) 1f else 0f, spring(dampingRatio = 0.88f, stiffness = 280f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * 60.dp.toPx()
                val s = 0.94f + 0.06f * p
                scaleX = s
                scaleY = s
            }
            .pointerInput(Unit) { } // blocks touches from reaching the home screen underneath
            .statusBarsPadding()
            .nestedScroll(nested)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    focusManager.clearFocus()
                    onClose()
                }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(46.dp)
                .glass(RoundedCornerShape(23.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchGlyph(Color.White.copy(alpha = 0.7f), Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Search apps", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        filtered.firstOrNull()?.let {
                            focusManager.clearFocus()
                            onLaunch(it, null)
                        }
                    }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                Text(
                    "✕",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { query = "" }.padding(6.dp)
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                Text("No apps found", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLS),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                items(filtered, key = { it.key }) { app ->
                    AppIcon(
                        app = app,
                        index = 0,
                        showLabel = true,
                        animateIn = false,
                        onLaunch = onLaunch,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
