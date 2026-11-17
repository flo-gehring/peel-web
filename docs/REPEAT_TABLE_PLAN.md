# Plan: Wiederholte Trace-Blöcke als Tabelle

Stand: 17. November 2026. Vom Nutzer freigegeben und implementiert.

## Umsetzungsstand

- `peelTable` und `peelTableColumn` sind im Editor, in der Dokumentvalidierung
  und im gemeinsamen HTML-/PDF-Renderer verfügbar. Die Kernbibliothek bleibt unverändert.
- Die Bausteinauswahl fügt eine Tabelle mit zwei Muster-Spalten ein und schließt
  danach. Spalten bieten Überschrift, Ausrichtung, Verschieben und Entfernen;
  die letzte Spalte bleibt geschützt, Änderungen unterstützen Undo.
- Werte und Berechnungen nutzen den Zeilenkontext und den vorhandenen Blockkatalog.
  Ungültige Blocktypen sind in Zellen durch Schema, Einfügeaktionen und Servervalidierung gesperrt.
- `PeelTable.spec.ts` prüft Schema/Clipboard und echte Node-Views;
  `TableDocumentTest` prüft Trace-Auswahl, Fehler, Formatierung und PDF-Umbrüche.
  Datei- und Autorisierungstests prüfen Versionen, Löschschutz und Druckaufträge.
- Mehrseitige Test-PDFs wurden als Bilder gerendert und visuell geprüft.
  Eine zusätzliche Live-Browserabnahme ist noch nicht durchgeführt.

Der nachfolgende Plan bleibt als freigegebene Spezifikation erhalten.

## Ziel und Vorschlag

Ein ausgewähltes ausgeführtes Blockvorkommen erzeugt genau eine Tabellenzeile.
Spaltenüberschriften und Zellvorlagen werden einmal im Dokument bearbeitet.
Die Auswahl arbeitet wie `peelRepeat`: vollständige Skriptausführung, danach
Auswahl aus dem vorhandenen Trace. Keine zusätzliche PEEL-Syntax oder Berechnung.

Beispiel: `jahresberechnung` liefert zehn Vorkommen. Die Tabelle hat die Spalten
Jahr, Anfangskapital, Zinsen und Endkapital. Eine Zelle kann auch einen
Berechnungsbaustein mit „Eingesetzte Werte und Ergebnis“ enthalten.

| Jahr | Anfangskapital | Zinsen | Endkapital |
| --- | --- | --- | --- |
| 1 | 1.000,00 € | 50,00 € | 1.050,00 € |
| 2 | 1.050,00 € | 52,50 € | 1.102,50 € |

Die Beispieldarstellung mit Dezimalkomma ist illustrativ. Das erste Tabellenfeature
verwendet die vorhandene Zahlenformatierung; es führt keine neue Locale-Regel ein.
Die Werte stammen aus den Variablen-Captures des jeweiligen Jahresblocks.

## Datenmodell: eigener Tabellenbaustein mit gemeinsamer Auswahlsemantik

Empfehlung: `peelTable` und `peelTableColumn` ergänzen. Eine Tabelle hat einen
anderen editierbaren Aufbau als die freie Blockvorlage eines `peelRepeat`.
Eigene Knotentypen erlauben eine klare Tiptap-Struktur, statt den erlaubten
Kindaufbau abhängig von einem Layout-Attribut umzudeuten. Den Trace-Resolver und
die vorhandenen Werte-/Berechnungsbausteine gemeinsam verwenden.

```text
peelTable { scope, scriptId, block }
  peelTableColumn { title: "Jahr", align: "left" }
    paragraph
      peelValue { scope: "current", binding: "jahr" }
  peelTableColumn { title: "Zinsen", align: "right" }
    paragraph
      peelValue { scope: "current", binding: "zinsen", decimals: 2 }
      text " €"
```

- Tabellen haben mindestens eine Spalte. Spalten sind nur direkte Kinder einer
  Tabelle und gehören nicht zur allgemeinen `block`-Gruppe.
- Im ersten Umfang enthalten Zellen Absätze mit Text/Inline-Werten und
  `peelCalculation`. Keine verschachtelten Tabellen, zusammengeführten Zellen,
  frei platzierten Listen oder Wiederholungen innerhalb von Zellen.
