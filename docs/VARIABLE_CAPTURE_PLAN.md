# Implementierungsplan: Variablenzustand im Execution Trace

Stand: 15. November 2026. Status: in Kern und Web implementiert; Live-Browserabnahme offen.
Gilt für `../peel` (Kernbibliothek) und dieses Repository (PEEL Web).

## Umsetzungsstand

- Kern: `variablesAtExit` an jedem ausgeführten Block und der Programmwurzel,
  rekursiv eingefrorene Trace-Werte und Scope-Abbau auch bei `return` und Fehlern.
  `VariableCaptureTest` dokumentiert K01–K20. Bestehende Ergebnistests bleiben erhalten.
- Lexikalische Sichtbarkeit ohne zusätzliche Parser-Metadaten: Beim Kopieren einer
  Funktions-/Closure-Umgebung wird die Menge sichtbarer Namen je Scope fixiert.
  Die bestehenden Scope-Referenzen liefern weiterhin deren aktuelle Werte.
  Spätere Deklarationen können die aufgezeichnete Namensbindung nicht erweitern
  (K20). Syntax und Variablenauflösung bleiben unverändert.
- Web: Auswahl liest ausschließlich den Snapshot des ausgewählten Vorkommens,
  einschließlich Programmwurzel. Eintrittsargumente werden nicht als Endwerte
  verwendet. Map-Pfade und leeres Feld als Blockergebnis bleiben erhalten.
- Editor: deutsche/englische Beschriftung und Hinweis erklären den Variablenwert
  beim Blockende; das gespeicherte Attribut heißt weiterhin `binding`.
- Automatisierte Prüfungen umfassen echte Kern-Traces, HTML/PDF, Auswahlfehler,
  Verschattung, Parameterisolation, Tiptap-Node-Views und bestehende Datei-/Druckpfade.
  Die API verwendet weiterhin generische Trace-Maps; keine DTO-/OpenAPI-Änderung.
- Die nachfolgende Planung bleibt als Semantik- und Testreferenz erhalten.
  Die manuelle Browserabnahme aus Schritt 7 ist noch nicht erneut durchgeführt.

## Ziel und bestätigte Semantik

Ein Trace-Wert-Baustein kann eine im ausgewählten Blockvorkommen sichtbare Variable
mit ihrem Wert beim Verlassen dieses Blocks ausgeben. Maßgeblich sind die echte
Variablenidentität und die lexikalischen Scopes der Laufzeit. Gleiche Namen in
unterschiedlichen Scopes sind unterschiedliche Variablen.

- Pro ausgeführtem Blockvorkommen wird ein eigener unveränderlicher Snapshot
  erzeugt, bei normalen Blöcken unmittelbar vor dem Verlassen des Scopes.
- Die letzte tatsächlich ausgeführte Zuweisung an die betreffende Variable zählt.
  Ohne neue Zuweisung gilt der dort weiterhin sichtbare Wert.
- Der Snapshot enthält lokale Variablen und sichtbare äußere Variablen. Die
  innerste sichtbare Deklaration gewinnt. Bereits verlassene Unterblock-Scopes
  und lokale Variablen des Aufrufers werden nicht hineinkopiert.
- Änderungen an echten äußeren Variablen wirken auf deren spätere Zustände;
  Änderungen an lokalen Schattenvariablen bleiben lokal.
- Jeder Schleifendurchlauf und jeder Funktionsaufruf besitzt einen eigenen
  eingefrorenen Zustand. Spätere Ausführung darf frühere Captures nicht verändern.
- Bei `return` gilt der Zustand beim tatsächlichen Verlassen. Nicht ausgeführte
  Anweisungen zählen nicht. Blockergebnis und Variablenzustand sind getrennt.
- Unbenannte Blöcke werden ebenfalls erfasst. Namen bleiben optional.
- Die gesamte Programmebene erhält ihren Endzustand. Dokumentauswahl und
  Rendering führen das Skript nicht erneut aus.
- Leeres Variablenfeld bedeutet weiterhin Blockergebnis; ein unbekannter Name
  ergibt einen verständlichen Fehler, keinen Rückgriff auf andere Scopes.

