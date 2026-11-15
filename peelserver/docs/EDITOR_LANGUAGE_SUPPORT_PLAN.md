# Umsetzungsplan: PEEL-Sprachunterstützung und erwartete Eingaben

Stand: 12. November 2026. Status: Planung, noch keine Feature-Implementierung.

## Ziel und bestätigter Umfang

PEEL Web erhält Syntax-Highlighting, eine Unterscheidung von Funktionen und
Variablen sowie Autovervollständigung über Strg+Leertaste. Skripte beschreiben
ihre externen Eingaben durch gespeicherte `expectedBindings`. Diese Beschreibung
wird auch bei Skriptausführung, Dokumentvorschau und Print Jobs berücksichtigt.

Für Version 1 sind zwei Produktentscheidungen bestätigt:

- Alle Expected Bindings sind verpflichtend. Es gibt kein `required`-Feld und
  keine optionalen Eingaben.
- Print Jobs prüfen fehlende CSV-Spalten bereits vor dem Start der Verarbeitung.

Die nachstehenden Detailregeln bilden den vorgeschlagenen Implementierungsumfang.
Es gibt keine neue PEEL-Deklarationssyntax, Typinferenz, Objektfeld-Schemas,
Standardwerte, CSV-Spaltenzuordnung, Versionshistorie oder Datenmigration.

## Ausgangslage und betroffene Schnittstellen

- Die Sprachsyntax liegt im benachbarten Repository `../peel` in
  `src/main/antlr/Peel.g4`. Der Java-Lexer und -Parser werden mit ANTLR erzeugt.
- `ProgrammVisitor` führt bereits lexikalische Gültigkeitsbereiche. Er erzeugt
  ausführbare Ausdrücke, die derzeit keine allgemeinen Quelltextbereiche tragen.
  Unbekannte lesende Namen sind erlaubt und werden erst zur Laufzeit aufgelöst.
- Die Laufzeit unterstützt externe Werte über `RequestBindings` bereits.
- `FileContent.ScriptContent` enthält bisher nur `source`. Alle Skripte liegen
  in der gemeinsamen `project-files`-Collection.
- `editorDrafts.ts` speichert Textentwürfe; `projectFiles.ts` hält bestätigte
  Dateiversionen und Metadatenentwürfe. Mehrere Speicherpfade bauen ScriptContent
  momentan ausschließlich aus dem Quelltext neu auf.
- `EditorPanel.vue` erkennt Sprachen am Dateinamen und registriert noch kein PEEL.
- Der bestehende GET-Endpunkt `/api/scripts/validate` nimmt einen Request-Body
  an. Seine Java-Implementierung dupliziert Teile der Syntaxanalyse und bildet
  einige semantische Fehler lediglich auf Position 1:1 ab.
- `DocumentReferenceService` löst gespeicherte Dateien in `PeelScript` auf.
  Dieser Zwischentyp transportiert noch keine Eingabebeschreibung.
- `PrintJobRunner` verwendet CSV-Header direkt als Binding-Namen und reicht
  dieselben Zeilenwerte an alle Skripte des Dokuments weiter. Referenzen werden
  bereits beim Jobstart aufgelöst, nicht beim Anlegen.
- Im Arbeitsverzeichnis bestehen umfangreiche andere Änderungen, insbesondere
  für Internationalisierung. Die spätere Umsetzung muss sie erhalten und die
  dann aktuelle i18n-Struktur für neue UI-Texte verwenden.

## Architektur

| Bereich | Verantwortung |
| --- | --- |
| PEEL-Kernbibliothek | Grammatik, editorunabhängige Namensanalyse, Quelltextbereiche, sichtbare Symbole und Diagnosen; Informationen über bereitgestellte Laufzeitsymbole. |
| PEEL-Web-Backend | Speicherung der Eingabebeschreibung, Projektberechtigungen, HTTP-Adapter, Prüfung tatsächlicher Bindings und CSV-Spalten. |
| PEEL-Web-Frontend | Lokaler Monarch-Tokenizer, Monaco-Provider, Farben, Entwürfe und Eingabeformular. |

Das Einfärben von Keywords, Literalen, Kommentaren und Operatoren funktioniert
unmittelbar im Browser. Eine verzögerte Serverantwort verhindert dieses
Highlighting nicht. Semantische Farben und lokale Vorschläge verwenden die
gemeinsame PEEL-Namensanalyse; es entsteht kein zweiter Scope-Resolver im Frontend.

