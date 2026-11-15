# PEEL: offene Punkte aus der Produktbesprechung

Stand: 15. November 2026. Diese Liste wird bei weiteren Beobachtungen und
Entscheidungen im Gespräch ergänzt. Ein Eintrag ist keine automatische Freigabe
zur Umsetzung. Bestehende fremde Änderungen bleiben erhalten.

## 1. Lesbarkeit eingebetteter Bausteine im Dokument

- Status: umgesetzt. Der Baustein-Hintergrund ist transparent, Dokumentinhalt
  erbt die Textfarbe des Dokuments. Das gilt auch innerhalb verschachtelter
  Wiederholungen. Auch die Einstellungsbereiche und geschlossenen Inline-Werte
  sind transparent und übernehmen die Dokumenttextfarbe. Eingabefelder und
  Schaltflächen behalten ihren eigenen kontrastreichen Hintergrund.
- Beobachtung des Nutzers: Der PEEL-graue Hintergrund eingebetteter Bausteine
  verschlechtert den Kontrast zur Dokumentenschrift.
- Gewünschte Richtung: Hintergrund des Dokumentinhalts transparent gestalten,
  sodass die Dokumentenfarbe durchscheint. Textfarbe passend zum Dokument erben.
- Umsetzung: `frontend/src/components/document/PeelNodeView.vue` setzt
  `.peel-node { background: transparent; color: inherit; }`. Der dunkle Hintergrund
  entfällt auch auf `.peel-node-settings`; Auswahlrahmen bleiben erhalten.
- Ursache der weiterhin alten Browseranzeige: Vite im Docker-Devcontainer erhielt
  keine zuverlässigen Dateiänderungsereignisse vom Windows-Bind-Mount und lieferte
  seinen alten Modul-/CSS-Cache auch nach einem Browser-Reload aus. Der Devcontainer
  aktiviert deshalb Polling für Vites Dateiüberwachung.

## 2. Bedeutung und Beschriftung der Variablenauswahl

- Status: implementiert. Semantik und Testkatalog stehen in
  [VARIABLE_CAPTURE_PLAN.md](VARIABLE_CAPTURE_PLAN.md).
- Das Feld heißt jetzt „Variable (leer = Blockergebnis)“. Ein Hinweis erläutert
  den letzten sichtbaren Wert beim Verlassen des ausgewählten Blockvorkommens.
- Der Kern erfasst `variablesAtExit` an jedem tatsächlich ausgeführten Block und
  der Programmwurzel. Lokale und sichtbare äußere Variablen sind damit auswählbar.
- Web liest ausschließlich diesen Snapshot; Map-Pfade wie `bauteil.name`
  bleiben möglich. Das Feld wertet keinen PEEL-Ausdruck aus.
- Die frühere Auswahl sammelte foreach-Eintrittswerte und Funktionsargumente
  aus dem Trace-Kontext. Diese Metadaten bleiben vorhanden, werden aber nicht
  mehr als Variablen-Endwerte verwendet. Spätere Zuweisungen sind jetzt berücksichtigt.
- Leeres Feld bedeutet weiterhin Blockergebnis. Unbekannte Variablen erzeugen
  einen Fehler mit Dokumentpfad. Ein vorhandenes `None` gilt als Wert.
- Automatisierte Kern-, Rendering- und Editorprüfungen sind vorhanden.
  Die erneute manuelle Browserabnahme steht noch aus.

### Festgehaltene erste Regel: letzte Zuweisung je ausgeführtem Blockvorkommen

- Nutzerentscheidung: Für den Anfang gilt die letzte tatsächlich ausgeführte
  Zuweisung einer Variablen innerhalb des jeweiligen ausgeführten Blocks.
  Lokale Variablen sollen dadurch ebenfalls auswählbar werden. Die bislang
  gesammelten Eintrittswerte allein reichen dafür nicht aus.
- Jedes Schleifenrumpf-Vorkommen bekommt seinen eigenen Endstand. Frühere
  Vorkommen dürfen durch spätere Iterationen nicht überschrieben werden.
- Das bewusste Beispiel mit `while` zeigt, dass diese Semantik nicht von einer
  speziellen foreach-Iterationsbindung abhängen soll:

  ```text
  jahr = 0;
  while (jahr < 3) {
      var notVisibleOutside = jahr + 1;
      jahr = notVisibleOutside;
      notVisibleOutside = 3;
  }
  ```