Diese Semantik präzisiert die bisherige Formulierung „letzte Zuweisung im Block“:
Eine Zuweisung an einen gleichnamigen Schatten in einem Unterblock ist keine
Zuweisung an die im Elternblock sichtbare Variable.

## Ausgangslage und konkrete Ansatzstellen

### PEEL-Kern

- `Scope.variablesView()` existiert bereits, liefert aber eine Sicht auf eine
  veränderliche Map. Diese Sicht darf nicht im Trace gespeichert werden.
- `EvaluationEnvironment` verwaltet eigene Scopes, eine lexikalische Parent-
  Umgebung und globale Variablen. Funktionsumgebungen werden in `PeelCodeFunction`
  aus der bei der Definition erfassten Umgebung abgeleitet.
- `Evaluator.evaluateBlock()` entfernt den lokalen Scope nach der Ausführung,
  bisher ohne Capture und ohne `finally` beim Verlassen per `return`.
- `return` wird über `ReturnValueFlow` weitergereicht und beim Funktionsaufruf
  abgefangen. Auch Schleifen-Scopes und die vorübergehend gewechselte Umgebung
  müssen zuverlässig zurückgesetzt werden.
- `TraceExpression.Assignment` enthält Name, Ausdruck und Wert, aber keine
  vollständige Scope-/Deklarationsidentität. Kein nachträglicher Namens-Replay.
- `TraceValueMapper` konvertiert Werte rekursiv in Trace-Werte und stellt
  Callables als Referenzen ohne Closure-Umgebung dar.
- `SimpleRuntime` entfernt den äußeren Trace-Block beim Aufbau von `TraceProgram`.
  Das neue Capture darf dabei nicht verloren gehen.
- `WhileLoopRecorder` erzeugt bisher für die letzte falsche Bedingung einen
  synthetischen leeren Body. Dieser ist kein ausgeführtes Blockvorkommen.

### PEEL Web

- `TraceSelection` sammelt derzeit foreach-Bindings und Funktionsargumente entlang
  des Trace-Baums. Daraus entsteht die heutige unvollständige Kontextwert-Tabelle.
- `StructuredDocumentRenderer.binding()` liest aus dieser Tabelle und unterstützt
  anschließende Map-Zugriffe wie `bauteil.name`.
- `peelValue` speichert den Feldinhalt unter `binding`; `scope`, `scriptId` und
  `block` wählen zuvor das konkrete Vorkommen aus.
- Vorschau und Druckaufträge benutzen bereits dieselbe Rendering-Pipeline.

## Datenvertrag

Vorgesehene Feldnamen, bei der Umsetzung konsistent in Kern und Map-/JSON-Ausgabe:

```text
TraceExpression.Block:
    content
    name                      // optional, unverändert
    variablesAtExit           // Map<String, TraceValue>

TraceProgram:
    expressions
    result
    variablesAtExit           // Snapshot des ausgeführten Programmblocks
```

Einträge sind echte aufgezeichnete Werte, keine Referenzen auf `Scope`,
`EvaluationEnvironment`, Laufzeit-Collections oder eine Closure-Umgebung.
Map und verschachtelte Trace-Collections sind unveränderlich.

Zur Variablenmenge gehören auch echte sichtbare Request-/Host-Variablen aus dem
globalen Variablen-Namespace. Funktions- und Operatorregistrierungen werden nicht
als Variablen erfunden. Eine tatsächlich in einer Variablen gespeicherte Funktion
bleibt als vorhandener `CallableRef` repräsentierbar.

Für einen nicht ausgeführten while-Body muss das Modell die Nichtausführung
ausdrücklich darstellen, vorzugsweise mit `body = null` in der abschließenden
Iteration. Ein wirklich ausgeführter leerer Body besitzt dagegen einen normalen
Snapshot. Kern-Serializer und alle konsumierenden Renderer gemeinsam anpassen;
keine zusätzliche Sprachsyntax einführen.

Keine Kompatibilitäts- oder Datenmigration implementieren. Bestehende Konstruktoren,
Fixtures und Assertions explizit anpassen; kein Fallback auf alte Kontext-Bindings.

## Arbeitspaket K1: Scopes charakterisieren und Capture spezifizieren

