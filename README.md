# CalWidg

Kalender-Widgets für Android (Jetpack Glance), gebaut für das Galaxy Z Fold.

- **Datum**: Tagesdatum im App-Icon-Stil
- **Monat**: Monatsraster mit Auslastung pro Tag, groß mit Datumskopf und Wetter
- **Heute & Morgen**: Agenda mit Wetter, abgelaufene Termine werden ausgeblendet
- **Kombi**: Monat und Agenda nebeneinander

Termine kommen aus dem Systemkalender (also auch Google Calendar), Wetter von Open-Meteo.
Aussehen (Hell/Dunkel/System, Glas-Effekt) wird pro Widget beim Hinzufügen festgelegt.

## Build

```
./gradlew assembleRelease
```

Für eine signierte APK `keystore.properties` im Projektordner anlegen (liegt nicht im Repo):

```
storeFile=release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Updates lassen sich nur mit demselben Schlüssel über eine installierte Version spielen.

## Lizenzen

Schrift Inter: SIL Open Font License 1.1 (`app/src/main/assets/fonts/LICENSE-Inter.txt`).
