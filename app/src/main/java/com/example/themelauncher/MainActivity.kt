package com.example.themelauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Theme = wallpaper (gradient) + icon pack (hugis/backdrop/tint) + default layout
data class LTheme(
    val cat: String, val name: String, val wall: List<Color>, val accent: Color,
    val iconBg: Color, val shape: Shape, val text: Color,
    val mono: Boolean = false, val circle: Boolean = false, val sparkle: Boolean = false
)

fun g(vararg c: Long) = c.map { Color(it) }
val W = Color(0x44FFFFFF)

val themes = listOf(
    // Classic
    LTheme("Classic", "Sunset", g(0xFFFF9966, 0xFFFF5E62, 0xFF3A1C71), Color(0xFFFFD1A9), Color(0x55FFFFFF), CircleShape, Color.White),
    LTheme("Classic", "Ocean", g(0xFF2193B0, 0xFF6DD5ED, 0xFF0F2027), Color(0xFFBDF3FF), W, RoundedCornerShape(18.dp), Color.White),
    LTheme("Classic", "Forest", g(0xFF134E5E, 0xFF71B280, 0xFF0B2B20), Color(0xFFD7FFD9), W, CutCornerShape(14.dp), Color.White),
    // Anime-inspired (original palettes, walang copyrighted characters)
    LTheme("Anime", "Shonen Fire", g(0xFF1A0A00, 0xFFD84315, 0xFFFFB300), Color(0xFFFFE082), Color(0x55000000), CutCornerShape(12.dp), Color.White, sparkle = true),
    LTheme("Anime", "Kawaii Pastel", g(0xFFFFC1E3, 0xFFC9B6FF, 0xFFB5F0E0), Color(0xFFFF5FA2), Color(0x99FFFFFF), CircleShape, Color(0xFF5A2A6B), sparkle = true),
    LTheme("Anime", "Cyber Tokyo", g(0xFF05010F, 0xFF2B0A5E, 0xFFFF2E93), Color(0xFF00E5FF), Color(0x44000000), RoundedCornerShape(8.dp), Color(0xFF00E5FF), mono = true, sparkle = true),
    LTheme("Anime", "Moonlit Sakura", g(0xFF0D1B3E, 0xFF3B2A6B, 0xFFE8A0BF), Color(0xFFFFC2D9), Color(0x33FFFFFF), RoundedCornerShape(22.dp), Color.White, sparkle = true),
    // OS-style
    LTheme("OS", "iOS Style", g(0xFFDCE8FF, 0xFFF5F7FF, 0xFFFFE9F1), Color(0xFF007AFF), Color(0xCCFFFFFF), RoundedCornerShape(16.dp), Color(0xFF1C1C1E)),
    LTheme("OS", "Windows Tiles", g(0xFF0078D7, 0xFF005A9E), Color(0xFFFFFFFF), Color(0x33FFFFFF), RoundedCornerShape(0.dp), Color.White),
    LTheme("OS", "Material You", g(0xFF1F2A2E, 0xFF2E4A52, 0xFF4F7C84), Color(0xFFA8E6CF), Color(0x44A8E6CF), RoundedCornerShape(50), Color.White),
    LTheme("OS", "Terminal", g(0xFF000000, 0xFF001A00), Color(0xFF33FF33), Color(0x2233FF33), RoundedCornerShape(2.dp), Color(0xFF33FF33), mono = true),
    // Mondo (circle layout)
    LTheme("Mondo", "Mondo Galaxy", g(0xFF020111, 0xFF191654, 0xFF43C6AC), Color(0xFF9BF6FF), Color(0x33FFFFFF), CircleShape, Color.White, circle = true, sparkle = true),
    LTheme("Mondo", "Mondo Sakura", g(0xFFFFE3EC, 0xFFFFB3CF, 0xFFD6A2E8), Color(0xFFD81B60), Color(0x99FFFFFF), CircleShape, Color(0xFF6A1B3A), circle = true, sparkle = true)
)

class AppItem(val label: String, val pkg: String, val cls: String, val icon: ImageBitmap)

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0).map {
        AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name,
            it.loadIcon(pm).toBitmap(128, 128).asImageBitmap())
    }.sortedBy { it.label.lowercase() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        val apps = loadApps(this)
        setContent {
            Launcher(apps, prefs.getInt("theme", 0).coerceIn(0, themes.lastIndex), prefs.getBoolean("circle", false)) { t, c ->
                prefs.edit().putInt("theme", t).putBoolean("circle", c).apply()
            }
        }
    }
}

// Kinakalkula ang mga ring: (laki ng icon, [(radius, capacity)]) para magkasya lahat sa isang page
fun plan(maxR: Float, n: Int): Pair<Float, List<Pair<Float, Int>>> {
    var s = 46f
    while (true) {
        val out = mutableListOf<Pair<Float, Int>>()
        var r = 40f + s / 2 + 6
        var total = 0
        while (r + s / 2 <= maxR) {
            val c = (2 * Math.PI * r / (s + 4)).toInt()
            out += r to c; total += c; r += s + 6
        }
        if (total >= n || s <= 22f) return s to out
        s -= 2f
    }
}