1. Die Testfälle K01–K20 unten zuerst als ausführbare Spezifikation anlegen.
   Anfangs fehlschlagende Tests sind die Grundlage der Implementierung.
2. Kleine interne Snapshot-Funktion in `EvaluationEnvironment` ergänzen, mit
   Zugriff auf die Variablen-Maps der beteiligten Scopes.
3. Globale Variablen, lexikalische Parent-Umgebungen und lokale Scopes anhand der
   tatsächlichen Namensbindung zusammenführen; die innerste sichtbare Deklaration
   gewinnt. Nicht den dynamischen Aufrufer-Trace durchsuchen. Die Runtime-Maps
   allein können zu viel enthalten: Eine geteilte Scope-Map kann später angelegte
   Namen enthalten, während der Parser einen Funktionszugriff schon auf einen
   anderen Scope gebunden hat (K20).
4. Falls die Charakterisierung diese Differenz bestätigt, im Parser pro
   `Expression.Block` die am Blockende lexikalisch sichtbaren lokalen Namen mit
   ihren Scope-Offsets festhalten. Beim Capture diese Bindungen über dieselbe
   Environment-Auflösung lesen; noch nicht ausgeführte Deklarationen auslassen,
   globale Variablen als globalen Namespace ergänzen. Die Metadaten beschreiben
   Namensbindung, keine zweite Ausführung. Konkrete Record-Namen klein halten;
   keinen allgemeinen Symbol-/Analyse-Service als Nebenprojekt bauen.
5. Werte unmittelbar mit `TraceValueMapper` einfrieren. Eine flache Map-Kopie
   mit weiterhin veränderlichen `PeelValue`-Collections genügt nicht.
6. Laufzeit- und Capture-Sicht an gezielt gelesenen Variablen vergleichen.
   Falls bestehende Scope-Auflösung dem vereinbarten Verhalten widerspricht,
   die Abweichung separat benennen; keine stillschweigende Sprachänderung.

Abnahme: Capture folgt der vorhandenen lexikalischen Auflösung und hält keine
veränderlichen Runtime-Referenzen fest.

## Arbeitspaket K2: Capture bei allen tatsächlichen Blockausgängen

1. `BlockTraceRecorder` bekommt eine Methode für den eingefrorenen Endzustand.
2. `evaluateBlock()` zeichnet den Zustand auf, solange sein Scope noch existiert:
   normal nach der letzten ausgeführten Anweisung oder beim Abfangen und
   Weiterreichen von `ReturnValueFlow`.
3. Scope-Abbau per `finally` absichern. Das Capture darf kein erfolgreiches
   Ergebnis für einen Laufzeitfehler erfinden oder dessen ursprüngliche Exception
   verdecken. Für diesen Umfang keine neue API für partielle Fehler-Traces bauen.
4. In `evaluate`, `evaluateIfStatement`, `runLoop`, `runForEachLoop` und
   `PeelCodeFunction` prüfen, wo zusätzliche Scopes geöffnet werden, und deren
   zuverlässigen Abbau bei `return`/Fehlern sicherstellen.
5. In `evaluateFunction` die ursprüngliche Umgebung ebenfalls per `finally`
   wiederherstellen. Rekursion und Closures mitprüfen.
6. `SimpleRuntime` übernimmt das Capture des Programmblocks in `TraceProgram`.
7. Reale leere Bodies von der abschließenden falschen while-Bedingung trennen.
8. `TraceMapOutput`/JSON-Ausgabe, Beispiele und betroffene Tests synchronisieren.

Abnahme: Resultate und Ausführungsreihenfolge bleiben unverändert. Capture ist
zusätzliche Information im Trace und verwendet keine zweite Evaluation.

## Verbindlicher Kern-Testkatalog

Die Namen sind vorgeschlagene JUnit-Testnamen. Alle Codefixtures verwenden die
vorhandene PEEL-Syntax mit Semikolons. Die beschriebenen Maps zeigen relevante
Ausschnitte; zusätzliche tatsächlich sichtbare Variablen nicht pauschal verbieten.
Explizit genannte fehlende Namen müssen dagegen fehlen.