Die Kernbibliothek kennt weder Monaco noch Spring, MongoDB, CSV oder Dateiversionen.
Sie bekommt externe Symbolbeschreibungen als Argumente. Die Anwendung speichert
diese Beschreibungen und stellt die konkreten Werte für einen Lauf bereit.

Eine vollständige generische Lexer-API ist hierfür nicht erforderlich. Kommentare
können im bestehenden ANTLR-Lexer zunächst weiter per `skip` verworfen werden,
weil der lokale Tokenizer ihre Hervorhebung übernimmt.

## Datenvertrag und Verhalten in Version 1

Vorgesehener Skriptinhalt:

```json
{
  "type": "script",
  "source": "var brutto = betrag * 1.19;",
  "expectedBindings": [
    {
      "name": "betrag",
      "description": "Nettobetrag der Rechnung"
    }
  ]
}
```

- `expectedBindings` ist eine Liste; neue Skripte beginnen mit `[]`.
- Ein Eintrag enthält einen Namen und eine Beschreibung, die leer sein darf.
  Werte werden hier nicht gespeichert.
- Namen müssen gültige PEEL-Identifier sein, dürfen keine reservierten Wörter
  sein und innerhalb eines Skripts nicht doppelt vorkommen. Die Prüfung nutzt
  die Sprachdefinition statt einer unabhängig gepflegten Backend-Keywordliste.
- Groß-/Kleinschreibung entspricht PEEL. Verschiedene Skripte dürfen denselben
  externen Namen erwarten und erhalten dann denselben Binding-Wert.
- Verpflichtend bedeutet zunächst Anwesenheit des Schlüssels. Ein vorhandener
  JSON-Wert `null` zählt als vorhanden und bleibt nach bestehendem Mapping `None`.
  In CSV zählt eine vorhandene Spalte auch bei leerer Zelle als vorhanden.
  Typen, Wertebereiche und Leerwertregeln sind nicht Bestandteil dieser Prüfung.
- Zusätzliche tatsächliche Bindings beziehungsweise CSV-Spalten sind erlaubt.
- Lokale Deklarationen und Verschattung behalten die bestehende Sprachsemantik.
  Ein explizit erwartetes Binding bleibt Teil des Vertrags, auch wenn es im
  aktuellen Quelltext ungenutzt ist.
- Unbekannte lesende Namen erzeugen im Editor zunächst eine Warnung mit dem
  Hinweis auf eine mögliche fehlende Eingabebeschreibung. Sie verhindern weder
  das Speichern noch ändern sie das bisherige Laufzeitverhalten.
- Syntaxfehler und bereits bestehende semantische Fehler werden als Fehler mit
  passenden Quelltextbereichen gemeldet. Nicht ausführbare Entwürfe bleiben
  speicherbar, sofern die Struktur ihrer Eingabebeschreibung gültig ist.

## Arbeitspaket 1: Eingabevertrag durchgängig speichern und verwenden

Backend:

- `FileContent.ScriptContent` um die Eingabebeschreibung erweitern. Struktur und
  Namen beim Anlegen und Aktualisieren prüfen, einschließlich verschachtelter
  Bean Validation beziehungsweise gezielter fachlicher Validierung.
- `PeelScript` und `DocumentReferenceService` erweitern, damit Referenzauflösung
  Quelltext und Expected Bindings aus demselben gelesenen Dateistand transportiert.
- `RunRequest` um `expectedBindings` ergänzen. Ein Editor-Run sendet den aktuellen
  Quelltextentwurf und dessen aktuelle Eingabebeschreibung zusammen. Es wird
  dafür nicht heimlich die neueste gespeicherte Skriptversion geladen.
- Die Prüfung fehlender Binding-Namen gemeinsam in den Ausführungspfad einbauen.
  Direkter Run und Dokumentvorschau verwenden dieselbe Prüfung. Bei Dokumenten
  vor der ersten Skriptausführung alle benötigten Namen prüfen und betroffene
  Skripte zuordnen; keine teilweise begonnene Dokumentberechnung.
- Fehler im vorhandenen JSON-Fehlerformat mit einem spezifischen Code und
  strukturierten fehlenden Namen ausgeben. Strukturell gültige Requests mit
  fehlenden Eingaben ergeben HTTP 400.