@Composable
fun Ring(apps: List<AppItem>, t: LTheme, launch: (AppItem) -> Unit) {
    var rot by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val c = Offset(size.width / 2f, size.height / 2f)
                val a1 = atan2(change.previousPosition.y - c.y, change.previousPosition.x - c.x)
                val a2 = atan2(change.position.y - c.y, change.position.x - c.x)
                var d = Math.toDegrees((a2 - a1).toDouble()).toFloat()
                if (d > 180) d -= 360
                if (d < -180) d += 360
                rot += d
            }
        }
    ) {
        val den = LocalDensity.current.density
        val cx = maxWidth.value * den / 2
        val cy = maxHeight.value * den / 2
        val (s, rings) = plan(minOf(maxWidth.value, maxHeight.value) / 2, apps.size)
        Box(Modifier.align(Alignment.Center).size(72.dp).clip(CircleShape).background(t.iconBg), Alignment.Center) {
            Text(t.name, color = t.text, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        var idx = 0
        rings.forEachIndexed { k, (r, cap) ->
            val take = minOf(cap, apps.size - idx)
            if (take <= 0) return@forEachIndexed
            val slice = apps.subList(idx, idx + take)
            idx += take
            slice.forEachIndexed { j, a ->
                val ang = Math.toRadians((rot * (if (k % 2 == 0) 1 else -1) + 360.0 * j / take - 90))
                Box(
                    Modifier
                        .offset { IntOffset((cx + r * den * cos(ang) - s * den / 2).toInt(), (cy + r * den * sin(ang) - s * den / 2).toInt()) }
                        .size(s.dp).clip(t.shape).background(t.iconBg).clickable { launch(a) },
                    Alignment.Center
                ) {
                    Image(a.icon, a.label, Modifier.size((s * 0.72f).dp),
                        colorFilter = if (t.mono) ColorFilter.tint(t.accent) else null)
                }
            }
        }
    }
}

@Composable
fun Launcher(apps: List<AppItem>, startTheme: Int, startCircle: Boolean, save: (Int, Boolean) -> Unit) {
    val ctx = LocalContext.current
    var ti by remember { mutableIntStateOf(startTheme) }
    var circle by remember { mutableStateOf(startCircle) }
    var picker by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val t = themes[ti]
    val btn = ButtonDefaults.buttonColors(
        containerColor = t.accent,
        contentColor = if (t.accent.luminance() > 0.5f) Color.Black else Color.White)
    val launch: (AppItem) -> Unit = { a ->
        runCatching {
            ctx.startActivity(Intent().setClassName(a.pkg, a.cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    BackHandler(picker) { picker = false }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(t.wall))) {
        if (t.sparkle) Canvas(Modifier.fillMaxSize()) {
            val r = java.util.Random(7)
            repeat(45) {
                drawCircle(Color.White.copy(alpha = 0.15f + r.nextFloat() * 0.5f), 1.5f + r.nextFloat() * 4f,
                    Offset(r.nextFloat() * size.width, r.nextFloat() * size.height))
            }
        }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    q, { q = it }, Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Search", color = t.text.copy(alpha = .6f)) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = t.text, unfocusedTextColor = t.text,
                        focusedBorderColor = t.accent, unfocusedBorderColor = t.text.copy(alpha = .4f))
                )
                Spacer(Modifier.width(6.dp))
                Button({ circle = !circle; save(ti, circle) }, colors = btn, contentPadding = PaddingValues(12.dp)) {
                    Text(if (circle) "Grid" else "Ring")
                }
                Spacer(Modifier.width(6.dp))
                Button({ picker = !picker }, colors = btn, contentPadding = PaddingValues(12.dp)) { Text("Themes") }
            }
            if (picker) {
                LazyRow(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(themes) { i, th ->
                        Box(
                            Modifier.width(92.dp).height(72.dp).clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(th.wall))
                                .border(if (i == ti) 3.dp else 0.dp, Color.White, RoundedCornerShape(14.dp))
                                .clickable { ti = i; circle = th.circle; save(i, circle) },
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(Modifier.padding(6.dp)) {
                                Text(th.cat, color = Color.White.copy(alpha = .7f), fontSize = 9.sp)
                                Text(th.name, color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                if (circle) Ring(shown, t, launch)
                else LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(shown) { a ->
                        Column(Modifier.clickable { launch(a) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(60.dp).clip(t.shape).background(t.iconBg), Alignment.Center) {
                                Image(a.icon, a.label, Modifier.size(42.dp),
                                    colorFilter = if (t.mono) ColorFilter.tint(t.accent) else null)
                            }
                            Text(a.label, color = t.text, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