| ID / Testname | Fixture / Ausführung | Erwartete Assertions |
| --- | --- | --- |
| K01 `capturesLastAssignmentAndInheritedValue` | `var a = 1; var b = 9; teil: { a = 2; a = 3; }` | `teil`: `a=3`, `b=9`; Wurzel: `a=3`, `b=9`. Keine neue Zuweisung an `b` erforderlich. |
| K02 `keepsShadowedVariablesSeparateFromBlockResult` | Dreifach-Verschattung aus Fixture A unten | Wurzel `wert=10`, `abschnitt` `wert=20`, `detail` `wert=31`; Ergebnis von `abschnitt` ist separat `31`. |
| K03 `propagatesWritesToActualOuterVariable` | `var x=1; a: { b: { x=7; } }` | `b`, `a` und Wurzel jeweils `x=7`; keine versehentliche lokale Kopie. |
| K04 `freezesEachWhileIteration` | Fixture B | Genau drei ausgeführte Bodies: `(jahr,lokal)=(1,3),(2,3),(3,3)`; Wurzel `jahr=3`, ohne `lokal`. Frühere Werte bleiben nach Laufende erhalten. |
| K05 `doesNotLeakConditionalShadowAcrossIterations` | Fixture C | `sonderfall` einmal mit `zins=5`; `runde` hat `(jahr,zins)=(1,3),(2,4)`; Wurzel `zins=4`, ohne `jahr`. |
| K06 `capturesAssignedForeachVariableAtExit` | `for (jahr in [1,2]) runde: { jahr=jahr+10; }` | Body-Snapshots `jahr=11,12`; die vorhandenen Iterations-Bindings dürfen weiterhin die Eintrittswerte `1,2` dokumentieren. Wurzel ohne `jahr`. |
| K07 `capturesParameterExitValuesWithoutCallerLocals` | Fixture D | `funktion`: `x=2,3`, ohne `jahr`; `aufruf`: `x=100`, `jahr=1,2`; Wurzel `x=100`. |
| K08 `capturesBeforeUnwindingNestedReturn` | Fixture E | `inner`: `x=2`, `lokal=7`; `funktion`: `x=2`, ohne `lokal`; Funktionsresultat `9`. Nicht ausgeführte Zuweisung `x=99` zählt nicht. |
| K09 `keepsRecursiveInvocationsIndependent` | Fixture F | Drei Funktionsvorkommen mit `n=3,2,1` und jeweils `lokal=30,20,10`; alle Werte auch nach vollständigem Unwinding unverändert, Resultat `6`. |
| K10 `capturesClosureDefinitionEnvironment` | Fixture G | Closure-Body `x=7`, Resultat `7`; der gleichnamige Aufruferwert `x=100` ersetzt den Capture-Wert nicht. |
| K11 `capturesAnonymousBlocksAndProgramRoot` | `var x=1; { var y=2; x=3; }` | Unbenannter Body: `x=3,y=2`; Programm-Capture: `x=3`, ohne `y`. |
| K12 `doesNotInventSnapshotsForSkippedBlocks` | `if (False) nie: { var x=99; } while (False) leer: { var y=99; } for (i in []) auchNie: { i; }` | Kein ausgeführtes Vorkommen dieser drei Bodies. Abschließende while-Bedingung bleibt dokumentiert, besitzt aber keinen ausgeführten Body. |
| K13 `distinguishesExecutedEmptyBodyFromSkippedBody` | `var x=4; leer: {}` sowie `for(i in [1]) wirklichLeer: {}` | `leer` hat Capture `x=4`; tatsächlich leerer Schleifenbody hat `i=1,x=4`. Keine Verwechslung mit Nichtausführung. |
| K14 `separatesMissingVariableFromNone` | `fun f() { return; } teil: { var n=f(); }` | `n` ist vorhanden und hat `TraceValue.none()`; `unbekannt` fehlt. |
| K15 `freezesNestedHostCollections` | Host-Binding `daten` mit veränderlicher verschachtelter Map/Liste; Script `teil: { daten; }`; Host mutiert Eingabe nach dem Lauf | Frühere Captures unverändert. Mutation der Snapshot-Map und ihrer Trace-Listen/Einträge ist nicht möglich. |
| K16 `includesVisibleBindingsAndHonorsLocalShadow` | Request-Binding `eingabe=10`; Script `teil: { var eingabe=20; }` | Wurzel `eingabe=10`, Teil `eingabe=20`; Request-/Runtime-Globale bleiben unverändert. |
| K17 `serializesSnapshotsWithoutRuntimeReferences` | Fixture mit Integer, Decimal, Float, Bool, Text, Liste, Map und in Variable gespeicherter Closure | Vorhandene Trace-Werttypen in Block und Programm-Map/JSON; Callable nur als Referenz, keine Umgebung/Runtime-Objekte; keine Assertions auf zufällige HashMap-Reihenfolge. |
| K18 `doesNotCaptureUnreachedDeclarations` | `fun f(x) teil: { return x; var spaeter=99; } f(2);` | Capture enthält `x=2`, nicht `spaeter`; Resultat `2`. |
| K19 `cleansUpScopesOnErrorAndSeparatesRuns` | Mehrere Läufe derselben Runtime mit unterschiedlichen Request-Bindings; dazwischen verschachtelter Lauf mit ungültigem Selector | Fehler wird unverändert gemeldet; nächster Lauf erhält keinen Zustand des ersten/fehlgeschlagenen Laufs. Bereits zurückgegebene Snapshots bleiben unverändert. |
| K20 `matchesResolvedBindingDespiteLaterSameNameDeclaration` | Request-Binding `x=7`; Script `fun f() funktion: { x; } var x=100; f();` | Der Parser bindet den Funktionszugriff an das globale `x`: Funktionsresultat und Capture `funktion.x` müssen beide `7` sein; Programm-Capture `x=100`. Ein späterer Eintrag in einer geteilten Parent-Scope-Map darf nicht fälschlich `100` im Funktions-Capture erzeugen. Den tatsächlichen Variablenzugriff als Referenz mitprüfen. |

