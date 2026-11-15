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
und den Katalog aller Blocknamen, ohne es auszuführen. `TraceExpression.Block`
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
| `peelCalculation` | Blockelement ohne Kinder; `mode`: `detailed`, `compact`, `result`. |
| `peelValue` | Inline-Element; `binding` leer für das Blockergebnis, sonst Kontextname mit optionalem Map-Pfad, z. B. `bauteil.name`. |
| `peelRepeat` | Blockcontainer mit editierbaren Blockelementen; Ausgabe einmal je Vorkommen. |

Alle drei verwenden `scope: "script"` mit `scriptId` oder `scope: "current"`
innerhalb einer Wiederholung. `block: ""` bedeutet das gesamte Skript bzw. das
aktuelle Vorkommen; ein Name wählt diesen Block im jeweiligen Kontext, inklusive
des aktuellen Blocks selbst. Bei `current` wird die Skript-ID des umgebenden
Vorkommens verwendet. `decimals` ist optional und liegt zwischen 0 und 12.

Direkte Skript-IDs werden aus dem Baum abgeleitet, projektgebunden aufgelöst
und beim Speichern sowie vor Vorschau/Druck geprüft. Löschschutz berücksichtigt
auch Referenzen in verschachtelten Wiederholungen. Es gibt keine Alias-Tabelle.

## Auswahl und Ausführung

Alle referenzierten Skripte laufen vollständig und je Dokumenterzeugung einmal.
Die Trace-Auswahl löst keine weitere Berechnung aus. Schleifen-Bindings,
Funktionsparameter und die Baumposition bleiben an jedem Vorkommen erhalten.
Relative Auswahl innerhalb einer Wiederholung bleibt auf deren Teilbaum begrenzt.

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

Der Server validiert den Dokumentbaum, löst Trace-Referenzen auf und erzeugt HTML.
Pebble rendert weiterhin die Ausdrucksfragmente; die Auswahl- und
Wiederholungssemantik liegt in Java. Die ausführliche Darstellung ergänzt
eingesetzte Werte und Ergebnisse, die kompakte Darstellung Formel und Ergebnis.
Unbenannte Blöcke und alle aufgezeichneten Schleifendurchläufe werden beim Rendern
eines übergeordneten Blocks berücksichtigt. Die ausführliche Darstellung zeigt
auch aufgezeichnete Funktionskörper. Rundung betrifft nur die Darstellung.
Vorschau und Print Jobs verwenden dieselbe HTML-/OpenPDF-Pipeline.

## Prüfungen

- Kern: `./gradlew.bat test` im benachbarten `peel`-Repository.
- Server: `StructuredDocumentTest`, `ExpressionRendererTest`, `ProjectFileTest`,
  `ProjectAuthorizationTest`, `ProjectPrintJobsTest`, `ProjectPrintJobCompletionTest`.
- `DocumentOpenApiTest` exportiert den aktuellen Vertrag nach `build/openapi.json`.
- Frontend: Tiptap-Roundtrip, echte Vue-Node-Views, Dokument-/Datei-Lifecycle,
  Vorschau-Request-Zuordnung, Typprüfung und Produktionsbuild.

Eigene Nutzerbausteine, freie Selektorausdrücke und neue Tabellenbausteine sind
nicht Bestandteil dieses ersten Umfangs.