- Die generierten OpenAPI-Typen in `src/types/api.ts` und
  `frontend/src/types/api.ts` synchron aktualisieren. Keine Kompatibilitäts-API
  und kein Fallback für alte gespeicherte Daten einführen.

Frontend:

- Eingabebeschreibungen als dateigebundene Skriptentwürfe im bestehenden
  `projectFiles.ts` ergänzen. Text bleibt in `editorDrafts.ts`; keine neue
  allgemeine Dokumentabstraktion einführen.
- Ein kleines Formular für Namen/Beschreibungen in `FileSettings.vue` über
  dessen vorhandenen Slot einbetten. Nur Skripte zeigen diese Eingaben an.
  Die gemeinsamen Name-/Beschreibungsfelder und das bestehende Zahnrad bleiben
  der Einstieg. Keine zusätzlichen Save-/Run-Buttons in Dockview-Headern.
- Alle Speicherpfade berücksichtigen: `FileToolbar.vue`, initiale Erstellung
  im Explorer, Speichern beim Projektwechsel in `ProjectView.vue` sowie
  Metadaten-only-Speicherung. Expected Bindings dürfen dabei nicht verloren gehen.
- Dirty-Anzeige, Tabtitel, Navigation, Logout, explizites Reload und Löschen
  berücksichtigen Änderungen der Eingabebeschreibung.
- Quelltext, Dateimetadaten und Expected Bindings werden atomar mit der zuletzt
  bestätigten `expectedVersion` gespeichert. Ein Konflikt erhält alle Entwürfe.
  Änderungen während eines Saves bleiben dirty; nur der bestätigte Snapshot
  darf als gespeichert markiert werden.
- Run verwendet aktuelle Entwürfe; Vorschau/Print Jobs verwenden wie bisher
  gespeicherte referenzierte Skripte. Die UI muss diesen Unterschied beibehalten.

Abnahme: Ein Skript mit externer Eingabe lässt sich speichern, erneut öffnen und
ohne lokale Deklaration ausführen. Eine fehlende Eingabe wird für Run und Vorschau
vor der Berechnung nachvollziehbar gemeldet. Alle Speicher-/Verwerfpfade erhalten
den erwarteten Vertragsstand.

## Arbeitspaket 2: Unmittelbares Syntax-Highlighting in PEEL Web

- `frontend/src/adapter/peelLanguage.ts` registriert PEEL einmal je Monaco-Instanz.
- Monarch-Regeln bilden die tatsächliche Grammatik ab: Keywords, `True`/`False`,
  Identifier, Integer/Dezimalzahlen, doppelt zitierte Strings mit ihren erlaubten
  Escapes, `//`-Kommentare, Operatoren, Klammern und Trennzeichen.
- Mehrzeichenoperatoren wie `**`, `==`, `!=`, `&&` und `||` korrekt priorisieren.
  Keine JavaScript-Syntax wie Blockkommentare oder einzeilige Quotes übernehmen.
- Unvollständige Strings und ungültige Escapes dürfen die Farben nachfolgender
  Zeilen nicht dauerhaft verändern. Kommentare in Strings bleiben Stringinhalt.
- Monaco-Sprachkonfiguration für Klammern, Quotes, Zeilenkommentare und
  Einrückung ergänzen. Funktionen/Variablen bekommen ihre verlässliche Zuordnung
  später aus Arbeitspaket 3; keine konkurrierende Regex-Symboltabelle bauen.
- `EditorPanel.vue` verwendet für Skripte immer `peel`, für Renderkonfigurationen
  weiterhin `json`. Der Dateiname entscheidet nicht über die Skriptsprache.
- Farben im vorhandenen `peel-dark`-Theme ergänzen; JSON-Editoren und das übrige
  Dockview-Erscheinungsbild erhalten ihre bisherigen Rollen.

Abnahme: PEEL-Code ist auch ohne erreichbares Analyse-Backend korrekt lexikalisch
hervorgehoben. Mehrere offene Tabs verursachen keine Mehrfachregistrierungen.

## Arbeitspaket 3: Editorunabhängige Namensanalyse in PEEL

- Kleine öffentliche API, sinngemäß `analyze(source, externalSymbols,
  runtimeSymbols)`, mit unveränderlichen Ergebnisdaten ergänzen. Die endgültigen
  Java-Namen beim Implementieren an die vorhandene Paketstruktur anpassen.