### Fixture A: drei verschiedene Variablen mit demselben Namen

```text
var wert = 10;
abschnitt: {
    var wert = 20;
    detail: {
        var wert = 30;
        wert = 31;
    }
}
```

### Fixture B: while und äußerer Schreibzugriff

```text
var jahr = 0;
while (!(jahr == 3)) jahresblock: {
    var lokal = jahr + 1;
    jahr = lokal;
    lokal = 3;
}
```

Der Test verwendet bewusst keine neue `<`-Syntax. Zusätzlich nach dem Loop
`jahr=99;` anhängen und nachweisen, dass die drei Body-Captures bei `1,2,3`
bleiben, während sich nur der spätere Programm-Endzustand auf `99` ändert.

### Fixture C: Schatten existiert nur im ersten Durchlauf

```text
var zins = 2;
for (jahr in [1, 2]) runde: {
    if (jahr == 1) sonderfall: { var zins = 5; }
    zins = zins + 1;
}
```

### Fixture D: Parameter und Aufrufer-Scope

```text
fun berechnen(x) funktion: {
    x = x + 1;
    x;
}
var x = 100;
for (jahr in [1, 2]) aufruf: {
    berechnen(jahr);
}
```

### Fixture E: vorzeitiges return durch einen inneren Scope

```text
fun f(x) funktion: {
    inner: {
        var lokal = 7;
        x = 2;
        return x + lokal;
    }
    x = 99;
}
f(1);
```

### Fixture F: Rekursion mit getrennten lokalen Werten

```text
fun summe(n) funktion: {
    var lokal = n * 10;
    if (n == 1) { return 1; }
    return n + summe(n - 1);
}
summe(3);
```

### Fixture G: Closure statt dynamischer Aufrufer-Sichtbarkeit

```text
var x = 7;
var f = fun() closure: { x; };
aufrufer: {
    var x = 100;
    f();
}
```

Zusätzlich zu K19 einen instrumentierten Scope-/Environment-Test vorsehen:
Bei `return` und Fehlern werden alle geöffneten Scopes genau einmal entfernt
und die temporär gewechselte Umgebung wiederhergestellt. Ein frischer Runtime-
Run allein beweist dies nicht, weil er bereits neue Ausführungsumgebungen anlegt.

## Arbeitspaket W1: Trace-Captures im Web konsumieren

