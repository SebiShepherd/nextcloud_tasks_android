# Unteraufgaben — Notizen für die Umsetzung

Quelle der UI-Entscheidungen: `Unteraufgaben Optionen.dc.html` (Turn 5, Option 5a) und `Unteraufgaben Prototyp.dc.html`.

## Modell & Datenschicht
- `Task.parentUid` existiert bereits; `TaskDraft` braucht `parentUid`.
- `TasksContent` baut heute `groupBy(listId)`; nötig ist pro Liste ein Baum (Top-Level + Kinder in Reihenfolge).
- **Zyklen brechen:** zwei Clients können `A -> B` und `B -> A` schreiben. Beim Baumbau Besuchsmenge mitführen, bei Zyklus die zweite Kante ignorieren (nicht auf dem Server reparieren).
- **Fremde Tiefe erhalten:** die Weboberfläche erlaubt Kind→Kind→Kind. Die App zeigt flach auf Ebene 1, darf `RELATED-TO` dabei aber nicht verändern. Nur explizite Nutzeraktionen schreiben `RELATED-TO`.
- **percentComplete** wird aus den Kindern nur berechnet und angezeigt, nie geschrieben — das Feld gehört dem Slider im Detail.
- **Schreib-Verstärkung vermeiden:** ein Häkchen an einer Unteraufgabe ändert nur dieses VTODO. Kein Mit-Sync des Parents, kein ETag-Roundtrip auf der ganzen Gruppe.
- **Löschen** kaskadiert in CalDAV nicht: Dialog entscheidet zwischen „alles löschen“ und „Kinder freistellen“ (`parentUid = null`).
- **Liste wechseln** = VTODO zwischen Collections verschieben (UID und Felder erhalten, ETag-sicher). Parent nimmt seine Kinder mit; ein einzeln verschobenes Kind verliert `RELATED-TO`.

## Favoriten = PRIORITY (kein neues Feld)
Belegt im Web-Client `nextcloud/tasks`:
- `src/store/tasks.js` → `toggleStarred`: liegt `priority` nicht zwischen 1 und 4, wird sie auf **1** gesetzt, sonst auf **0**.
- `src/store/storeHelper.js` → Sammlung `starred` nutzt `isTaskPriority`: `priority > 0 && priority < 5`.
- Sortierung im Web: `sortByPinned, sortByDue, sortByPriority, sortAlphabetically` — Priorität ist überall sekundäres Kriterium, kein eigenes „starred first“.

Für die App heißt das:
- Kein Schema-Migrationsbedarf, `Task.priority` existiert bereits.
- Stern in der Zeile und der `PriorityRow` im Detail schreiben **dasselbe Feld** — beide müssen aus einer Quelle lesen, sonst driften sie auseinander (Stern setzen und danach „Dringlichkeit: Mittel“ wählen darf den Stern nicht stehen lassen).
- Stern sitzt rechts in jeder Zeile, vertikal zentriert (wie Google Tasks / MS To Do), und in der Icon-Reihe des Erstell-Blatts.

## Sortierung
- `TaskSort` bekommt `MANUAL`, user-facing „Meine Reihenfolge“, Default für Neuinstallationen.
- Ziehen/Handles nur bei `MANUAL` — Vorbild `TaskAdapterProvider` in tasks/tasks (`filter.supportsManualSort() && queryPreferences.isManualSort`).
- Persistenz der Reihenfolge: kein CalDAV-Standardfeld. Vorschlag `X-APPLE-SORT-ORDER` schreiben (Reminders-kompatibel), lokal spiegeln, bei Konflikt letzter Schreiber gewinnt.
- Drop-Regel wie `AstridTaskAdapter.canMove`: ein Parent darf nie in seinen eigenen Nachfahren fallen.

## Zustände, die persistiert werden müssen
- Klapp-Zustand pro Aufgabe in die Room-DB (nicht `remember`), damit er Rotation und Sync übersteht.
- Auswahl-Modus ist flüchtig, muss aber `onSaveInstanceState` überleben (Rotation mitten in der Auswahl).

## Berechtigungen
- `ShareAccess.READ` sperrt: Handles, „Unteraufgabe hinzufügen“, Drop in diese Liste, Swipe-Aktionen.

## Interaktion (aus dem Prototyp)
- Long-Press = Auswahl **und** Handles frei; Zurück-Geste beendet den Modus (`BackHandler`).
- Swipe rechts = erledigen, links = löschen, beides mit einer Snackbar samt Rückgängig; Bulk-Aktionen erzeugen genau eine Snackbar.
- Zeilen enthalten keine Aktions-Icons (kein Delete, kein Dreipunkt).
- Kind-Zeilen: 16dp Einrückung, 2dp Guide-Linie, 10dp Padding.

## Entschieden (Turn 6)
1. **Abhaken kaskadiert.** Parent abhaken hakt alle Nachfahren mit ab; ein Kind wieder öffnen öffnet alle Vorfahren mit; read-only Aufgaben werden übersprungen. Vorbild: `TaskCompleter.setComplete(item, completed, includeChildren = true)` in tasks/tasks. Ein Undo über die ganze Kette.
2. **Zwei Ebenen.** Ebene 1 = 16dp Einrückung, Ebene 2 = 12dp, tiefer wird in der Anzeige gekappt (fremde Tiefe bleibt in den Daten unangetastet). Verschachteln über Ebene 2 hinaus wird abgelehnt (Snackbar).
3. **Sortierung global als Default**, plus Einstellung „Sortierung pro Liste merken“ (Vorbild `p_per_list_sort` in tasks/tasks: Auswahl zwischen `FilterPreferences` und globalen `Preferences`).
4. **Suchtreffer mit Parent-Kontext:** Ergebnisliste durchläuft denselben Baumbau mit gefilterter Menge; Parent-Zeile als nicht-tappbarer Kontext über dem Treffer.