- Inhaltlicher Hinweis zum konzeptuellen Beispiel: Bei `< 3` gibt es drei
  ausgeführte Rümpfe, mit den Endständen
  `{notVisibleOutside: 3, jahr: 1}`, `{notVisibleOutside: 3, jahr: 2}` und
  `{notVisibleOutside: 3, jahr: 3}`. Im vorgeschlagenen äußeren Endstand wäre
  `jahr` ebenfalls `3`; die vom Nutzer skizzierte Liste mit zwei Einträgen und
  äußerem `jahr = 2` passt zu zwei Durchläufen. Keine Änderung der PEEL-Syntax
  aus dieser konzeptuellen Notation ableiten.
- Lokale Variablen wie `notVisibleOutside` bleiben auf ihr jeweiliges
  Blockvorkommen beschränkt. Zuweisungen an eine äußere Variable wie `jahr`
  werden davon unterschieden. Äußere Endstände und Verschattung werden jetzt
  anhand der lexikalischen Laufzeitumgebung direkt im Kern erfasst.
- Aufgelöster technischer Prüfpunkt: Zuweisungen werden nicht anhand gleicher
  Namen aus Unterblöcken zusammengesammelt. Der Kern nimmt den Snapshot direkt
  aus den lexikalischen Laufzeit-Scopes auf, bevor er sie entfernt.
- Nur tatsächlich ausgeführte Blöcke berücksichtigen. Die abschließende falsche
  while-Bedingung ist kein weiterer ausgeführter Schleifenrumpf.

### Vorschlag zur Verschattung: sichtbare Variablen beim Blockende

Status: Vom Nutzer am 15. November 2026 bestätigt und implementiert.
Umsetzungs- und Testplan: [VARIABLE_CAPTURE_PLAN.md](VARIABLE_CAPTURE_PLAN.md).
Der Plan dokumentiert auch den Prüfpunkt späterer Deklarationen in geteilten
Scope-Maps gegenüber bereits aufgelösten Namen im Funktionscode (Test K20).
Maßgeblich soll die echte lexikalische Variablenauflösung der
Sprache sein. Gleicher Name bedeutet nicht gleiche Variable.

- Der Wert einer ausgewählten Variablen entspricht ihrem letzten Zustand am
  Ende des konkreten Blockvorkommens. Dabei auch sichtbare äußere Variablen
  berücksichtigen. Bei vorzeitigem `return` zählt der Stand beim Verlassen.
- Die innerste zu diesem Zeitpunkt sichtbare Deklaration gewinnt. Lokale
  Deklarationen in bereits verlassenen Unterblöcken gehören nicht zur Variablenliste
  des Elternblocks. Schreibzugriffe auf echte äußere Variablen wirken dagegen weiter.
- Technische Empfehlung: Der Kern hält beim Verlassen eines Blockvorkommens
  einen unveränderlichen Snapshot der sichtbaren Variablen im Trace fest, bevor
  der lokale Scope entfernt wird. Die Webanwendung liest diesen Snapshot.
  Damit muss sie weder Scopes anhand gleicher Namen erraten noch Ausführung
  erneut simulieren. Das Feld heißt `variablesAtExit`, auch an der Programmwurzel.
- Ein Snapshot darf nicht auf einen später weiterveränderten Runtime-Scope
  verweisen. Schleifendurchläufe und rekursive Aufrufe bleiben getrennt.
- Aufruf-/Schleifenmetadaten getrennt von lexikalisch sichtbaren Variablen führen.
  Ein Name aus dem aufrufenden Trace-Teilbaum ist nicht automatisch im
  aufgerufenen Funktionskörper sprachlich sichtbar.
- Das Blockergebnis bleibt separat bestehen; es muss keiner sichtbaren Variablen
  gleichen. Beispiel: Der letzte Ausdruck kann ein Unterblock mit eigener Variable sein.

Die drei diskutierten Randfälle und das vorgeschlagene Verhalten:

1. **Verschachtelte lokale Deklarationen:**

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

   Variablen-Endstände: Wurzel `wert = 10`, `abschnitt` `wert = 20`, `detail`
   `wert = 31`. Ergebnis von `abschnitt` trotzdem `31`, da `detail` sein letzter
   ausgewerteter Ausdruck ist. Kein Überschreiben äußerer Namen durch lokale Schatten.