1. `TraceSelection.Occurrence` bekommt `variablesAtExit` statt der rekonstruierten
   Binding-Tabelle. Jedes Vorkommen verwendet ausschließlich sein eigenes Capture.
2. Die künstliche Programmwurzel übernimmt `TraceProgram.variablesAtExit`.
3. foreach-Eintrittsbindings und Funktionsargumente bleiben als Trace-Metadaten
   erhalten, werden aber nicht mehr als Variablen-Endwerte vererbt/überschrieben.
4. Nicht ausgeführte while-Bodies beim Traversieren und Rendern überspringen;
   deren Bedingung weiterhin korrekt darstellen.
5. `StructuredDocumentRenderer` liest den ausgewählten Namen aus dem Capture.
   Punktpfade für Maps bleiben erhalten. Kein automatischer Zugriff auf globale
   Request-Bindings, kein Interpretieren von PEEL-Ausdrücken und keine Auswahl
   einer gleichnamigen Variablen in einem anderen Vorkommen.
6. Unbekannte Variable und unbekanntes Map-Feld unterscheidbar melden, mit
   Dokumentpfad und ausgewähltem Block. `None` als vorhandenen Wert behandeln.
7. Bestehende Kardinalitätsregeln bleiben: Einzelwert/Berechnung genau ein
   Vorkommen, Wiederholung null bis viele; strukturelle Referenzfehler bleiben Fehler.
8. Vorschau und Print Jobs verwenden denselben Pfad. Skripte werden wie bisher
   einmal pro referenzierter Skript-ID und Dokumenterzeugung vollständig ausgeführt.

## Arbeitspaket W2: Editor, Vertrag und Dokumentation

- UI-Feld in beiden Sprachen verständlich beschriften, vorgesehen:
  „Variable (leer = Blockergebnis)“ / „Variable (empty = block result)“.
- Hint erläutert: letzter sichtbarer Wert beim Verlassen des ausgewählten
  Blockvorkommens. Map-Pfadbeispiele behalten. Keine automatische Umbenennung
  gespeicherter Werte oder neue Selektorsprache.
- Den internen Dokumentattributnamen `binding` zunächst beibehalten; seine
  Bedeutung wird auf den Capture-Zugriff umgestellt. Ein Feldrename bringt hier
  keinen Nutzervorteil und würde zusätzliche Speicher-/Clipboard-Pfade betreffen.
- Keine Variablenliste aus einem zweiten Frontend-Parser ableiten. Für diesen
  Umfang bleibt die Namenseingabe bestehen; eine Vorschlagsliste benötigt einen
  gesonderten Vertrag für vorhandene/fehlende Testausführungen.
- Soweit sich explizite API-DTOs ändern, OpenAPI exportieren und die beiden
  TypeScript-Dateien `src/types/api.ts` und `frontend/src/types/api.ts` synchron
  regenerieren. `RunResponse` enthält derzeit generische Trace-Maps; trotzdem
  deren neue Ausgabe mit Vertragstests dokumentieren.
- `STRUCTURED_DOCUMENTS.md` und `OPEN_POINTS.md` auf den dann realen Stand bringen.
  Zinsbeispiel erneut mit `while` und lokalen Variablen prüfen.

## Verbindliche Web- und UI-Tests

Tests nutzen echte Kern-Traces für Semantik; reine handgebaute Maps reichen nicht
als Nachweis. Kleine künstliche Maps nur für gezielte Fehlerfälle verwenden.