- Das Ergebnis enthält Symboldefinitionen, Vorkommen, Gültigkeitsbereiche und
  Diagnosen. Informationen über Deklarationsposition und Sichtbarkeitsbeginn
  erlauben das Bestimmen der an einer Cursorposition sichtbaren Symbole.
- Zwischen lokalen Variablen, Parametern, deklarierten Funktionen, erwarteten
  externen Variablen und bereitgestellten Laufzeitsymbolen unterscheiden.
  Deklaration, Referenz und Aufrufposition bleiben getrennte Informationen.
- Scope-Regeln an der bestehenden Ausführung charakterisieren: verschachtelte
  Blöcke, Parameter, foreach-Namen, Closures, Rekursion, Deklarationsreihenfolge
  und gleiche Namen für Funktionen/Variablen. Keine neue Hoisting- oder
  Namespace-Regel durch den Analyzer einführen.
- Bei `var f = bekannteFunktion;` bleibt `f` zunächst eine Variable mit einem
  Aufrufvorkommen bei `f(...)`. Es gibt keine Zusicherung statischer Aufrufbarkeit
  und keine Nachverfolgung dynamischer Funktionswerte über Zuweisungen.
- Bereitgestellte Funktionen und globale Variablen aus derselben
  Runtime-Konfiguration beschreiben, aus der sie registriert werden. Namen und
  vorhandene Aritäten nutzen; keine separate Liste von Built-ins in PEEL Web.
- Analyse führt weder Skripte noch Host-Funktionen aus. Den bisherigen
  ausführbaren Visitor nicht unverändert auf fehlerhafte Teilbäume anwenden.
  Vollständige Teilbäume können Ergebnisse liefern; unsichere Teilbereiche
  erzeugen keine erfundenen Auflösungen oder Warnungskaskaden.
- Leere und unvollständige Skripte sind reguläre Analyse-Eingaben. Syntaxfehler
  werden als Diagnosen geliefert und nicht als HTTP-/Java-Systemfehler behandelt.
- Alle öffentlichen Bereiche sind nullbasierte UTF-16-Offsets `[start, end)`.
  ANTLR-Codepoint-Indizes explizit umrechnen; LF, CRLF und Zeichen außerhalb der
  BMP testen. Keine ANTLR-Tokennummern oder Monaco-Typen öffentlich machen.

Abnahme: Derselbe Kernbibliotheksaufruf kann von einem anderen JVM-Konsumenten
verwendet werden. Ergebnisse stimmen mit den getesteten Gültigkeitsregeln der
Laufzeit überein; unvollständiger Code lässt den Analyzer nicht abstürzen.

## Arbeitspaket 4: Analyse, semantische Farben und Strg+Leertaste

Backend:

- Den bisherigen GET-Endpunkt `/api/scripts/validate` durch einen POST-Endpunkt
  `/api/scripts/analyze` ersetzen. Der Request enthält Projekt-ID, Quelltext und
  aktuelle Expected Bindings; das Backend ergänzt die tatsächlichen Runtime-Symbole.
- Projektzugriff prüfen; Analyse ist lesend und benötigt keinen aktiven Run.
  Das Ergebnis adaptiert die Kernbibliotheksdaten, ohne sie zu Monaco-Daten zu machen.
- `ValidationService` durch den dünnen Analyseadapter ersetzen. Doppelte
  Lexer-/Parserdiagnostik im Web-Backend entfernen, betroffene Aufrufer/DTOs und
  generierte Typen gemeinsam umstellen. Keine parallele Validierungslogik pflegen.

Frontend:

- Analyse mit kurzer Entprellung, zunächst etwa 300 ms, pro Datei/Monaco-Modell
  auslösen. Quelltext und Eingabebeschreibung bilden zusammen den analysierten Stand.
- Antworten nur übernehmen, wenn Modell, Quelltextversion, Vertragsrevision und
  Workspace-Generation weiterhin passen. Anfragen nach Änderungen abbrechen
  beziehungsweise verspätete Antworten ignorieren.
- Analysezustand pro Dokument-ID halten; Modelle haben eindeutige, dateigebundene
  URIs. Provider werden einmal registriert und greifen auf den passenden Zustand
  zu, statt Daten über Dockview-Funktionsparameter zu transportieren.