- Tabellen selbst dürfen innerhalb bestehender Wiederholungen stehen. Quelle
  „Aktuelle Wiederholung“ wählt dort pro äußeres Vorkommen die inneren Zeilen.
- Überschriften sind konstanter Text. `align` ist `left`, `center` oder `right`;
  Nachkommastellen bleiben am jeweiligen Werte-/Berechnungsbaustein.
- Kein automatischer Umbau bestehender Wiederholungen: Die freie Vorlage kann
  Überschriften, Listen und mehrere Absätze enthalten, die sich nicht eindeutig
  in Spalten zerlegen lassen. Tabellen werden zunächst gezielt neu eingefügt.

## Auswahl- und Fehlerverhalten

- `scope`, `scriptId` und `block` verwenden den vorhandenen `TraceSelection`-Pfad
  und den statischen Blockkatalog. Die Reihenfolge folgt dem Trace; keine
  implizite Sortierung, Aggregation oder Gruppierung.
- Für jede Zeile wird deren konkretes Blockvorkommen als `current` gesetzt.
  Die Zellinhalte lesen ausschließlich die passenden `variablesAtExit`-Captures.
  Ein leeres Variablenfeld liefert weiterhin das Blockergebnis.
- In einer Tabellenzelle ist der Zeilenkontext für relative Quellen maßgeblich.
  Der Frontend-Kontextresolver muss deshalb Tabellen genauso wie Wiederholungen
  berücksichtigen; ein zweiter Sprachparser ist nicht nötig.
- Zellbausteine dürfen über die bestehende Quelle „Skript“ auch feste globale
  Werte darstellen. Die Einzelwert-Kardinalitätsregel bleibt bestehen.
- Bei null Zeilen wird die gesamte Tabelle ausgelassen, einschließlich Kopfzeile,
  und wie bei `peelRepeat` eine Vorschauwarnung erzeugt.
- Fehlende Variable, unbekannter Block und mehrdeutige Einzelzellauswahl bleiben
  Fehler. Keine stillen Leerzellen oder automatischen Ersatzwerte.
- Fehlerpfade enthalten Tabellen-, Zeilen- und Spaltenposition, damit zum Beispiel
  ein nur im dritten Vorkommen fehlender Wert eindeutig zugeordnet werden kann.

## Editorbedienung

- In der Bausteinauswahl „Tabelle aus Wiederholung“ anbieten; nach Einfügen schließt
  die Auswahl wie bei den vorhandenen Bausteinen.
- Quelle und Block werden einmal an der Tabelle gewählt. Darunter steht eine
  editierbare Musterzeile; die zehn Laufzeitzeilen existieren nur in der Vorschau.
- Jede Spalte besitzt Überschrift, Ausrichtung und ihre Zellvorlage. Spalten
  hinzufügen, entfernen und nach links/rechts verschieben; letzte Spalte kann
  nicht entfernt werden. Entfernen bleibt über Editor-Undo rückgängig machbar.
- Zellen verwenden vorhandene Trace-Wert-/Berechnungsbausteine. Die Einfügeaktion
  im Zellkontext muss unzulässige Blocktypen verhindern.
- Transparenter Hintergrund, kleine Symbolbuttons und Dokumenttextfarbe wie bei
  den übrigen Bausteinen. Tabellenstruktur durch dezente Linien sichtbar machen.
- JSON-Speichern/Öffnen und HTML-Clipboard müssen auch Spaltenreihenfolge,
  Überschriften, Ausrichtung und verschachtelte Referenzen erhalten.

## HTML, PDF und technische Schritte

1. Tests für Knotenschema und Tabellen-Semantik zuerst festschreiben.
2. In `peelNodes.ts` zwei Tiptap-Knoten und passende Node-Views ergänzen.
   Header/Steuerung einmal bearbeiten; für Zellinhalte Tiptaps regulären
   Content-DOM verwenden, ohne verschachtelte unabhängige Editorinstanzen.
3. `DocumentNode` um Knotentypen, Platzierungsprüfung, Attribute und
   Skriptreferenz-Sammlung erweitern. Relative Quellen in Tabellenzellen erlauben.
