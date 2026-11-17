# Strukturierte Berechnungsdokumente

## Benannte PEEL-Blöcke

Jeder normale Block kann optional einen Identifier vor den öffnenden Klammern
tragen. Der Name ändert weder Scope noch Ausführung. Er ist innerhalb des
gesamten Skripts eindeutig, auch in nicht ausgeführten Zweigen und Funktionen.

```text
berechnung: {
    var summe = 0;
    for (wert in [2, 3]) schritt: {
        summe = summe + wert;
    }
    if (True) {
        summe * 2;
    }
}
```

Eigenständige benannte und unbenannte Blöcke sind Statements. Vorhandene
einzelne Statements als Schleifen-/Funktions-/Bedingungsrumpf bleiben möglich.
Namen sind nur an Blöcken mit geschweiften Klammern erlaubt.

Die Kernbibliothek bietet `PeelGrammar.parseWithBlocks(source)` für das Programm
und einen aus dem fertigen AST abgeleiteten Blockkatalog, ohne es auszuführen.
`BlockCatalog.analyze(program)` ist auch für bereits vorhandene oder direkt
erzeugte ASTs verwendbar und prüft doppelte Blocknamen. Der Parser-Visitor baut
ausschließlich den AST. `TraceExpression.Block`
und die Map-/JSON-Ausgabe enthalten einen nullable `name`.

## Dokument und Referenzen

`content.document` enthält den Tiptap-Dokumentbaum als JSON-Objekt. Es gibt keine
zusätzlich gespeicherte HTML-Vorlage und keinen JSON-String als zweite Quelle.
`renderConfigurationId` ist optional; ohne Referenz gilt die Standarddarstellung.
Bestehende Entwicklungsdokumente müssen neu erstellt werden; es gibt keinen
alten Renderingpfad und keine Datenmigration.

Unterstützte PEEL-Knoten:

| Typ | Inhalt / Attribute |
| --- | --- |
| `peelCalculation` | Blockelement ohne Kinder; `mode`: `detailed`, `compact`, `substituted`, `result`. |
| `peelValue` | Inline-Element; `binding` leer für das Blockergebnis, sonst sichtbarer Variablenname mit optionalem Map-Pfad, z. B. `bauteil.name`. |
| `peelRepeat` | Blockcontainer mit editierbaren Blockelementen; Ausgabe einmal je Vorkommen. |
| `peelTable` | Tabellenbaustein mit `scope`, `scriptId`, `block`; eine Datenzeile je Vorkommen. |
| `peelTableColumn` | Nur innerhalb einer Tabelle; `title` als konstanter Text, `align`: `left`, `center`, `right`; Zellvorlage aus Absätzen und Berechnungen. |

Werte, Berechnungen, Wiederholungen und Tabellen verwenden `scope: "script"`
mit `scriptId` oder `scope: "current"`
innerhalb einer Wiederholung. `block: ""` bedeutet das gesamte Skript bzw. das
aktuelle Vorkommen; ein Name wählt diesen Block im jeweiligen Kontext, inklusive
des aktuellen Blocks selbst. Bei `current` wird die Skript-ID des umgebenden
Vorkommens verwendet. `decimals` ist optional und liegt zwischen 0 und 12.

Direkte Skript-IDs werden aus dem Baum abgeleitet, projektgebunden aufgelöst
und beim Speichern sowie vor Vorschau/Druck geprüft. Löschschutz berücksichtigt
auch Referenzen in verschachtelten Wiederholungen. Es gibt keine Alias-Tabelle.

## Auswahl und Ausführung

Alle referenzierten Skripte laufen vollständig und je Dokumenterzeugung einmal.
Die Trace-Auswahl löst keine weitere Berechnung aus. Eintrittswerte von Schleifen
und Funktionsargumenten bleiben separate Trace-Metadaten.
Relative Auswahl innerhalb einer Wiederholung bleibt auf deren Teilbaum begrenzt.

Jeder ausgeführte Block und die Programmwurzel besitzen `variablesAtExit`:
einen unveränderlichen Snapshot aller beim Verlassen lexikalisch sichtbaren
Variablen. Die innerste Deklaration gewinnt; verlassene Unterblock-Variablen und
lokale Variablen des Aufrufers werden nicht hineinkopiert. Äußere Schreibzugriffe
wirken weiter. Jeder Schleifendurchlauf und Funktionsaufruf hält seinen eigenen
Endstand fest, bei `return` vor dem Scope-Abbau. Spätere Zuweisungen und Mutationen
an Host-Collections verändern frühere Trace-Werte nicht.

Ein Trace-Wert liest ausschließlich aus diesem Snapshot. Eine unbekannte Variable,
ein unbekanntes Map-Feld und ein Map-Zugriff auf einen anderen Werttyp ergeben
unterscheidbare Fehler mit Dokumentpfad. Ein vorhandenes `None` bleibt ein Wert.
Ein leeres Variablenfeld liefert weiterhin das separate Blockergebnis. Die UI
bezeichnet das Feld als „Variable (leer = Blockergebnis)“.

Die abschließende falsche while-Bedingung hat `body = null`, da kein Rumpf ausgeführt
wurde. Ein tatsächlich ausgeführter leerer Block hat dagegen einen normalen Snapshot.

Einzelbausteine verlangen genau ein Vorkommen. Null oder mehrere Vorkommen ergeben
einen Fehler mit Dokumentpfad. Unbekannte Blocknamen werden anhand des statischen
Katalogs von bekannten, nicht ausgeführten Blöcken unterschieden. Wiederholungen
akzeptieren null bis beliebig viele Vorkommen in Trace-Reihenfolge. Eine leere
Wiederholung erzeugt keinen Inhalt; die Vorschau liefert einen Hinweis in
`warnings`. Warnungen gehören zum jeweiligen Vorschau-Request.

