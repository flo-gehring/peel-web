# Gemeinsame Dateiverwaltung

Skripte, Renderkonfigurationen und Dokumentvorlagen liegen gemeinsam in MongoDB in
`project-files`. Eine Datei besitzt eine unveränderliche UUID und Projektzuordnung,
einen Namen, eine optionale Beschreibung (gespeichert als leerer String), eine
Änderungsnummer sowie Erstellungs- und Änderungszeitpunkt. Namen werden außen
getrimmt, dürfen nicht leer sein und sind pro Projekt und Dateityp eindeutig
(Groß-/Kleinschreibung wird unterschieden). Referenzen verwenden ausschließlich IDs.

`FileContent` ist ein sealed Interface mit Records für `script`, `render-config`
und `document`. Der Typ steht in API-Antworten unter `content.type`; es gibt keinen
zweiten veränderbaren Typ auf Dateiebene. Jackson verwendet explizite logische
Typnamen. Spring Data verwendet dafür unabhängig `_class` und `@TypeAlias` mit
denselben Namen. Alle Inhaltsklassen werden beim Start im Mapping-Kontext registriert.
Der Typ einer bestehenden Datei kann nicht geändert werden.

## API

Alle Verwaltungsaktionen liegen unter `/api/projects/{projectId}/files`:

| Methode / Pfad | Verhalten |
| --- | --- |
| `GET /files` | Metadaten ohne Inhalt; optional `?type=script`, `render-config` oder `document`. Der abgeleitete Typ steht in der Listenprojektion als `type`. |
| `GET /files/{fileId}` | Gemeinsame Metadaten und typisierter Inhalt. |
| `POST /files` | Erstellen mit vom Client erzeugter UUID, Name, Beschreibung und Inhalt. |
| `PUT /files/{fileId}` | Vollständiger Zielzustand aus Name, Beschreibung, Inhalt und `expectedVersion`. |
| `DELETE /files/{fileId}?expectedVersion=N` | Versionsgeschützte, wiederholbare Löschung. |

Eine Erstellung beginnt bei Version **0**. Erstellen und Aktualisieren sind strikt
getrennt. Existiert die ID bereits, liefert POST nur dann Erfolg, wenn die Datei
zum Zielprojekt gehört, noch Version 0 besitzt und ihr vollständiger Zustand der
Anfrage entspricht. Andernfalls folgt `409 file_already_exists`; Erstellen
überschreibt niemals. Ein Namenskonflikt ergibt `409 file_name_conflict`.
POST und PUT liefern bei Erfolg `200` mit der aktuellen Datei, DELETE `204`.

Updates vergleichen zunächst den vollständigen normalisierten Zielzustand mit dem
gespeicherten Zustand. Ist er identisch, wird die aktuelle Datei erfolgreich
zurückgegeben, auch bei abweichender erwarteter Version. Es gibt keinen
Schreibvorgang; Version und Änderungszeitpunkt bleiben unverändert. Maps werden
strukturell verglichen, Quelltext und andere Textfelder exakt.

Bei tatsächlichen Änderungen müssen gespeicherte Version und `expectedVersion`
übereinstimmen. MongoDB aktualisiert Zustand, Zeitstempel und Versionsnummer in
einem atomaren Compare-and-Set anhand von ID, Projekt und Version. Verliert ein
konkurrierender Schreiber dieses Rennen, wird erneut gelesen: Zielzustand bereits
vorhanden bedeutet Erfolg, ansonsten `409 file_version_conflict` mit
`error.details.currentVersion`. Es gibt kein Upsert und kein automatisches Rebase.
Die Versionsnummer wird explizit im atomaren Update verwaltet, nicht zusätzlich
durch Spring Datas automatische `@Version`-Erhöhung.

Das ermöglicht die Wiederholung nach einer verlorenen Antwort. Erfolg bestätigt
den aktuell vorhandenen Zielzustand, nicht die historische Ausführung einer
bestimmten Anfrage. Die Änderungsnummer ist noch keine Versionshistorie.