4. In `StructuredDocumentRenderer` die vorhandene Auswahl und Zellrenderer nutzen:
   ein `<table>`, ein `<thead>`, ein `<tbody>`, pro Vorkommen ein `<tr>` und pro
   Spalte ein `<td>`. Header mit `<th scope="col">`; Texte konsequent escapen.
5. Dieselbe HTML-/OpenPDF-Pipeline für Vorschau und Print Jobs verwenden.
   Kopfzeilen auf Folgeseiten wiederholen. Normale Zeilen möglichst zusammenhalten;
   überhohe Zeilen dürfen keinen Inhalt verlieren und brauchen einen geprüften
   Umbruch. Nicht allein von Browser-CSS auf PDF-Verhalten schließen.
6. Zunächst gleich breite Spalten innerhalb der Dokumentbreite. Lange Texte
   umbrechen. Freies Ziehen von Spaltenbreiten ist ein späteres Feature.
7. Löschschutz, Projektgrenzen, Dateiversionierung, Drafts und Print-Job-Snapshots
   für Referenzen innerhalb von Tabellen prüfen. Gemeinsame Dateilifecycle-Pfade
   beibehalten, keine Migration hinzufügen.
8. Bei API-DTO-Änderungen OpenAPI exportieren und beide TypeScript-Kopien
   regenerieren. Generische Dokumentattribute allein benötigen keinen neuen Typ.

Im PEEL-Kern ist für diesen Vorschlag keine Änderung erforderlich.

## Verbindliche Abnahmetests vor Umsetzung

| ID | Fall | Erwartung |
| --- | --- | --- |
| T01 | Zehn Jahresblock-Vorkommen, vier Spalten | Ein Tabellenkopf, exakt zehn Datenzeilen, richtige Reihenfolge und Werte je Jahr. |
| T02 | Lokale Variable wird mehrfach zugewiesen; Verschattung | Zelle verwendet den Endwert des konkreten Vorkommens; keine Werte aus Geschwisterzeilen. |
| T03 | Null Treffer bzw. unbekannter Block | Leere Wiederholung: keine Tabelle und Warnung; unbekannter Name: Fehler. |
| T04 | Variable fehlt nur in einer Zeile | Fehler nennt Tabelle, Zeile und Spalte; vorhandenes `None` wird als Wert ausgegeben. |
| T05 | Text, Map-Pfad, Rundung und HTML-Sonderzeichen | Bestehende Formatierung und Escaping; Captures werden nicht verändert. |
| T06 | Zellberechnung mit allen vier Darstellungsmodi | Formel/Einsetzung/Ergebnis entsprechen jeweils dem ausgewählten Modus. |
| T07 | Tabelle innerhalb einer äußeren Wiederholung | Zeilen und relative Blockliste bleiben im passenden äußeren Kontext. |
| T08 | Mehrfachverwendung desselben Skripts | Genau eine Skriptausführung pro Dokumenterzeugung, auch mit Tabellen und freien Wiederholungen. |
| T09 | Spalten ändern und Tabelle speichern/kopieren | Reihenfolge, Überschrift, Ausrichtung, Quellen und Zellvorlagen bleiben in JSON und HTML erhalten. |
| T10 | Ungültige Knotenstruktur | Spalte außerhalb einer Tabelle, null Spalten und verschachtelte Tabellen werden vor dem Rendern abgewiesen. |
| T11 | Mehrseitige PDF-Tabelle und überhohe Zeile | Header wiederholt; kein abgeschnittener/verlorener Inhalt; PDF-Inhalt und gerenderte Seiten prüfen. |
| T12 | Projektgrenzen, Löschschutz, Save-Konflikt und Print Job | Bestehende Sicherheits-/Dateiregeln gelten auch für Tabellenreferenzen und eingefrorene Druckvorlagen. |

## Freigabe und Abgrenzung

Die konkreten Produktentscheidungen dieses Entwurfs wurden vom Nutzer bestätigt:
eigener Tabellenbaustein, Kopfzeile als konstanter Text, erlaubte Zellinhalte und
Verhalten bei null Zeilen.
Summenzeilen, Gruppierung, Zellverbindungen und variable Spaltenzahlen bleiben
außerhalb des ersten Umfangs.
