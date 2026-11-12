# Druckaufträge im Projekt

Druckaufträge sind ein eigener Bereich jedes Projekts. Der Workspace und die
Auftragsübersicht sind über die Projektnavigation erreichbar; Projektkarten
bieten ebenfalls einen direkten Einstieg. Die bisherigen globalen Listen- und
Erstellen-Endpunkte sowie die globale Frontend-Ansicht sind entfernt.

## Gruppierung und Identität

Die Übersicht zeigt alle vorhandenen Dokumentvorlagen des Projekts als
einklappbare Gruppen, einschließlich Vorlagen ohne Aufträge. Jede Gruppe bietet
„Neuer Druckauftrag“. Aufträge stehen mit Name, Erstellungszeit, Eingabedatei,
Status und Ergebnisanzahl unter ihrer Vorlage, neueste zuerst.

Aufträge gelöschter Vorlagen stehen weiter unten im Bereich „Gelöschte
Dokumentvorlagen“, weiterhin nach ursprünglicher Dokument-ID gruppiert. Es gibt
dort keinen Erstellen-Button. Eine neu angelegte gleichnamige Vorlage bekommt
eine eigene Gruppe, weil ausschließlich IDs die Identität bestimmen.

Jeder Auftrag speichert unveränderlich seine Projekt-ID, Dokument-ID, den
Vorlagennamen beim Anlegen und den Erstellungszeitpunkt. Vorhandene Gruppen zeigen
den aktuellen Vorlagennamen. Gelöschte Gruppen verwenden den neuesten verfügbaren
historischen Namen ihrer Aufträge. Die Detailansicht zeigt zusätzlich den Namen
beim Anlegen. Status-/Dateiänderungen erhalten diese Metadaten.

Ältere Aufträge können Datum und historischen Namen noch nicht besitzen. Sie
bleiben lesbar: fehlende Datumswerte werden zuletzt sortiert und als „Zeitpunkt
nicht erfasst“ angezeigt. Fehlt bei einer gelöschten Gruppe jeder historische
Name, lautet ihre Überschrift „Gelöschte Dokumentvorlage“. Es werden keine Daten
erraten oder beim Lesen ergänzt; eine Datenmigration ist nicht enthalten.

## Anlegen und Ausführen

Der Dokumenteditor hat einen eigenen Printer-Button „Create print job“. Dieser
und der Erstellen-Button einer Gruppe verwenden dieselbe Maske: Vorlage ist
bereits gesetzt, der Benutzer vergibt einen Auftragsnamen. Leere Namen werden
serverseitig abgewiesen. Anschließend führt die Navigation zum Auftrag, wo CSV
hochgeladen und die Verarbeitung ausdrücklich gestartet wird.

Bei ungespeicherten Dokumentinhalten oder Metadaten lautet die Aktion
„Speichern und Auftrag anlegen“. Das Speichern muss erfolgreich sein, bevor die
Erstellen-Anfrage gesendet wird. Konflikte, Verbindungsfehler oder weitere
Änderungen während des Speicherns verhindern das Anlegen und erhalten den Entwurf.
Andere offene Entwürfe bleiben durch den bisherigen Navigationsschutz geschützt.

Die Zuordnung zu Projekt und Vorlage kann nach dem Anlegen nicht gewechselt
werden. Beim Start werden wie bisher der dann gespeicherte Vorlagenstand und die
referenzierten Skripte/Renderkonfigurationen geprüft und für den Worker aufgelöst.
Die Maske erklärt dieses Verhalten. Eine unveränderliche historische
Inhaltsversion beim Anlegen ist damit noch nicht eingeführt.

## Rechte und fehlende Vorlagen

Die Projektberechtigung wird bei Liste, Einzelansicht und Erstellung geprüft.
Die ausgewählte Vorlage muss ein Dokument im angegebenen Projekt sein, auch wenn
der Benutzer zusätzlich Zugriff auf andere Projekte hat. Projektzuordnung wird
serverseitig geprüft und nicht aus ungesicherten Clientangaben übernommen.

Nach Löschen einer Vorlage bleiben Aufträge sowie CSV-/PDF-Downloads über deren
eigene Projektzuordnung zugänglich. Noch nicht gestartete Aufträge ohne Vorlage
können nicht starten. Bereits laufende Aufträge dürfen mit ihrem beim Start
aufgelösten Stand fertig werden. Die Detailansicht weist auf die gelöschte Vorlage
hin und bietet keinen Start an.

Archivierte Projekte bleiben lesbar; neue Aufträge, Uploads und Starts sind
gesperrt. Das Backend erzwingt diese Regeln unabhängig von der Oberfläche.
Downloads sind weiterhin erlaubt. Die bestehende Projektlöschung entfernt auch
alle Druckaufträge und Dateien des Projekts.

## API und Navigation

| Endpunkt | Verhalten |
| --- | --- |
| `GET /api/projects/{projectId}/print-jobs` | Projektname/-status, vorhandene Dokumentgruppen und Gruppen gelöschter Dokumente. |
| `POST /api/projects/{projectId}/print-jobs` | Auftrag anhand von `name` und `documentId: { id }` anlegen. |
| `GET /api/projects/{projectId}/print-jobs/{id}` | Einzelauftrag, Projektstatus und aktueller Vorlagenname, falls vorhanden. |
| `/api/print-jobs/{id}/data`, `/run`, `/file/{fileId}` | Bestehende Upload-, Start- und Download-Aktionen mit Autorisierung über den gespeicherten Auftrag. |

Frontend-Routen: `/projects/{projectId}/print-jobs` und
`/projects/{projectId}/print-jobs/{id}`. Login-Rücksprünge berücksichtigen beide.
Die Detailansicht lädt ausschließlich den ausgewählten Auftrag und sein Projekt.
Der Status kann in Liste und Detailansicht ausdrücklich aktualisiert werden.

Die Gruppierung lädt Projektaufträge über den Projektindex und Vorlagen als
Metadaten ohne Inhalte. Es gibt keine Abfrage pro Auftragszeile und keine Abfragen
über alle zugänglichen Projekte. Pagination und eine globale Gesamtübersicht sind
nicht Bestandteil dieser Umsetzung. Die bestehende Koordination über die
Projektsperre gilt weiterhin für eine Backend-Instanz.

`ProjectPrintJobsTest` prüft Gruppierung, leere/gelöschte Vorlagen, neue gleichnamige
Vorlagen, historische Metadaten, fehlende Alt-Metadaten, Berechtigungen und
Archivschutz. Frontend-Tests prüfen Projektgruppen, die gemeinsame Erstellen-Maske,
Speichern vor dem Anlegen, Fehlerfälle und Detailansichten gelöschter/archivierter
Vorlagen.
