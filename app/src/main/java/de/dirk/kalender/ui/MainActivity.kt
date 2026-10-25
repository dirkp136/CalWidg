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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import de.dirk.kalender.data.WidgetUpdater
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dirk.kalender.data.CalInfo
import de.dirk.kalender.data.CalendarRepo
import de.dirk.kalender.data.Place
import de.dirk.kalender.data.Prefs
import de.dirk.kalender.data.Scheduler
import de.dirk.kalender.data.WeatherRepo
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
        setContent {
            val ctx = LocalContext.current
            val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            MaterialTheme(colorScheme = scheme) { SettingsScreen() }
        }
    }
}

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

    fun pin(cls: Class<*>) {
        val mgr = ctx.getSystemService(AppWidgetManager::class.java)
        if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(ctx, cls), null, null)
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("Widgets", Icons.Outlined.Widgets, Icons.Filled.Widgets),
        Triple("Kalender", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        Triple("Anleitung", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    )

    Scaffold(
        topBar = {
            Text(
                "CalWidg",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, outlined, filled) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(if (tab == i) filled else outlined, contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { inner ->
        if (tab == 2) {
            GuideScreen(Modifier.fillMaxSize().padding(inner))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (tab == 0) {
                if (!granted) item {
                    Section("Kalenderzugriff fehlt") {
                        Text("Ohne Zugriff zeigen die Widgets keine Termine.")
                        FilledTonalButton(onClick = { tab = 1 }) { Text("Zum Tab Kalender") }
                    }
                }

                item {
                    Section("Widget hinzufügen") {
                        // 2 x 2 grid, fits the narrow cover screen as well
                        val kinds = listOf(
                            "Datum" to DateWidgetReceiver::class.java, "Monat" to MonthWidgetReceiver::class.java,
                            "Agenda" to AgendaWidgetReceiver::class.java, "Kombi" to ComboWidgetReceiver::class.java
                        )
                        kinds.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (label, cls) ->
                                    FilledTonalButton(onClick = { pin(cls) }, modifier = Modifier.weight(1f)) { Text(label) }
                                }
                            }
                        }
                        Text(
                            "Alternativ lange auf den Startbildschirm drücken › Widgets › CalWidg. Größe frei anpassbar.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Section("Darstellung neuer Widgets") {
                        Text(
                            "Gilt für Widgets, die du ab jetzt hinzufügst. Bereits platzierte behalten ihr Aussehen.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val options = listOf("System", "Hell", "Dunkel")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            options.forEachIndexed { i, label ->
                                SegmentedButton(
                                    selected = theme == i,
                                    onClick = { theme = i; prefs.theme = i },
                                    shape = SegmentedButtonDefaults.itemShape(i, options.size)
                                ) { Text(label) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Glas-Effekt")
                                Text(
                                    "Transparent mit Glaskante, der Hintergrund scheint durch",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = glass, onCheckedChange = { glass = it; prefs.glass = it })
                        }
                        TextButton(onClick = { scope.launch { WidgetUpdater.applyStyleToAll(ctx) } }) {
                            Text("Auf alle platzierten Widgets anwenden")
                        }
                    }
                }

                item {
                    Section("Wetter") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Wetter anzeigen", modifier = Modifier.weight(1f))
                            Switch(checked = weatherOn, onCheckedChange = {
                                weatherOn = it; prefs.weatherEnabled = it; Scheduler.refreshNow(ctx)
                            })
                        }
                        if (weatherOn) {
                            Text("Ort: $place", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                label = { Text("Ort suchen") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            LaunchedEffect(query) {
                                results = if (query.trim().length < 2) emptyList()
                                else withContext(Dispatchers.IO) { runCatching { WeatherRepo.searchPlaces(query) }.getOrDefault(emptyList()) }
                            }
                            results.forEach { p ->
                                Column(Modifier.fillMaxWidth().clickable {
                                    prefs.placeName = p.name; prefs.lat = p.lat; prefs.lon = p.lon
                                    place = p.name; query = ""; results = emptyList()
                                    Scheduler.refreshNow(ctx)
                                }.padding(vertical = 8.dp)) {
                                    Text(p.name)
                                    Text(p.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    Button(onClick = { Scheduler.refreshNow(ctx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Widgets jetzt aktualisieren")
                    }
                }
            } else {
                if (!granted) item {
                    Section("Kalenderzugriff") {
                        Text("Die Widgets lesen deine Termine aus dem Kalender des Handys, also auch alles, was dort mit Google synchronisiert wird.")
                        Button(onClick = { permLauncher.launch(Manifest.permission.READ_CALENDAR) }) { Text("Zugriff erlauben") }
                    }
                }

                if (granted) item {
                    Section("Angezeigte Kalender") {
                        if (calendars.isEmpty()) Text("Keine Kalender gefunden.")
                        calendars.groupBy { it.account }.forEach { (account, list) ->
                            Text(account, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            list.forEach { cal ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selected = if (cal.id in selected) selected - cal.id else selected + cal.id
                                        prefs.selectedCalendars = selected
                                        Scheduler.refreshNow(ctx)
                                    }.padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked = cal.id in selected, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(12.dp).background(Color(cal.color), CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(cal.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