## Referenzen, Rechte und Löschung

Dokumentvorlagen referenzieren Skripte und eine Renderkonfiguration. Existenz,
passender Dateityp und Zugehörigkeit zum selben Projekt werden vor dem Speichern
und erneut vor Vorschau/Druck geprüft. Skripte dürfen als leere oder noch nicht
ausführbare Entwürfe gespeichert werden. Strukturierte Inhalte müssen ihr Schema
erfüllen; die Speichervalidierung ersetzt keine erfolgreiche Ausführung/Vorschau.

Löschen benötigt ebenfalls die erwartete Version. Eine zwischenzeitlich geänderte
Datei liefert einen Versionskonflikt. Eine bereits fehlende Datei gilt als gelöscht.
Bei Referenzen anderer Dateien folgt `409 file_in_use` mit
`error.details.usages` (Metadaten der blockierenden Dateien). Dokumentvorlagen
können gelöscht werden; bestehende Print Jobs und Ergebnisse behalten ihre eigene
Projektzuordnung. Ein noch nicht gestarteter Job ohne Vorlage kann nicht starten.

Projektberechtigung und Archivschutz gelten vor jeder Aktion, einschließlich
inhaltlich identischer Wiederholungen. Fremde/unsichtbare Projekte und Ressourcen
werden mit `404` verborgen; fehlende Anmeldung ergibt `401`. Bei einem aktiven
Projekt gibt DELETE für eine dort nicht sichtbare Datei ebenfalls `204`, ohne
deren Existenz in einem anderen Projekt zu bestätigen.

Die vorhandene Projektsperre koordiniert Referenzprüfung, Schreiben, Löschen und
Archivieren innerhalb **einer Backend-Instanz**. Das Dateiupdate selbst ist auch
gegen konkurrierende Schreiber in MongoDB geschützt. Projektzustand und
Referenzintegrität über mehrere Dokumente sind dagegen keine verteilte
Transaktion. Mehrere schreibende Instanzen benötigen dafür vorab eine gemeinsame
Transaktions-/Sperrstrategie.

## Frontend und fachliche Funktionen

Ein gemeinsamer Pinia-Store verwaltet gelesene Dateistände und die gemeinsame API.
Er speichert beim Update die ursprünglich bestätigte Versionsnummer. Fehler und
Netzwerkabbrüche ändern diese Basis nicht; Entwürfe bleiben erhalten. Parallele
initiale Ladevorgänge werden zusammengeführt. Erfolgreiche Antworten aktualisieren
die Basis, während währenddessen neu eingegebene Änderungen ungespeichert bleiben.

Der Explorer erstellt alle Typen, bearbeitet Namen/Beschreibung, löscht mit Version
und zeigt blockierende Verwendungen an. Eine fehlgeschlagene Erstellung kann im
Dialog mit exakt derselben UUID und Nutzlast wiederholt werden. „Reload file“
lädt nach ausdrücklicher Bestätigung den Serverstand und verwirft nur den lokalen
Entwurf dieser Datei. Explizites Logout/Accountwechsel leert auch die Dateibasis.

Skriptausführung und Validierung, Standard-Renderkonfiguration, Dokumentvorschau
und Print Jobs bleiben fachliche Endpunkte. Ihre bisherigen CRUD-Endpunkte sind
entfernt. Die Projektübersicht liefert weiterhin nach Typ gruppierte
Metadatenprojektionen, die ausschließlich auf der gemeinsamen Collection beruhen.

## Einheitlicher Editor

Alle Dateitypen verwenden dieselbe kompakte Aktionsleiste unter den Tabs:
Speicherstatus, Output, Dateieinstellungen, Save und eine typabhängige Hauptaktion.
Skripte haben Run, Dokumente Render preview, Renderkonfigurationen ausschließlich
Save. Fehler beim Speichern erscheinen direkt unter der Leiste. JSON-/Schemafehler
der Renderkonfiguration werden zusätzlich im Monaco-Modell markiert.

