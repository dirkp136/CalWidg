package de.dirk.kalender.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dirk.kalender.data.CalInfo
import de.dirk.kalender.data.CalendarRepo
import de.dirk.kalender.data.Place
import de.dirk.kalender.data.Prefs
import de.dirk.kalender.data.Scheduler
import de.dirk.kalender.data.WeatherRepo
import de.dirk.kalender.data.WidgetUpdater
import de.dirk.kalender.widget.AgendaWidgetReceiver
import de.dirk.kalender.widget.ComboWidgetReceiver
import de.dirk.kalender.widget.DateWidgetReceiver
import de.dirk.kalender.widget.MonthWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OneUiTheme { SettingsScreen() } }
    }
}

private val tabs = listOf(
    Triple("Widgets", Icons.Outlined.Widgets, Icons.Filled.Widgets),
    Triple("Kalender", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Triple("Anleitung", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
)
private val subtitles = listOf(
    "Hinzufügen, Aussehen und Wetter",
    "Welche Termine in den Widgets erscheinen",
    "So richtest du CalWidg ein",
)

@Composable
private fun SettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(CalendarRepo.hasPermission(ctx)) }
    var calendars by remember { mutableStateOf(emptyList<CalInfo>()) }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var theme by remember { mutableStateOf(prefs.theme) }
    var glass by remember { mutableStateOf(prefs.glass) }
    var weatherOn by remember { mutableStateOf(prefs.weatherEnabled) }
    var place by remember { mutableStateOf(prefs.placeName) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Place>()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) { Scheduler.armCalendarTrigger(ctx); Scheduler.refreshNow(ctx) }
    }

    LaunchedEffect(granted) {
        if (granted) {
            calendars = withContext(Dispatchers.IO) { CalendarRepo.calendars(ctx) }
            selected = withContext(Dispatchers.IO) { CalendarRepo.selectedIds(ctx) }
        }
    }
    LaunchedEffect(query) {
        results = if (query.trim().length < 2) emptyList()
        else withContext(Dispatchers.IO) { runCatching { WeatherRepo.searchPlaces(query) }.getOrDefault(emptyList()) }
    }

    fun pin(cls: Class<*>) {
        val mgr = ctx.getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(ctx, cls), null, null)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(tab) { listState.scrollToItem(0) }
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomBar(tabs, tab) { tab = it } },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = inner.calculateTopPadding(), bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Box(Modifier.statusBarsPadding()) { BigTitle(tabs[tab].first, subtitles[tab]) } }

                when (tab) {
                    0 -> widgetsTab(
                        granted = granted, onGoToCalendars = { tab = 1 },
                        pin = ::pin,
                        theme = theme, onTheme = { theme = it; prefs.theme = it },
                        glass = glass, onGlass = { glass = it; prefs.glass = it },
                        onApplyAll = { scope.launch { WidgetUpdater.applyStyleToAll(ctx) } },
                        weatherOn = weatherOn, onWeather = { weatherOn = it; prefs.weatherEnabled = it; Scheduler.refreshNow(ctx) },
                        place = place, query = query, onQuery = { query = it }, results = results,
                        onPick = { p ->
                            prefs.placeName = p.name; prefs.lat = p.lat; prefs.lon = p.lon
                            place = p.name; query = ""; results = emptyList()
                            Scheduler.refreshNow(ctx)
                        },
                        onRefresh = { Scheduler.refreshNow(ctx) },
                        accent = accent,
                    )
                    1 -> calendarsTab(
                        granted = granted,
                        onGrant = { permLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        calendars = calendars, selected = selected,
                        onToggle = { id ->
                            selected = if (id in selected) selected - id else selected + id
                            prefs.selectedCalendars = selected
                            Scheduler.refreshNow(ctx)
                        },
                    )
                    else -> guideItems()
                }
            }
            CollapsedBar(tabs[tab].first, collapsed)
        }
    }
}