- Diagnosen in Monaco-Marker, Symbolvorkommen in Semantic Tokens und Beschreibungen
  in Hover-Inhalte übersetzen. Externe Variablen als Herkunft kennzeichnen;
  lokale Variablen, Parameter und Funktionen passend darstellen.
- Completion Provider für Strg+Leertaste registrieren. Für den aktuellen Stand
  sichtbare lokale Namen, Expected Bindings, Runtime-Funktionen und Keywords
  liefern; Shadowing und Sichtbarkeitsbeginn anhand des Analyseergebnisses beachten.
- Nach Änderungen keine alten lokalen Vorschläge oder Semantic Tokens auf neue
  Positionen anwenden. Bis zur aktuellen Analyse bleiben aktuelle Expected
  Bindings, Runtime-Symbole und Keywords verfügbar. In Kommentaren/Strings keine
  allgemeinen Codevorschläge anbieten.
- Netzwerkausfälle lassen den lokalen Tokenizer und die vorhandenen externen
  Eingabebeschreibungen nutzbar. Sie dürfen nicht als Syntaxfehler erscheinen.
- Beim Reload, Löschen, Logout und Workspace-Wechsel Analysezustände und Marker
  verwerfen; beim Schließen von Modellen deren Listener/Registrierungen freigeben.

Abnahme: Strg+Leertaste schlägt eine erwartete Eingabe ohne Testwert vor. Lokale
Vorschläge stammen nur aus dem aktuellen Skript und gültigen Scope. Eine langsamere
alte Antwort kann weder Farben noch Vorschläge eines neuen Entwurfs überschreiben.

## Arbeitspaket 5: CSV-Prüfung vor dem Print-Job-Start

- Nach projektgebundener Referenzauflösung die Vereinigung aller Expected
  Bindings der tatsächlich verwendeten Skripte bilden. Fehlende Namen mit ihren
  Skriptbezügen für Fehlermeldungen erhalten; gleiche Namen nur einmal verlangen.
- Vor `CALCULATING` und vor dem Scheduling prüfen, ob eine CSV vorhanden ist
  und deren Header alle erwarteten Namen enthalten. Dasselbe CSV-Format wie im
  Worker verwenden; die CSV-Parsing-Konfiguration an einer Stelle halten.
- Headernamen exakt und mit gleicher Groß-/Kleinschreibung vergleichen. Keine
  automatische Spaltenumbenennung, Normalisierung oder Verschachtelung einführen.
  Doppelte/mehrdeutige Header gezielt als ungültige CSV melden.
- Fehlende Spalten als HTTP 400 mit spezifischem Fehlercode und Details
  zurückgeben. Der Job bleibt `CREATED`; es werden keine PDFs erzeugt und der
  Benutzer kann eine korrigierte Datei hochladen und erneut starten.
- Spaltenprüfung und Worker verwenden die beim Start aufgelösten Skripte samt
  Expected Bindings. Änderungen gespeicherter Skripte nach dem Start beeinflussen
  den laufenden Job nicht. Kein historischer Snapshot beim Anlegen einführen.
- Sicherstellen, dass der Worker dieselbe CSV verarbeitet, deren Header geprüft
  wurden: Während `CALCULATING` Eingabedatei nicht ersetzen lassen. Upload und
  Start über die bestehende Projektkoordination absichern, inklusive serverseitiger
  Prüfung und passender Deaktivierung des Uploads in der Detailansicht.
- `PrintJobDetailView.vue` zeigt die fehlenden Namen und zugehörigen Skripte
  verständlich an. Keine neue Mapping-Maske oder zusätzlicher Prüfendpunkt nötig.
- Bindings, die nur die Dokumentvorlage und kein Skript verwendet, werden durch
  diese erste Prüfung nicht erfasst. Sie bleiben ein gesonderter zukünftiger Bedarf.

Abnahme: Ein Job mit fehlender Spalte startet keinen Worker. Ein korrigierter
Upload kann danach erfolgreich starten. Eine Änderung der Anforderungen zwischen
Anlegen und Start wird berücksichtigt; nach Start bleibt der aufgelöste Stand stabil.

## Tests und Verifikation