## Editor und Ausgabe

Der Banana-Button öffnet die Bausteinauswahl. Jeder Baustein bietet Skript,
Block und Darstellung direkt in seiner Node-View an. Die Blockliste verwendet
gespeicherte Skripte und lässt sich aktualisieren. Der projektgebundene Endpunkt
`GET /api/projects/{projectId}/files/{fileId}/blocks` führt kein Skript aus.
Wiederholungsvorlagen werden einmal bearbeitet, nicht als editierbare Laufzeitkopien.

Der Endpunkt liefert Einträge mit `name`, optionalem `parentName` (nächster
benannter syntaktischer Vorfahr) und `reachableBlocks` (mögliche Trace-Nachfahren,
einschließlich des Blocks selbst). Unbenannte Zwischenblöcke werden durchlaufen.
Bei Quelle „Skript“ werden alle Namen angeboten; bei „Aktuelle Wiederholung“ nur
die möglichen Nachfahren des dort ausgewählten Blocks. Eine leere Blockauswahl
übernimmt das umgebende Vorkommen, auch durch verschachtelte Wiederholungen.

Die Analyse berücksichtigt alle Zweige und Schleifen unabhängig von Eingaben.
Funktionsdeklarationen erzeugen für sich keine Ausführungskante; Aufrufe verbinden
die möglichen Funktionskörper. Gleiche Funktionsnamen und Überladungen bleiben
konservative Kandidaten. Bei variablen/dynamischen oder externen Aufrufzielen
bleiben alle im Skript definierten Callable-Körper mögliche Ziele. Rekursion wird
mit einem Besuchsset begrenzt. Daher kann die Liste mehr Kandidaten enthalten, als
ein konkreter Lauf erreicht. Der Trace-Resolver entscheidet weiterhin über echte
Vorkommen; die UI ist eine Auswahlhilfe, keine Ausführungsgarantie.

Ändert sich ein Vorfahr, werden relative Listen sofort neu gefiltert. Eine bereits
gespeicherte, nun nicht passende Referenz bleibt sichtbar, aber als deaktivierte
Option erhalten; sie wird nicht stillschweigend gelöscht oder umgebogen.

Der Server validiert den Dokumentbaum, löst Trace-Referenzen auf und erzeugt HTML.
Pebble rendert weiterhin die Ausdrucksfragmente; die Auswahl- und
Wiederholungssemantik liegt in Java. Die ausführliche Darstellung ergänzt
eingesetzte Werte und Ergebnisse, die kompakte Darstellung Formel und Ergebnis.
„Eingesetzte Werte und Ergebnis“ (`substituted`) lässt die symbolische Formel weg,
zum Beispiel `zinsen = (1000 * 0.05) = 50`. Zuweisungsnamen und `return` bleiben zur
Zuordnung erhalten. Bedingungen und aufgezeichnete Funktionskörper verwenden
ebenfalls eingesetzte Werte. Bereits aufgelöste Einzelwerte/Selektoren werden
ohne redundantes `Wert = Wert` dargestellt; Rundung betrifft nur die Ausgabe.
Unbenannte Blöcke und alle aufgezeichneten Schleifendurchläufe werden beim Rendern
eines übergeordneten Blocks berücksichtigt. Die ausführliche Darstellung zeigt
auch aufgezeichnete Funktionskörper. Rundung betrifft nur die Darstellung.
Vorschau und Print Jobs verwenden dieselbe HTML-/OpenPDF-Pipeline.

## Prüfungen

- Kern: `./gradlew.bat test` im benachbarten `peel`-Repository.
- Server: `TableDocumentTest`, `StructuredDocumentTest`, `ExpressionRendererTest`, `ProjectFileTest`,
  `ProjectAuthorizationTest`, `ProjectPrintJobsTest`, `ProjectPrintJobCompletionTest`.
- `DocumentOpenApiTest` exportiert den aktuellen Vertrag nach `build/openapi.json`.
- Frontend: Tiptap-Roundtrip, echte Vue-Node-Views, Dokument-/Datei-Lifecycle,
  Vorschau-Request-Zuordnung, Typprüfung und Produktionsbuild.

Tabellen bearbeiten eine Musterzeile mit einer oder mehreren Spalten. Zellen
enthalten Absätze mit Text/Inline-Werten oder Berechnungsbausteine. Deren relative
Quelle bezieht sich auf das konkrete Zeilen-Vorkommen. Tabellen dürfen innerhalb
freier Wiederholungen stehen; verschachtelte Tabellen, Zellverbindungen und freie
Wiederholungen in Zellen sind nicht erlaubt. Bei null Treffern wird die gesamte
Tabelle ausgelassen und eine Warnung ausgegeben. Fehlerpfade enthalten Zeile und Spalte.

Die PDF-Ausgabe verwendet gleich breite Spalten, wiederholt Kopfzeilen auf
Folgeseiten und hält normale Zeilen möglichst zusammen. Überhohe Zeilen können
über Seiten umbrechen. Die Tests prüfen Kopfzeilen und vollständigen Text auch
in mehrseitigen Tabellen und überhohen Zellen.

Eigene Nutzerbausteine, freie Selektorausdrücke, Summenzeilen und Gruppierung
sind nicht Bestandteil dieses Umfangs. Spezifikation und Testkatalog stehen in
[REPEAT_TABLE_PLAN.md](../../docs/REPEAT_TABLE_PLAN.md).