private fun LazyListScope.widgetsTab(
    granted: Boolean, onGoToCalendars: () -> Unit,
    pin: (Class<*>) -> Unit,
    theme: Int, onTheme: (Int) -> Unit,
    glass: Boolean, onGlass: (Boolean) -> Unit, onApplyAll: () -> Unit,
    weatherOn: Boolean, onWeather: (Boolean) -> Unit,
    place: String, query: String, onQuery: (String) -> Unit, results: List<Place>, onPick: (Place) -> Unit,
    onRefresh: () -> Unit, accent: Color,
) {
    if (!granted) item {
        Card("Kalenderzugriff fehlt") {
            SubText("Ohne Zugriff zeigen die Widgets keine Termine.")
            PillButton("Zum Tab Kalender", filled = false, onClick = onGoToCalendars)
        }
    }

    item {
        Card("Widget hinzufügen") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Datum", Icons.Outlined.Today, Color(0xFFFF6B4A)) { pin(DateWidgetReceiver::class.java) }
                Tile("Monat", Icons.Outlined.CalendarViewMonth, accent) { pin(MonthWidgetReceiver::class.java) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Agenda", Icons.Outlined.ViewAgenda, Color(0xFF3E8BFF)) { pin(AgendaWidgetReceiver::class.java) }
                Tile("Kombi", Icons.Outlined.Dashboard, Color(0xFF9B6BFF)) { pin(ComboWidgetReceiver::class.java) }
            }
            SubText("Alternativ lange auf den Startbildschirm drücken › Widgets › CalWidg. Größe frei anpassbar.")
        }
    }

    item {
        Card("Darstellung neuer Widgets") {
            SubText("Gilt für Widgets, die du ab jetzt hinzufügst. Bereits platzierte behalten ihr Aussehen.")
            Segmented(listOf("System", "Hell", "Dunkel"), theme, onTheme)
            RowDivider()
            SwitchRow(
                "Glas-Effekt", "Transparent mit Glaskante, der Hintergrund scheint durch", glass, onGlass,
                lead = { RoundIcon(Icons.Outlined.Layers, Color(0xFF3E8BFF), 36) }
            )
            RowDivider()
            Text(
                "Auf alle platzierten Widgets anwenden", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = accent, modifier = Modifier.clickable(onClick = onApplyAll).padding(vertical = 6.dp)
            )
        }
    }

    item {
        Card("Wetter") {
            SwitchRow("Wetter anzeigen", "Ort: $place", weatherOn, onWeather,
                lead = { RoundIcon(Icons.Outlined.WbSunny, Color(0xFFFFA726), 36) })
            if (weatherOn) {
                TextField(
                    value = query, onValueChange = onQuery, singleLine = true,
                    placeholder = { Text("Ort suchen") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                results.forEachIndexed { i, p ->
                    if (i > 0) RowDivider()
                    Column(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 8.dp)) {
                        Text(p.name, fontSize = 16.sp)
                        SubText(p.region)
                    }
                }
            }
        }
    }

    item {
        PillButton("Widgets jetzt aktualisieren", modifier = Modifier.fillMaxWidth(), onClick = onRefresh)
    }
}

private fun LazyListScope.calendarsTab(
    granted: Boolean, onGrant: () -> Unit,
    calendars: List<CalInfo>, selected: Set<Long>, onToggle: (Long) -> Unit,
) {
    if (!granted) {
        item {
            Card("Kalenderzugriff") {
                SubText("Die Widgets lesen deine Termine aus dem Kalender des Handys, also auch alles, was dort mit Google synchronisiert wird.")
                PillButton("Zugriff erlauben", onClick = onGrant)
            }
        }
        return
    }
    if (calendars.isEmpty()) item { Card { SubText("Keine Kalender gefunden.") } }
    calendars.groupBy { it.account }.forEach { (account, list) ->
        item {
            Text(
                account, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
            )
        }
        item {
            Card {
                list.forEachIndexed { i, cal ->
                    if (i > 0) RowDivider()
                    SwitchRow(cal.name, checked = cal.id in selected, onChange = { onToggle(cal.id) }, lead = {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(cal.color)))
                    })
                }
            }
        }
    }
}