Das Zahnrad öffnet eine seitliche Einstellungsansicht. Name und Beschreibung
zählen als Entwurf und werden mit dem Inhalt zusammen gespeichert. Dokumente
öffnen Skriptverknüpfungen und Renderkonfiguration über eigene Banana-/Palette-Buttons
mit Tooltips in separaten seitlichen Ansichten. Die Hauptaktionen verwenden reine
Symbole: Play für Skripte, Eye für die Dokumentvorschau. Ein Punkt im
Tab kennzeichnet ungespeicherte Inhalte oder Metadaten; Navigation und Logout
berücksichtigen beide.

Dokumente wechseln zwischen Edit und Preview im selben Arbeitsbereich. Der
Texteditor bleibt dabei gemountet; Formatierungsleiste und Dokumentseite nehmen
im Bearbeitungsmodus die verfügbare Höhe ein. Output wird beim Wechsel auf einen
Dokumenttab geschlossen und kann über die Aktionsleiste wieder geöffnet werden.
Beim Wechsel auf ein Skript wird der Output-Bereich wiederhergestellt. Seine
Daten bleiben im Store erhalten. Es gibt keine separate PDF-Dockview-Seite mehr.

Ctrl/Cmd+S speichert die zuletzt aktive Editor-Datei, Ctrl/Cmd+Enter startet deren
Run beziehungsweise Vorschau, auch wenn Bindings oder Output fokussiert sind.
Run und Vorschau speichern nicht automatisch. Die Vorschau verwendet den aktuellen
Dokumententwurf; referenzierte Skripte und Renderkonfigurationen werden wie bisher
aus ihren gespeicherten Ständen aufgelöst.

PDF-Vorschauen sind nach Datei-ID getrennt und zeigen Namen und Erstellungszeit.
Änderungen am Dokumententwurf oder an den Bindings kennzeichnen eine vorhandene
Vorschau als veraltet. Später eintreffende Antworten dürfen nach Löschen/Neuladen
oder Workspace-Wechsel keine alte Vorschau wiederherstellen. Objekt-URLs werden
beim Ersetzen und Verwerfen freigegeben. Skriptergebnisse zeigen ebenfalls ihre
Quelle; Ausführen eines weiteren Skripts ist während eines laufenden Runs gesperrt.

## Performance und Umfang

Einzelzugriffe verwenden den `_id`-Index. Ein eindeutiger zusammengesetzter Index
auf Projekt, Inhaltsalias und Name sichert Namen und unterstützt Projekt-/Typlisten.
Listen laden nur Metadaten und den Alias, keine Quelltexte/Vorlagen.
Referenzauflösung lädt Skripte gesammelt. Der Löschschutz durchsucht derzeit die
Dateien des Projekts nach Dokumentreferenzen; eine zusätzliche Referenzablage oder
Pagination ist noch nicht implementiert und kann bei gemessenem Bedarf folgen.
Dateien unterliegen weiterhin MongoDBs Dokumentgrößenlimit; Binärdaten/PDFs werden
nicht in diesen Textinhalt eingebettet.

Keine Datenmigration, keine Kompatibilitätsendpunkte und keine Revisionshistorie.
Alte Collections werden nicht mehr gelesen oder bei normalen Dateiaktionen
verändert. Bestehende Entwicklungsinhalte müssen neu angelegt werden; alte Daten
werden durch diese Umstellung nicht automatisch gelöscht.

`ProjectFileTest` prüft Idempotenz, atomare konkurrierende Updates (auch mit
unabhängigen Projektsperren), Alias-Auflösung, API-Schema, Validierung,
Referenzen, Berechtigungen und Archivschutz. Die bisherigen Vorschau-, Druck- und
Projektlöschungstests verwenden die gemeinsame Datenhaltung. Frontend-Tests
prüfen Wiederholungen, Konflikte, Entwurfserhalt und die gemeinsame API.