| ID | Verhalten / erwartete Assertions |
| --- | --- |
| W01 | Fixture B in `peelRepeat`: `peelValue(binding=jahr)` erzeugt `1,2,3`, `binding=lokal` jeweils `3`; die Programmebene liefert `jahr=3`. |
| W02 | Fixture A: `peelValue(binding=wert)` für `abschnitt` liefert `20`; leeres Feld liefert `31`; `detail` liefert `31`; Wurzel liefert für `wert` `10`. |
| W03 | Fixture C: erste Runde zeigt `zins=3`, zweite `4`; keine Ausgabe von `5` als Rundenvariable, keine nachträgliche Überschreibung des ersten Werts. |
| W04 | Fixture D: Funktionsvorkommen liefern `x=2,3`. Auswahl von `jahr` im Funktionskörper ergibt einen Variablenfehler, obwohl ein foreach-Aufrufer existiert. |
| W05 | Verschachtelte Wiederholungen behalten ihren Teilbaum und verwenden jeweils die dazugehörigen Captures. Kein Vermischen von Geschwistervorkommen, Skripten oder Rekursionsaufrufen. |
| W06 | Vorhandenes `None` wird als Wert dargestellt; fehlende Variable ergibt Fehler mit Dokumentpfad. `bauteil.name` funktioniert bei einer Map; fehlendes Feld und Zugriff auf einen Nicht-Map-Wert ergeben passende Fehler. |
| W07 | Leere Wiederholung, nicht ausgeführter Block und ungültiger Blockname behalten ihre bisherigen unterschiedlichen Ergebnisse. Kein falsches while-Body-Vorkommen. |
| W08 | Numerische Rundung betrifft nur die Ausgabe, HTML-Sonderzeichen bleiben escaped; Capture-Werte werden beim Rendern nicht verändert. |
| W09 | Ein Dokument verwendet dasselbe Skript in mehreren Werte-/Berechnungs-/Wiederholungselementen: Spy bestätigt genau eine Ausführung; erneutes Rendern derselben Traces führt keine Berechnung aus. |
| W10 | Vorschau und Print-Job-Renderer liefern für dieselben aufgelösten Dokumente/Bindings dieselben Variablenwerte. PDF enthält z. B. die drei Jahreswerte und keine lokalen Variablen außerhalb ihres Scopes. |
| W11 | Echte Tiptap-Node-View zeigt die neue Beschriftung und aktualisiert weiterhin `binding`. JSON-Speichern/Öffnen und HTML-Clipboard erhalten Name, Punktpfad, `scope`, `scriptId`, `block` und Nachkommastellen. |
| W12 | Vorhandene Save-/Konflikt-/Draft-Tests bleiben grün. Ein Variablenfehler in der Vorschau verändert weder Dokumententwurf noch bestätigte Dateiversion; veraltete Vorschauantworten bleiben ignoriert. |

## Reihenfolge und Abnahme

1. Kern-Charakterisierung und die erwarteten Captures als Tests festschreiben.
2. Laufzeit-Snapshot, korrektes Scope-Unwinding und Trace-Vertrag implementieren.
3. Vollständige Kern-Testsuite ausführen, einschließlich bestehender Ergebnis-,
   Rekursions-, Closure-, while- und Trace-Serialisierungstests.
4. Web auf den neuen Kernvertrag umstellen, bestehende Trace-/Renderer-Fixtures
   anpassen und W01–W10 ausführen. Projektgrenzen und Print Jobs mitprüfen.
5. UI-Beschriftung aktualisieren und W11–W12 sowie Übersetzungstests ausführen.
6. Frontend-Typprüfung und Produktionsbuild, gezielte Lint-Prüfung und
   `git diff --check` in beiden Repositories.
7. Browser-Abnahme: lokale Variable einfügen, zweimal zuweisen, Endwert anzeigen;
   drei while-Durchläufe wiederholen; Verschattung und Parameterbeispiel anzeigen;
   speichern, erneut öffnen und Vorschau/PDF prüfen. Einen nicht funktionierenden
   eingebetteten PDF-Viewer getrennt vom Inhalt der heruntergeladenen Datei bewerten.

Erster durchgängiger Meilenstein: Fixture B liefert aus dem echten Kern-Trace
ein Dokument mit drei lokalen Endständen und äußerem `jahr=3`, einschließlich
Speichern, erneutem Öffnen und PDF. Danach die übrigen Randfälle vervollständigen;
der Meilenstein ersetzt nicht die vollständige Testabnahme.

## Abgrenzung

Keine neue Blocksyntax, Scope-Regel, Trace-Abfragesprache, Datenmigration,
Variablenhistorien-UI oder zweite Berechnung im Renderer. Die separaten offenen
Punkte zum transparenten Hintergrund und zur strukturellen Filterung der
Blockauswahl werden durch diesen Plan nicht nebenbei umgesetzt.
