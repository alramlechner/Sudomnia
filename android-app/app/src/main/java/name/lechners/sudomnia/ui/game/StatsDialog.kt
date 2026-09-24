package name.lechners.sudomnia.ui.game

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.History
import name.lechners.sudomnia.data.Outcome
import name.lechners.sudomnia.data.Stats
import name.lechners.sudomnia.data.Title
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.AppBackground
import name.lechners.sudomnia.ui.theme.AppOutline
import name.lechners.sudomnia.ui.theme.AppSurface
import name.lechners.sudomnia.ui.theme.LogoBlue
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

private val Good = Color(0xFF6BC46D)
private val Bad = Color(0xFFE07A70)

/** The statistics as a full screen: rating, trend, charts, then the plain counters. */
@Composable
fun StatsDialog(stats: Stats, history: History, onReset: () -> Unit, onDismiss: () -> Unit) {
    var askReset by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<Level?>(null) }

    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text(stringResource(R.string.stats_reset_ask)) },
            confirmButton = {
                TextButton(onClick = { askReset = false; onReset() }) {
                    Text(stringResource(R.string.stats_reset_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { askReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { askReset = true }) { Text(stringResource(R.string.stats_reset)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_done)) }
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.stats),
                    color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                )
                LevelChips(filter) { filter = it }
                Overview(stats, history, filter)
                RatingChart(history, filter)
                if (filter != null) TimeChart(history, filter!!)
                Calendar(history)
                Details(stats, history, filter)
                ShareButton(history, filter)
            }
        }
    }
}

@Composable
private fun LevelChips(selected: Level?, onSelect: (Level?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.stats_all)) },
        )
        for (l in Level.entries) {
            FilterChip(
                selected = selected == l,
                onClick = { onSelect(l) },
                label = { Text(levelLabel(l), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun Overview(stats: Stats, history: History, filter: Level?) {
    val rating = history.rating(filter)
    val trend = history.trend(filter)
    val solved = if (filter == null) stats.totalSolved else stats[filter].solved
    val played = history.forLevel(filter).size
    val quote = if (played == 0) null else 100 * history.count(Outcome.SOLVED, filter) / played

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(
                modifier = Modifier.weight(1.4f),
                label = stringResource(R.string.stats_rating),
                value = rating?.let { it.toString() + if (history.provisional(filter)) "?" else "" }
                    ?: stringResource(R.string.stats_none),
                sub = rating?.let { titleLabel(History.titleOf(it)) },
                extra = trend?.let { (if (it >= 0) "▲ +" else "▼ ") + it },
                extraColor = if ((trend ?: 0) >= 0) Good else Bad,
            )
            Tile(Modifier.weight(1f), stringResource(R.string.stats_solved), solved.toString())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(
                Modifier.weight(1f), stringResource(R.string.stats_quote),
                quote?.let { "$it %" } ?: stringResource(R.string.stats_none),
            )
            Tile(Modifier.weight(1f), stringResource(R.string.stats_streak), history.currentStreak(filter).toString())
            Tile(Modifier.weight(1f), stringResource(R.string.stats_best), formatDuration(bestMs(stats, filter)))
        }
    }
}

private fun bestMs(stats: Stats, filter: Level?): Long =
    if (filter != null) stats[filter].bestMs
    else Level.entries.map { stats[it].bestMs }.filter { it > 0 }.minOrNull() ?: 0L

@Composable
private fun Tile(
    modifier: Modifier,
    label: String,
    value: String,
    sub: String? = null,
    extra: String? = null,
    extraColor: Color = TextSecondary,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(AppSurface).padding(12.dp),
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = TextSecondary, fontSize = 13.sp)
        if (extra != null) Text(extra, color = extraColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppSurface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = TextSecondary, fontSize = 12.sp)
        content()
    }
}

@Composable
private fun Empty() =
    Text(stringResource(R.string.stats_no_data), color = TextSecondary, fontSize = 13.sp)

/** The rating after each game -- the one picture of "am I getting better". */
@Composable
private fun RatingChart(history: History, filter: Level?) {
    val curve = history.ratingCurve(filter).takeLast(60)
    ChartCard(stringResource(R.string.stats_rating_curve)) {
        if (curve.size < 2) { Empty(); return@ChartCard }
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val lo = curve.min() - 30
            val hi = curve.max() + 30
            val span = (hi - lo).coerceAtLeast(1.0)
            fun pt(i: Int, v: Double) = Offset(
                x = size.width * i / (curve.size - 1),
                y = (size.height * (1 - (v - lo) / span)).toFloat(),
            )
            val path = Path()
            curve.forEachIndexed { i, v -> pt(i, v).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
            drawPath(path, LogoBlue, style = Stroke(width = 3.dp.toPx()))
            drawCircle(LogoBlue, 4.dp.toPx(), pt(curve.lastIndex, curve.last()))
        }
        Text(
            stringResource(R.string.stats_range, curve.min().toInt(), curve.max().toInt()),
            color = TextSecondary, fontSize = 11.sp,
        )
    }
}

