# Handoff: Unteraufgaben (Subtasks) für Nextcloud Tasks Android

Betrifft: [Issue #59](https://github.com/SebiShepherd/nextcloud_tasks_android/issues/59) (Unteraufgaben), [#107](https://github.com/SebiShepherd/nextcloud_tasks_android/issues/107) (Aufgabe in andere Liste verschieben), [#108](https://github.com/SebiShepherd/nextcloud_tasks_android/issues/108) (Quick Actions: Long-Press + Swipe).
Zielrepo: `SebiShepherd/nextcloud_tasks_android`, Branch `main`. Kotlin, Jetpack Compose, Material 3, Clean Architecture (app/data/domain), Hilt, Room, WorkManager, iCal4j.

---

## Overview

Die App zeigt Aufgaben heute flach, gruppiert nach Liste. `Task.parentUid` existiert im Domain-Modell, wird aber nirgends gelesen. Dieses Paket beschreibt eine vollständige UI/UX-Lösung für Unteraufgaben — Darstellung, Erzeugung, Umhängen, Löschen, Sortierung — und nimmt dabei die beiden verwandten Feature-Requests (#107, #108) mit, weil sie sich dieselben Gesten teilen.

Kern der Lösung:

1. **Liste:** Unteraufgaben stehen eingerückt unter ihrer übergeordneten Aufgabe, mit Guide-Linie und Klapp-Chip (`1/3`) am Parent.
2. **Gesten:** Long-Press schaltet **Auswahl-Modus und Drag-Handles gleichzeitig** frei (wie Tasks.org und MS To Do); Zurück-Geste beendet ihn. Swipe rechts = erledigen, links = löschen. Keine Aktions-Icons in den Zeilen.
3. **Erstellen:** Bottom Sheet statt `AlertDialog` (Google-Tasks-Muster). Titel ist das einzige Pflichtfeld; Beschreibung, Fälligkeit, Favorit und übergeordnete Aufgabe hängen an einer Icon-Reihe.
4. **Favoriten:** neu, aber ohne Schema-Änderung — der Stern ist `PRIORITY`, genau wie im Nextcloud-Web-Client.

---

## About the Design Files

Die HTML-Dateien in diesem Bundle sind **Design-Referenzen**, keine Produktionsvorlage. Sie zeigen Aussehen und Verhalten; implementiert wird das Ganze in Kotlin/Compose im bestehenden Codebase-Stil (`MainActivity.kt`, `detail/TaskDetailScreen.kt`, `ui/theme/*`). Bitte nichts aus dem HTML portieren — Werte ablesen, Verhalten nachbauen.

Öffnen: beide `.dc.html` im Browser (benötigen `support.js` und `android-frame.jsx` im selben Ordner, beide liegen bei).

- **`Unteraufgaben Optionen.dc.html`** — die Design-Exploration in Runden. Relevant für die Umsetzung sind **Turn 5** (`5a`, fünf Views des gewählten Interaktionsmodells), **Turn 7** (`7a`, vier Views des Erstell-Blatts) sowie die Begründungskarten `5r`, `5s`, `6r`, `7r`. Turns 1–4 dokumentieren verworfene Alternativen — nur als Kontext lesen, nicht implementieren.
- **`Unteraufgaben Prototyp.dc.html`** — der interaktive Prototyp des gewählten Stands (Liste `5a` + Erstell-Blatt `7a`). Hier lässt sich das Verhalten durchklicken; er ist die Referenz bei Widersprüchen.
- **`UMSETZUNG.md`** — technische Notizen und Belege aus den Referenz-Codebases.

## Fidelity

**High-fidelity.** Farben, Abstände, Radien und Typografie sind aus dem bestehenden Code abgeleitet (`ui/theme/Color.kt`, `ui/theme/Theme.kt`, `MainActivity.kt`, `detail/TaskDetailScreen.kt`) und in dp-Werten angegeben. Die HTML-Mocks sind bei 1 dp = 1 px gezeichnet, Light Theme, ohne Dynamic Color.

**Nicht gezeichnet** (bewusste Lücken, siehe „Offene Punkte"): Dark Mode, Dynamic Color ab Android 12, Tablet/expanded, leerer Zustand im Detail, Read-only-Listen.

---

## Screens / Views

### 1. Aufgabenliste (Ruhezustand)

**Zweck:** Übersicht, Abhaken, Einstieg ins Detail.
**Referenz:** `5a` View 1, Prototyp Startzustand.

Layout wie heute (`TasksContent`): `LazyColumn`, `contentPadding` 16 dp (24 dp auf expanded), Gruppenkopf pro Liste (8 dp Farbpunkt + `titleSmall`, `FontWeight.Medium`, `onSurfaceVariant`, 8 dp Abstand nach unten, 16 dp nach oben zwischen Gruppen), 12 dp Abstand zwischen Karten.

**Top-Level-Karte** (Änderung an `TaskCard`):

| Element | Wert |
| --- | --- |
| Container | `surface` #F8F8F8, 1 dp Border `outlineVariant` #E0E0E0, `shapes.medium` (12 dp), Padding 12 dp, Elevation 0 |
| Ausrichtung | `Alignment.Top` wenn Zusatzinhalt vorhanden, sonst `CenterVertically` |
| Checkbox | M3 `Checkbox`, 18 dp Box, unchecked 2 dp Border `onSurfaceVariant` #767676, checked `primary` #0082C9 mit weißem Haken |
| Titel | `titleMedium` (16 sp / 24 sp, w500), `onSurface` #222222; durchgestrichen bei `completed` oder `status == CANCELLED` |
| Beschreibung | `bodyMedium` (14 sp / 20 sp), `onSurfaceVariant`, `maxLines = 2`, Ellipsis, 4 dp Abstand oben |
| Meta-Zeile | 8 dp Abstand oben, 8 dp horizontaler Gap |
| **Klapp-Chip (neu)** | Höhe 24 dp, Radius 12 dp, `surfaceVariant` #E7E7E7, Padding 0/8/0/4 dp; Icon `ExpandLess`/`ExpandMore` 18 dp `onSurface`, Text `done/total` (`labelMedium` 12 sp, w500) |
| Fälligkeit | `labelSmall` (11 sp / 16 sp, w500), `onSurfaceVariant`, Format wie heute: `stringResource(R.string.task_due_label, …)` mit `getBestDateTimePattern(locale, "MMMMd")` |
| **Favoriten-Stern (neu)** | 36 dp Trefferfläche rechts, **vertikal zentriert** (`Modifier.align(Alignment.CenterVertically)`), Icon 22 dp; gesetzt: gefüllt #ECA700, nicht gesetzt: Outline `onSurfaceVariant` |
| **Entfernt** | Das `Delete`-IconButton (48 dp) fällt weg. Löschen läuft über Swipe und Auswahl-Modus. |

**Unteraufgaben-Zeile (neu):** Wrapper mit `padding-start` 16 dp (Ebene 1) bzw. 12 dp (Ebene 2), linker Guide-Strich 2 dp `outlineVariant`, Wrapper-Margin-Start 9 dp (Ebene 1) bzw. 25 dp (Ebene 2), 8 dp Abstand zwischen Kind-Zeilen.
Karte identisch zur Top-Level-Karte, aber Padding **10 dp**, Titel `bodyLarge`-Größe (16 sp / 22 sp, w400), kein Beschreibungsfeld, Stern 32 dp/20 dp. Erledigte Kinder: Titel `onSurfaceVariant` + `LineThrough`.

**Erledigte Aufgaben:** unverändert `TextButton` mit `R.string.show_completed_tasks` / `hide_completed_tasks`.

**FAB:** unverändert 56 dp, `primary`, Radius 16 dp, `Icons.Default.Add`; öffnet jetzt das Erstell-Blatt.

### 2. Auswahl-Modus (Long-Press)

**Zweck:** Aktionen auf eine oder mehrere Aufgaben; gleichzeitig Sortieren und Verschachteln.
**Referenz:** `5a` Views 2–4.

Long-Press auf eine Zeile (ca. 430 ms) wählt sie aus **und** blendet die Drag-Handles ein. Weitere Zeilen antippen ergänzt die Auswahl; Antippen einer ausgewählten Zeile nimmt sie heraus; leere Auswahl beendet den Modus. Zurück-Geste (`BackHandler`) beendet ebenfalls.

**Kontextuelle Top-Bar** ersetzt die `UnifiedSearchBar` an derselben Stelle (Box 64 dp, innen 48 dp hoch, Radius 24 dp, 16 dp horizontaler Rand, 8 dp vertikal):
`primaryContainer` #D3E4F4, Inhalt `onPrimaryContainer` #006AA3. Von links: `ArrowBack` (beendet), Anzahl (`titleMedium`, 4 dp Abstand), dann Aktionen als 48 dp Icon-Buttons — `CheckCircle` (erledigen), `SubdirectoryArrowRight` (Unteraufgabe hinzufügen, nur bei genau einer Auswahl, sonst 40 % Deckkraft), `DriveFileMove` (Liste wechseln), `MoreVert` (Overflow).

**Overflow-Menü** (`DropdownMenu`, direkt unter der Bar, Breite 238 dp, Radius 4 dp, 8 dp vertikales Padding, Einträge 48 dp mit 20 dp Icon + 12 dp Gap):
„Aus Unteraufgabe lösen" (nur wenn mindestens eine Auswahl ein Kind ist) · „Zuordnen zu …" (nur bei einer Auswahl, sonst ausgegraut) · „Reihenfolge ändern" (nur wenn Sortierung ≠ „Meine Reihenfolge"; stellt darauf um) · „Alle auswählen" · Divider · „Löschen" in `error` #E9322D.

**Ausgewählte Zeile:** Container `primaryContainer` #D3E4F4, Border `primary` #0082C9, Titel `onPrimaryContainer` #006AA3. Die Checkbox zeigt weiterhin **den Erledigt-Zustand**, nicht die Auswahl.

**Drag & Drop:** Handle `DragIndicator` rechts (22 dp Top-Level / 20 dp Kind, `outline` #CACACA, aktiv `primary`), nur bei Sortierung „Meine Reihenfolge". Gezogene Karte: `surface` #FFFFFF, 1 dp `primary`-Border, Shadow `0 10dp 20dp rgba(0,0,0,.26)`, leichte Rotation (−0,7°), horizontal um max. 40 dp verschoben. Drop-Ziel als 40 dp hohe gestrichelte Zone (2 dp `primary`, Radius 12 dp, Füllung `primary` @ 6 %) mit Text (`labelMedium`, `primary`): „Ebene 1 · Unteraufgabe von ‚…'" bzw. „Ebene 0 · eigenständige Aufgabe". Horizontale Verschiebung > 28 dp = eine Ebene tiefer. Restliche Zeilen 45 % Deckkraft.

### 3. Aufgaben-Detail

**Zweck:** Felder bearbeiten, Unteraufgaben verwalten.
**Referenz:** `1f` View 2 (Platzierung), Prototyp-Detail.

Struktur wie heute (`TaskDetailContent`): `TopAppBar` mit `ArrowBack`, Titel als `BasicTextField` (`headlineSmall` 24 sp / 32 sp, 16 dp / 8 dp Padding), `TabRow` Details/Notizen, Detailzeilen 56 dp mit 20 dp Icon + 16 dp Gap und `HorizontalDivider` `outlineVariant`.

**Neu, im Details-Tab zwischen Fälligkeitsdatum und Standort:**

1. Abschnittskopf: `AccountTree` 20 dp `primary` + „Unteraufgaben" (`labelLarge`, `primary`) + Zähler rechts (`labelMedium`, `onSurfaceVariant`), Padding 12/16/4 dp.
2. Fortschrittsbalken: Höhe 4 dp, Radius 2 dp, Spur `surfaceVariant` #E7E7E7, Füllung `primary`, 16 dp horizontal, 8 dp unten. Prozent = erledigte Kinder / Kinder.
3. Kind-Zeilen: min. 48 dp, Checkbox 18 dp, Titel `bodyLarge` mit 12 dp Abstand, `MoreVert` 20 dp rechts.
4. Inline-Eingabe „Unteraufgabe hinzufügen": `Add` 20 dp `primary` + Hinweis `onSurfaceVariant`; im Bearbeitungszustand `BasicTextField` mit 2 dp `primary`-Unterstrich und „FERTIG" (`labelLarge`, `primary`). Enter legt an und hält den Fokus für die nächste.

**Bei einer Unteraufgabe** zeigt die TopAppBar rechts einen Rücksprung zum Parent: `ArrowUpward` 18 dp `primary` + Parent-Titel (`labelMedium`, `primary`, einzeilig mit Ellipsis, max. 160 dp).

**Fälligkeitszeile** öffnet den M3-`DatePicker` — derselbe wie im Erstell-Blatt.

### 4. Erstell-Blatt („Neue Aufgabe")

**Zweck:** Aufgabe oder Unteraufgabe in einem Zug anlegen. Ersetzt `CreateTaskDialog`.
**Referenz:** `7a` Views 1–4, Prototyp über den FAB.

`ModalBottomSheet`, Radius oben 28 dp, `surface` #FFFFFF, Shadow nach oben, über der Tastatur (`imePadding`).

Von oben:

1. **Parent-Zeile** (nur wenn vorbelegt oder gewählt): `SubdirectoryArrowRight` 18 dp `primary` + „Unteraufgabe von ‚…'" (`labelMedium`, `primary`, einzeilig, Ellipsis) + `Close` 18 dp zum Entfernen. Padding 10/20/6 dp.
2. **Titel:** `BasicTextField`, `bodyLarge` (16 sp / 24 sp), Platzhalter „Neue Aufgabe" bzw. „Neue Unteraufgabe", Autofokus, 12 dp / 20 dp Padding. **Einziges Pflichtfeld.**
3. **Beschreibung:** erscheint **nur** nach Tippen aufs Notizen-Icon; `bodyMedium`, Platzhalter „Beschreibung".
4. **Fälligkeits-Zeile** (nur wenn gesetzt): `Event` 20 dp `primary` + Datumstext + `Close`.
5. **Listen-Zeile** — nur wenn **keine** übergeordnete Aufgabe gesetzt ist: `ListAlt` 20 dp `onSurfaceVariant` + Listenname (`bodyMedium`, `onSurface`) + `ArrowDropDown`. Mit Parent entfällt die Zeile ganz; die Liste ergibt sich aus dem Parent.
6. **Divider** `outlineVariant`, 20 dp horizontal.
7. **Icon-Reihe** (48 dp Trefferflächen, 22 dp Icons, Padding 4/8/8 dp): `Notes` (Beschreibung ein/aus, aktiv `primary`) · `Schedule` (öffnet den DatePicker, aktiv `primary`) · `Star` (Favorit, gesetzt gefüllt #ECA700) · `SubdirectoryArrowRight` (öffnet die Parent-Auswahl, aktiv `primary`) · rechts „SPEICHERN" (`labelLarge`, `primary`, bei leerem Titel `outline` #CACACA und inaktiv).

**Parent-Auswahl** als eigenes Bottom Sheet über dem Erstell-Blatt: Drag-Handle 32×4 dp, Titel „Übergeordnete Aufgabe" (`titleMedium`), Suchfeld (44 dp, Radius 22 dp, `surfaceVariant` @ 60 %, `Search` 20 dp), Eintrag „Keine", dann Aufgaben **gruppiert nach Liste** (Gruppenlabel `labelSmall`, `onSurfaceVariant`, uppercase) mit 8 dp Farbpunkt; Ebene-1-Aufgaben eingerückt (40 dp) mit 6 dp grauem Punkt. Ebene 2 wird nicht angeboten. Zeilen 52 dp, aktuelle Wahl mit `Check` `primary`.

**DatePicker:** M3 `DatePickerDialog`, Breite 328 dp, Radius 28 dp; Label „Fälligkeitsdatum wählen" (`labelMedium`, `onSurfaceVariant`), Headline 32 sp / 40 sp, Divider, Monatsnavigation (40 dp Chevrons, `labelLarge` Mitte), Wochentagsreihe 24 dp (`labelMedium`, `onSurfaceVariant`), Tageszellen 44 dp hoch mit 40 dp Kreis (`bodyMedium`), Auswahl `primary` mit `onPrimary`-Text, Buttons ABBRECHEN/OK (OK inaktiv ohne Auswahl).

### 5. Liste wechseln

**Zweck:** #107.
**Referenz:** `5a` View 5.

Bottom Sheet, nur Titel „Liste wechseln" und die Listen (52–56 dp, 10 dp Farbpunkt, `Check` bei der aktuellen), plus „Neue Liste …". **Keine Warnung, kein Erklärtext.** Ein Parent nimmt seine Kinder mit; eine einzeln verschobene Unteraufgabe verliert ihre Verknüpfung. Danach die normale Snackbar „Nach ‚<Liste>' verschoben" mit RÜCKGÄNGIG.

### 6. Sortier-Dialog

`AlertDialog` wie heute (`SortDialog`), erweitert um **„Meine Reihenfolge"** als ersten Eintrag mit Unterzeile „Ziehen & Verschachteln möglich" (`labelSmall`, `primary`). Radiobuttons 20 dp, Zeilen 48 dp. Keine Fußnote.

---

## Interactions & Behavior

**Zeilen-Gesten (Normalmodus)**

| Geste | Wirkung |
| --- | --- |
| Tap | Detail öffnen |
| Tap auf Checkbox / Chip / Stern | nur diese Aktion — der Zeilen-Tap darf **nicht** mitfeuern (im Prototyp über `stopPropagation` beim Pointer-Down gelöst; in Compose: eigene Clickables ohne Weitergabe) |
| Long-Press ≈ 430 ms | Auswahl-Modus + Handles |
| Swipe rechts > 64 dp | erledigen; Hintergrund `NextcloudSuccess` #46BA61, Icon `Check` + „Erledigt" in `onPrimary` |
| Swipe links > 64 dp | löschen; Hintergrund `error` #E9322D, Icon `Delete` + „Löschen"; bei Kindern erst der Löschdialog |
| Swipe < 64 dp | federt zurück |

Swipe-Aktionsfeld ist 64 dp breit und liegt an der Screen-Kante, nicht am eingerückten Kartenrand.

**Abhaken kaskadiert** (belegt in `TaskCompleter` von Tasks.org, identisch zu Todoist, Reminders, Google Tasks): Parent abhaken hakt alle Nachfahren mit ab; ein Kind wieder öffnen öffnet **alle Vorfahren** mit; `readOnly`-Aufgaben werden übersprungen. Eine Snackbar, ein Rückgängig für die ganze Kette. Bestehende Animation (`SimpleAnimatedTaskCard`: 200 ms Checkbox, 200 ms `shrinkVertically` + 150 ms `fadeOut`, 250 ms bis zur Datenänderung) bleibt und muss die Kinder mitnehmen.

**Verschachteln**

- Maximal **zwei sichtbare Ebenen**. Ebene 1 = 16 dp Einrückung, Ebene 2 = 12 dp. Tiefer wird flach dargestellt.
- Ein Verschachteln über Ebene 2 hinaus wird abgelehnt (Snackbar „Tiefer als zwei Ebenen geht nicht").
- Ein Parent darf nie in seinen eigenen Nachfahren fallen (`canMove`-Regel aus `AstridTaskAdapter`: `!isDescendantOf(target, source)`).
- Beim Verschachteln wandern die eigenen Kinder mit.
- **Fremde Tiefe aus dem Web bleibt in den Daten unangetastet** — nur die Anzeige kappt.

**Löschen mit Kindern:** `AlertDialog` „‚<Titel>' löschen?" / „Diese Aufgabe hat n Unteraufgabe(n). Sollen sie mitgelöscht oder freigestellt werden?" mit drei Aktionen: UNTERAUFGABEN BEHALTEN (`primary`), ALLES LÖSCHEN (`error`), ABBRECHEN (`onSurfaceVariant`). Bei Mehrfachauswahl ohne Rückfrage löschen und Kinder freistellen, mit **einer** Snackbar.

**Sortierung × Drag:** Beides gleichzeitig ist logisch unmöglich. `TaskSort.MANUAL` („Meine Reihenfolge") ist Default für Neuinstallationen und die einzige Sortierung mit Handles — Vorbild `TaskAdapterProvider` in Tasks.org (`filter.supportsManualSort() && queryPreferences.isManualSort`). Bei Feld-Sortierung bleibt der Auswahl-Modus nutzbar, nur ohne Handles; im Overflow liegt „Reihenfolge ändern", das umstellt. Sortiert wird immer **innerhalb einer Ebene**; Kinder bleiben bei ihrem Parent.

**Snackbars** (`surfaceVariant` / `onSurfaceVariant`, Aktion `primary`, ca. 4 s): „Erledigt · n Unteraufgabe(n) mit" · „Wieder offen · übergeordnete Aufgabe mit" · „Aufgabe gelöscht" · „Aufgabe und Unteraufgaben gelöscht" · „Aufgabe gelöscht, Unteraufgaben behalten" · „Zu ‚…' verschoben" · „Aus Unteraufgabe gelöst" · „Nach ‚<Liste>' verschoben" · „Reihenfolge geändert". Immer mit RÜCKGÄNGIG, außer bei rein informativen.

**Offline / Read-only:** bestehendes Verhalten bleibt (`offline_message`, `list_read_only_hint`). `ShareAccess.READ` sperrt zusätzlich Handles, „Unteraufgabe hinzufügen", Swipes und Drops in diese Liste.

---

## State Management

Neu in der Liste (`AuthenticatedHome` / ViewModel):

- `selectionMode: Boolean`, `selectedIds: Set<String>` — flüchtig, aber rotationsfest (`SavedStateHandle`).
- `expandedIds: Set<String>` — **persistent in Room**, nicht `remember`; muss Rotation und Sync überleben (macht Tasks.org genauso).
- `dragState: { id, dy, dx }` plus abgeleitetes Drop-Ziel `{ index, parentUid }`.
- `swipeOffset: { id, dx }`.
- `sort: TaskSort` inkl. `MANUAL`; optional `perListSort: Boolean` (siehe unten).

Neu im Erstell-Blatt: `title`, `description`, `showDescription`, `dueDate`, `starred`, `parentUid`, `listId`, sowie `parentPickerOpen` / `listPickerOpen` / `datePickerOpen`.

Datenschicht:

- `TaskDraft` braucht `parentUid`, `due`, `priority`.
- `TasksContent` baut pro Liste einen **Baum** statt `groupBy(listId)`.
- Beim Baumbau **Zyklen erkennen und brechen** (zwei Clients können `A→B` und `B→A` schreiben).
- `percentComplete` aus den Kindern nur **anzeigen**, nie schreiben — das Feld gehört dem Slider im Detail.
- Ein Häkchen an einer Unteraufgabe schreibt nur dieses VTODO (keine Schreib-Verstärkung auf die Gruppe).
- Manuelle Reihenfolge: kein CalDAV-Standardfeld. Vorschlag `X-APPLE-SORT-ORDER` (Reminders-kompatibel), lokal spiegeln, bei Konflikt letzter Schreiber gewinnt.
- Liste wechseln = VTODO zwischen Collections verschieben, UID und Felder erhalten, ETag-sicher.

**Favoriten = `PRIORITY`.** Belegt im Web-Client `nextcloud/tasks`: `toggleStarred` in `src/store/tasks.js` setzt `priority = 1`, wenn sie nicht zwischen 1 und 4 liegt, sonst 0; die Sammlung `starred` filtert in `src/store/storeHelper.js` über `priority > 0 && priority < 5`. Also **keine Migration** — `Task.priority` existiert. Wichtig: Stern in der Zeile und `PriorityRow` im Detail schreiben dasselbe Feld und müssen aus einer Quelle lesen.

---

## Design Tokens

Alles aus `app/src/main/java/com/nextcloud/tasks/ui/theme/Color.kt` und `Theme.kt` (Light Scheme):

| Token | Hex |
| --- | --- |
| `primary` | #0082C9 |
| `primaryContainer` | #D3E4F4 |
| `onPrimaryContainer` / `secondary` / `tertiary` | #006AA3 |
| `background` | #FFFFFF |
| `surface` | #F8F8F8 |
| `onSurface` | #222222 |
| `onSurfaceVariant` | #767676 |
| `surfaceVariant` | #E7E7E7 |
| `outline` | #CACACA |
| `outlineVariant` | #E0E0E0 |
| `error` | #E9322D |
| Erfolg (Swipe) | #46BA61 (`NextcloudSuccess`) |
| Favorit | #ECA700 (`NextcloudWarning`) |
| deaktivierter Text | `onSurface` @ 38 % |

Dark Scheme steht in `Theme.kt` (`surface` #1E1E1E, `surfaceVariant` #2C2C2C, `onSurface` #D8D8D8, `outline` #3A3A3A) — dort liegen Karten- und Rinnen-Fläche dicht beieinander, das braucht eine eigene Runde.

**Abstände:** 4 / 8 / 12 / 16 / 20 / 24 dp. Karten-Padding 12 dp (Top-Level) bzw. 10 dp (Kind), Kartenabstand 12 dp (Top-Level) bzw. 8 dp (Kind), Einrückung 16 dp (Ebene 1) / 12 dp (Ebene 2), Guide-Linie 2 dp.
**Radien:** 12 dp Karten (`shapes.medium`), 16 dp FAB, 24 dp Suchleiste/Bar, 28 dp Bottom Sheets und Dialoge, 4 dp Snackbar und Menüs, 2 dp Checkbox.
**Typografie:** `FontFamily.SansSerif` (Roboto). `headlineSmall` 24/32 · `titleMedium` 16/24 w500 · `titleSmall` 14/20 w500 · `bodyLarge` 16/24 · `bodyMedium` 14/20 · `labelLarge` 14 w500 · `labelMedium` 12 w500 · `labelSmall` 11/16 w500.
**Trefferflächen:** 48 dp für alle Bar- und Reihen-Icons; 36 dp (Top-Level) / 32 dp (Kind) für Stern und Handle in der Zeile — bewusst unter 48 dp, weil die Zeile selbst der Haupttreffer ist; wenn das nicht durch die Accessibility-Prüfung geht, Zeilenhöhe erhöhen statt Icons verkleinern.
**Elevation:** Karten 0 dp mit 1 dp Border (bestehende Konvention), FAB Standard, gezogene Karte `0 10dp 20dp rgba(0,0,0,.26)`, Bottom Sheet `0 -6dp 24dp rgba(0,0,0,.24)`.

---

## Assets

Keine neuen Bild-Assets. Alle Icons sind Material-Symbole, im Mock über „Material Symbols Outlined" gezeichnet, in der App über `androidx.compose.material.icons`:
`Menu`, `Sort`, `Add`, `Delete`, `ArrowBack`, `ArrowUpward`, `Check`, `CheckCircle`, `ExpandMore`, `ExpandLess`, `MoreVert`, `DragIndicator`, `SubdirectoryArrowRight`, `LowPriority`, `FormatIndentIncrease`, `DriveFileMove`, `SelectAll`, `SwapVert`, `Star`, `StarOutline`, `Notes`, `Schedule`, `Event`, `ListAlt`, `AccountTree`, `CalendarToday`, `CalendarMonth`, `LocationOn`, `Language`, `Label`, `Search`, `Close`, `ChevronLeft`, `ChevronRight`.

**Strings:** alle neuen Texte brauchen Keys in `res/values/strings.xml` und `res/values-de/strings.xml` (die anderen 11 Sprachen laufen über die üblichen Beiträge nach). Die deutschen Formulierungen in diesem Dokument sind final gemeint, die englischen bitte im Stil der bestehenden Keys bilden.

---

## Offene Punkte

Produktentscheidungen, die noch offen sind:

1. **„Meine Reihenfolge" global oder pro Liste?** Empfehlung: global als Default plus Einstellung „Sortierung pro Liste merken" — genau das Muster von Tasks.org (`p_per_list_sort`, „Remember sort and group settings for each list independently", Default aus).
2. **Suchtreffer auf eine Unteraufgabe:** Parent-Zeile als nicht-tappbarer Kontext über dem Treffer (Empfehlung, weil derselbe Baumbau mit gefilterter Menge reicht) — noch nicht gezeichnet.
3. **Favoriten als Sortierkriterium?** Der Web-Client sortiert Priorität nur sekundär (`sortByPinned, sortByDue, sortByPriority, sortAlphabetically`), kein „starred first". Offen, ob die App das übernimmt.
4. **Dark Mode, Dynamic Color, Tablet/expanded, leerer Zustand, Read-only-Zustände** sind nicht gezeichnet — Werte stehen oben, die Views fehlen.

---

## Files

| Datei | Inhalt |
| --- | --- |
| `Unteraufgaben Prototyp.dc.html` | interaktiver Prototyp des gewählten Stands — Referenz bei Widersprüchen |
| `Unteraufgaben Optionen.dc.html` | Design-Exploration; umsetzungsrelevant sind `5a`, `7a` und die Karten `5r`, `5s`, `6r`, `7r` |
| `UMSETZUNG.md` | technische Notizen, Belege aus `tasks/tasks` und `nextcloud/tasks` |
| `support.js`, `android-frame.jsx` | Laufzeit für die HTML-Referenzen (nur zum Öffnen im Browser) |

Referenz-Codebases, aus denen Verhalten belegt ist: `tasks/tasks` (`TaskCompleter.kt`, `TaskAdapterProvider.kt`, `AstridTaskAdapter.kt`, `TaskViewHolder.kt`, `compose/chips/SubtaskChip.kt`, `Preferences.kt`) und `nextcloud/tasks` (`src/store/tasks.js`, `src/store/storeHelper.js`).
