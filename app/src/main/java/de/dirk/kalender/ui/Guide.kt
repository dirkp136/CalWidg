package de.dirk.kalender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Building blocks of the in-app guide (same content as the "CalWidg – Anleitung" doc). */
private sealed interface G {
    data class P(val text: String) : G
    data class Steps(val items: List<String>) : G
    data class Bullets(val items: List<String>) : G
    /** Label/value pairs, e.g. widget overview or problem/solution. */
    data class Pairs(val items: List<Pair<String, String>>) : G
}

private class GuideSection(val title: String, val blocks: List<G>)

private val guide = listOf(
    GuideSection("Erste Schritte", listOf(
        G.P("CalWidg zeigt deine Termine, den Monat und das Wetter als Widgets auf dem Startbildschirm. Einrichten dauert etwa zwei Minuten."),
        G.Steps(listOf(
            "Unten den Tab Kalender öffnen.",
            "„Zugriff erlauben“ antippen und bestätigen.",
            "Im Tab Widgets ein Widget hinzufügen."
        )),
        G.P("CalWidg liest die Termine aus dem Kalender des Handys. Alles, was dort mit Google, Samsung oder Exchange synchronisiert wird, erscheint also auch in den Widgets. Die Termine verlassen das Gerät nicht."),
        G.P("Ein Update installierst du über die vorhandene App. Widgets und Einstellungen bleiben erhalten.")
    )),
    GuideSection("Widget hinzufügen", listOf(
        G.Steps(listOf(
            "Im Tab Widgets unter „Widget hinzufügen“ auf Datum, Monat, Agenda oder Kombi tippen.",
            "Im Dialog des Startbildschirms „Hinzufügen“ bestätigen. Das Widget landet auf dem nächsten freien Platz."
        )),
        G.P("Alternativ lange auf eine freie Stelle des Startbildschirms drücken, dann Widgets › CalWidg und das gewünschte Widget an seinen Platz ziehen."),
        G.P("Größe ändern: lange auf das Widget drücken, dann an den Rahmenpunkten ziehen. Schrift und Aufteilung passen sich an. Das Monats-Widget zeigt ab etwa 3×3 bis 4×4 Feldern (je nach Raster des Startbildschirms) zusätzlich einen großen Datumskopf mit Temperatur.")
    )),
    GuideSection("Die Widgets", listOf(
        G.Pairs(listOf(
            "Datum · 2×2, bis 1×1" to "Wochentag, Monat und Tag in großer Schrift. Antippen öffnet heute im Kalender.",
            "Monat · 2×2" to "Monatsraster, aktuelle Woche hinterlegt, heute blau. Tag antippen öffnet den Tag; ‹ › blättern, der Monatsname springt zurück zu heute.",
            "Heute & Morgen · 4×2" to "Termine von heute und morgen, danach weitere Tage mit Terminen, dazu das Tageswetter. Termin antippen öffnet ihn, „+ N weitere“ öffnet heute, + legt einen Termin an.",
            "Kombi · 4×2" to "Monat links, Agenda rechts. Bedienung wie bei Monat und Heute & Morgen."
        )),
        G.P("Die Färbung im Monat zeigt die Auslastung: je dunkler das Gelb bis Orange, desto mehr Termine (1, 2, 3, ab 4). Gezählt werden alle Termine des Tages, auch erledigte."),
        G.P("In der Agenda stehen ganztägige Termine als farbige Balken oben. Termine mit Uhrzeit verschwinden, sobald sie vorbei sind, ganztägige erst am Ende des Tages. Abgelehnte Einladungen werden nicht angezeigt. Passt nicht alles hinein, steht unten „+ N weitere“.")
    )),
    GuideSection("Darstellung", listOf(
        G.P("Jedes Widget merkt sich beim Hinzufügen sein Aussehen und behält es. So kannst du hell, dunkel und Glas nebeneinander verwenden."),
        G.Steps(listOf(
            "Im Tab Widgets unter „Darstellung neuer Widgets“ System, Hell oder Dunkel wählen. System folgt dem Dunkelmodus des Handys.",
            "Optional den Glas-Effekt einschalten: transparenter Hintergrund mit heller Kante.",
            "Danach das Widget hinzufügen."
        )),
        G.P("Ein bereits platziertes Widget umstellen: entweder entfernen und mit der neuen Einstellung neu hinzufügen, oder „Auf alle platzierten Widgets anwenden“ tippen. Das ändert alle auf einmal."),
        G.P("Der Glas-Effekt ist eine Tönung, keine echte Unschärfe; das kann Android bei Widgets nicht. Auf sehr hellen oder unruhigen Hintergrundbildern ist die dunkle Variante besser lesbar.")
    )),
    GuideSection("Kalender und Wetter", listOf(
        G.P("Kalender auswählen: Im Tab Kalender stehen alle Kalender des Handys, nach Konto gruppiert. Ein Haken bedeutet: erscheint in den Widgets. Änderungen gelten sofort für alle Widgets. Beim ersten Start sind die Kalender „Dirk“ und „Arbeit“ vorausgewählt, falls es sie gibt, sonst alle."),
        G.P("Wetter: Im Tab Widgets unter „Wetter“:"),
        G.Bullets(listOf(
            "„Wetter anzeigen“ schaltet Temperaturen und Symbole ein oder aus.",
            "„Ort suchen“: Namen eintippen und den richtigen Treffer wählen. Voreingestellt ist Friedrichsdorf."
        )),
        G.P("Das Wetter kommt von Open-Meteo und wird etwa alle 30 Minuten aktualisiert. Ist es älter als 12 Stunden, etwa weil das Handy offline war, blendet CalWidg es aus, statt veraltete Werte zu zeigen.")
    )),
    GuideSection("Bedienung im Widget", listOf(
        G.Bullets(listOf(
            "Tag oder Termin antippen: öffnet deine Kalender-App an dieser Stelle.",
            "‹ und › im Monat: blättert einen Monat zurück oder vor. Tipp auf den Monatsnamen springt zum aktuellen Monat. Um Mitternacht springt das Widget ohnehin zurück.",
            "+ unten rechts: legt in deiner Kalender-App einen neuen Termin an.",
            "„Kalenderzugriff fehlt“ im Widget: antippen öffnet CalWidg zum Einrichten."
        ))
    )),
    GuideSection("Wenn etwas nicht klappt", listOf(
        G.Pairs(listOf(
            "Widget zeigt „Kalenderzugriff fehlt“" to "Widget antippen, im Tab Kalender „Zugriff erlauben“.",
            "Termine fehlen" to "Im Tab Kalender prüfen, ob der Kalender angehakt ist. Sonst in den Android-Einstellungen unter Konten prüfen, ob der Kalender synchronisiert wird.",
            "Widget aktualisiert sich nicht" to "Im Tab Widgets „Widgets jetzt aktualisieren“ tippen. Bleibt es dabei: Einstellungen › Apps › CalWidg › Akku › „Nicht eingeschränkt“.",
            "Erledigter Termin steht noch da" to "Android verzögert die Aktualisierung im Energiesparmodus um einige Minuten; das ist normal.",
            "Kein Wetter" to "Prüfen, ob „Wetter anzeigen“ an ist und das Handy online war; nach dem nächsten Abruf erscheint es wieder.",
            "Glas-Widget schlecht lesbar" to "Widget mit Dunkel + Glas neu hinzufügen oder Glas-Effekt aus.",
            "Neues Icon wird nicht angezeigt" to "Startbildschirm oder Handy neu starten."
        ))
    )),
)

fun LazyListScope.guideItems() {
    items(guide) { sec ->
        Card(sec.title) { sec.blocks.forEach { Block(it) } }
    }
}

@Composable
private fun Block(b: G) {
    val body = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (b) {
        is G.P -> Text(b.text, style = body)
        is G.Steps -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            b.items.forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(t, style = body)
                }
            }
        }
        is G.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            b.items.forEach { t ->
                Row {
                    Text("•", style = body, modifier = Modifier.width(18.dp))
                    Text(t, style = body)
                }
            }
        }
        is G.Pairs -> Column {
            b.items.forEachIndexed { i, (k, v) ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(k, style = MaterialTheme.typography.titleSmall)
                Text(v, style = body, color = muted)
            }
        }
    }
}