/** Solve times of one level: a dot per game, a line through the running median of ten. */
@Composable
private fun TimeChart(history: History, level: Level) {
    val times = history.solved(level).takeLast(40).map { it.durationMs.toDouble() }
    ChartCard(stringResource(R.string.stats_time_curve)) {
        if (times.size < 2) { Empty(); return@ChartCard }
        val medians = times.indices.map { i ->
            val w = times.subList(maxOf(0, i - 9), i + 1).sorted()
            if (w.size % 2 == 1) w[w.size / 2] else (w[w.size / 2 - 1] + w[w.size / 2]) / 2
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val hi = times.max()
            val lo = times.min()
            val span = (hi - lo).coerceAtLeast(1.0)
            fun pt(i: Int, v: Double) = Offset(
                x = size.width * i / (times.size - 1),
                // Faster (smaller) is higher: up means better, as on the rating chart.
                y = (size.height * (0.05 + 0.9 * (v - lo) / span)).toFloat(),
            )
            times.forEachIndexed { i, v -> drawCircle(TextSecondary, 3.dp.toPx(), pt(i, v)) }
            val path = Path()
            medians.forEachIndexed { i, v -> pt(i, v).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
            drawPath(path, LogoBlue, style = Stroke(width = 3.dp.toPx()))
        }
        Text(
            stringResource(
                R.string.stats_time_range,
                formatDuration(times.min().toLong()), formatDuration(times.max().toLong()),
            ),
            color = TextSecondary, fontSize = 11.sp,
        )
    }
}

/** Twelve weeks, one square a day; brighter means more games. */
@Composable
private fun Calendar(history: History) {
    val perDay = remember(history) { history.perDay() }
    val today = remember { LocalDate.now() }
    ChartCard(stringResource(R.string.stats_calendar, history.dayStreak(today))) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            val weeks = 12
            val gap = 3.dp.toPx()
            val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
            // Column = week, row = weekday (Monday on top); the last column is this week.
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            for (w in 0 until weeks) for (d in 0 until 7) {
                val date = monday.minusWeeks((weeks - 1 - w).toLong()).plusDays(d.toLong())
                if (date.isAfter(today)) continue
                val n = perDay[date] ?: 0
                val color = if (n == 0) AppOutline.copy(alpha = 0.5f)
                else LogoBlue.copy(alpha = (0.35f + 0.2f * n).coerceAtMost(1f))
                drawRect(color, Offset(w * (cell + gap), d * (cell + gap)), Size(cell, cell))
            }
        }
    }
}

@Composable
private fun Details(stats: Stats, history: History, filter: Level?) {
    Column {
        Line(stringResource(R.string.stats_median), formatDuration(history.medianSolveMs(filter) ?: 0L))
        Line(stringResource(R.string.stats_clean), history.cleanSolves(filter).toString())
        Line(stringResource(R.string.stats_lost), history.count(Outcome.LOST, filter).toString())
        Line(stringResource(R.string.stats_abandoned), history.count(Outcome.ABANDONED, filter).toString())
        Line(stringResource(R.string.stats_streak_best), history.longestStreak(filter).toString())
        Line(stringResource(R.string.stats_playtime), formatPlaytime(history.totalPlayMs(filter)))
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        val started = if (filter == null) Level.entries.sumOf { stats[it].started } else stats[filter].started
        val noAids = if (filter == null) Level.entries.sumOf { stats[it].solvedNoAids } else stats[filter].solvedNoAids
        val noHints = if (filter == null) Level.entries.sumOf { stats[it].solvedNoHints } else stats[filter].solvedNoHints
        Line(stringResource(R.string.stats_started), started.toString())
        Line(stringResource(R.string.stats_no_aids), noAids.toString())
        Line(stringResource(R.string.stats_no_hints), noHints.toString())
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp)
    }
}

/** Sends the rating as text through the system share sheet -- no permission, no account. */
@Composable
private fun ShareButton(history: History, filter: Level?) {
    val rating = history.rating(filter) ?: return
    val context = LocalContext.current
    val text = stringResource(
        R.string.stats_share_text, rating, titleLabel(History.titleOf(rating)), history.currentStreak(filter),
    )
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        TextButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, null))
        }) { Text(stringResource(R.string.stats_share)) }
    }
}

@Composable
internal fun titleLabel(t: Title): String = stringResource(
    when (t) {
        Title.BEGINNER -> R.string.title_beginner
        Title.ADVANCED -> R.string.title_advanced
        Title.SKILLED -> R.string.title_skilled
        Title.MASTER -> R.string.title_master
        Title.GRANDMASTER -> R.string.title_grandmaster
    },
)

@Composable
private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return stringResource(R.string.stats_none)
    val total = ms / 1000
    val m = total / 60
    val sec = total % 60
    return if (m >= 60) "%d:%02d:%02d".format(m / 60, m % 60, sec) else "%d:%02d".format(m, sec)
}

private fun formatPlaytime(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) "%d h %02d min".format(minutes / 60, minutes % 60) else "$minutes min"
}