| Ebene | Wesentliche Fälle |
| --- | --- |
| PEEL | Auflösung und Verschattung, Parameter/foreach/Closures/Rekursion, externe Namen und Runtime-Symbole, unbekannte Namen, Funktionsreferenzen, leere/fehlerhafte Teilprogramme, UTF-16 und CRLF. |
| Datei-API | Vertragsvalidierung, Roundtrip, identische Wiederholung, Versionskonflikt, Speichern ungültiger Quelltextentwürfe, Projektgrenzen und Archivschutz. |
| Ausführung/Vorschau | Fehlende Eingaben vor Berechnung, zusätzliche Werte erlaubt, `null` vorhanden, richtige Eingaben aus Run-Entwurf beziehungsweise gespeicherten Referenzen. |
| Print Jobs | Vereinigung mehrerer Skripte, fehlende Spalten ohne Statuswechsel/Workerstart, zusätzliche/leere Zellen, doppelte Header, korrigierter Upload, Änderungen vor/nach Start, kein Austausch während Verarbeitung. |
| Frontend | Alle Speicherpfade, Dirty-/Reload-/Logout-Verhalten, Änderungen während Saves, Tokenizer-Grenzfälle, Provider-Lebenszyklus, getrennte Tabs, veraltete Analyseantworten und Basissuggestions ohne Backend. |

Frontend: gezielte Vitest-Tests, `npm run type-check`, `npm run build` und
`git diff --check`. Den bekannten Vue-Modulauflösungsfehler gegebenenfalls separat
berichten und nicht durch sachfremde Änderungen verdecken. Bestehende Lint-Skripte
mutieren Dateien; bei fremden Arbeitsänderungen gezielt nur eigene Dateien prüfen.

Backend/Kernbibliothek: zunächst gezielte Gradle-Tests für die betroffenen Klassen,
danach die relevanten Integrations-/Vertragstests. Dateipersistenz und Print Jobs
benötigen die bestehende Docker-/Testcontainers-Umgebung. API-Typen vor ihrer
Verwendung im Frontend regenerieren.

Zusätzlich manuell in Monaco prüfen: Farben, Strg+Leertaste, Hover, Fehlerbereiche,
mehrere Tabs, sehr schnelle Änderungen, langsame/fehlgeschlagene Analyse und
unveränderte Ctrl+S-/Ctrl+Enter-Aktionen. Für diesen Plan wurden keine Feature-Tests
ausgeführt; die spätere Implementierung umfasst diese Verifikation.

## Reihenfolge und Aufwand

Die Arbeitspakete sind einzeln prüfbare Änderungen. Paket 1 schafft den
Eingabevertrag, Paket 2 das sofort sichtbare Highlighting. Paket 3 trägt die
gemeinsame Semantik; Paket 4 hängt von 1 bis 3 ab. Paket 5 hängt fachlich von 1 ab
und kann vor oder nach 3/4 umgesetzt werden.

| Paket | Grobe Größenordnung inklusive gezielter Tests |
| --- | --- |
| 1: Datenvertrag, Entwürfe, Run/Vorschau | 2–3 Entwicklertage |
| 2: Lokaler Tokenizer und Theme | 0,5–1 Entwicklertag |
| 3: Kernanalyse und Runtime-Symbole | 3–5 Entwicklertage |
| 4: Analyseadapter, Monaco-Integration und Completion | 2–3 Entwicklertage |
| 5: Print-Job-Prüfung und UI | 1–2 Entwicklertage |
| Gemeinsame Verifikation und Dokumentation | 1 Entwicklertag |

Gesamt: ungefähr 10–15 Entwicklertage. Die frühere Schätzung von 3–5 Tagen bezog
sich nur auf die Namensanalyse; Speicherung, alle Entwurfs-/Ausführungspfade,
Autovervollständigung und Print-Job-Prüfung sind zusätzlicher Umfang. Die größte
Unsicherheit liegt in der robusten Analyse unvollständiger Programme und dem
Abgleich der bestehenden Scope-/Funktionsregeln.

## Referenzen

- [Dateiverwaltung](FILE_MANAGEMENT.md)
- [Print Jobs](PRINT_JOBS.md)
- [Monaco: Monarch-Tokenizer und Kombination mit Semantic Tokens](https://microsoft.github.io/monaco-editor/typedoc/functions/editor_editor_api.languages.setMonarchTokensProvider.html)
- [Monaco: Completion Provider](https://microsoft.github.io/monaco-editor/typedoc/interfaces/editor_editor_api.languages.CompletionItemProvider.html)