2. **Bedingter Schatten innerhalb wiederholter Blöcke:**

   ```text
   var zins = 2;
   for (jahr in [1, 2]) runde: {
       if (jahr == 1) sonderfall: { var zins = 5; }
       zins = zins + 1;
   }
   ```

   `sonderfall` hat genau ein Vorkommen mit `zins = 5`. Die Endstände von
   `runde` sind `{jahr: 1, zins: 3}` und `{jahr: 2, zins: 4}`. Der äußere
   Endstand ist `zins = 4`. Der erste Snapshot bleibt `3`; der Schattenwert `5`
   darf nicht in den Elternblock oder den nächsten Durchlauf gelangen.

3. **Funktionsparameter gegenüber Aufrufer und Aufrufkontext:**

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

   Funktionsvorkommen liefern für die Variable `x` die Endwerte `2` und `3`,
   nicht nur die Argument-Eintrittswerte `1` und `2`. Das äußere `x` bleibt `100`;
   in `aufruf` gelten `{jahr: 1, x: 100}` bzw. `{jahr: 2, x: 100}`. `jahr` ist
   im außerhalb der Schleife definierten Funktionskörper keine lexikalisch
   sichtbare Variable. Aufrufmetadaten könnten es separat beschreiben, sollten
   es aber nicht als Funktionsvariable ausgeben.

## 3. Blockauswahl innerhalb einer Wiederholung einschränken

- Status: implementiert als konservative, statische Auswahlhilfe. Der Resolver
  arbeitet weiterhin anhand der tatsächlich ausgeführten Trace-Vorkommen.
- Beobachtung des Nutzers: Bei Quelle „Aktuelle Wiederholung“ bietet die
  Blockauswahl auch Blöcke an, die gar nicht innerhalb dieser Wiederholung liegen.
- Früherer Grund: Die UI lud nur einen flachen Blocknamenkatalog des Skripts.
  Der Resolver begrenzt die relative Auswahl dagegen auf den aktuellen
  Trace-Teilbaum, einschließlich des aktuellen Vorkommens selbst.
- Ein bekannter Block außerhalb dieses Teilbaums liefert daher null Treffer.
  Ein Einzelwert bzw. eine Berechnung meldet einen Fehler; eine Wiederholung
  erzeugt keinen Inhalt und einen Vorschauhinweis. Es erfolgt kein automatischer
  Wechsel zur skriptweiten Auswahl.
- Vorgeschlagene Richtung: Bei relativer Quelle nur strukturell erreichbare
  Blockdefinitionen anbieten. Bei Quelle „Skript“ bleibt die skriptweite Auswahl.
  „Aktueller Block“ muss weiterhin auswählbar bleiben.
- Dabei strukturell unmögliche Referenzen von bedingt nicht ausgeführten Blöcken
  unterscheiden. Ein Unterblock in einem `if` kann bei anderen Eingaben vorkommen
  und sollte nicht allein aufgrund eines einzelnen Testlaufs ausgefiltert werden.
- Umsetzung: Der Kern analysiert den fertigen AST. Der Katalog enthält Namen,
  nächste benannte syntaktische Eltern und mögliche Trace-Nachfahren. Bekannte
  Funktionsaufrufe werden verfolgt; bei dynamischen Aufrufzielen bleiben alle
  Callable-Körper Kandidaten. Rekursion terminiert über ein Besuchsset.
- Die UI filtert anhand des nächsten wirksamen Wiederholungskontexts und reagiert
  auch auf Änderungen an Vorfahren. Bestehende unpassende Referenzen werden
  deaktiviert angezeigt statt stillschweigend geändert. Gespeicherte Skripte
  bleiben die Katalogquelle; ungespeicherte Skriptentwürfe sind nicht enthalten.

## Bereits beobachtete Einschränkung der Browser-Prüfung

- Beim Zinsbeispiel blieb der eingebettete PDF-Viewer im internen Browser weiß.
  Das heruntergeladene PDF enthielt die zehn Jahresberechnungen korrekt auf zwei
  Seiten und wurde visuell geprüft. Ursache der eingebetteten Anzeige ist noch
  nicht abschließend geklärt; nicht als Fehler der Berechnung einordnen.
- Die anschließende Browser-Verbindung hing beim Neuladen. Die Korrektur des
  unnötig ausgelösten Inhaltsänderungsereignisses in `DocumentPanel.vue` ist durch
  Regressionstests abgesichert; die abschließende Live-Bestätigung blieb aus.
