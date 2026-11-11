# Konzept: Serverseitige Ressourcenberechtigungen

Status: Implementiert. Die Regeln werden serverseitig in den Ressourcen-Services durchgesetzt.

## Ziel und Grundlage

Jeder API-Zugriff auf gespeicherte Inhalte wird anhand des angemeldeten Benutzers und des zugehörigen Projekts geprüft. Eine gefilterte Projektübersicht ersetzt diese Prüfung nicht: Auch direkte Aufrufe mit fremden Ressourcen-IDs müssen geschützt sein.

Benutzer-ID und Identität stammen ausschließlich aus der serverseitigen Session. Das bestehende Projektmodell mit Eigentümer und Mitgliedern bildet die Grundlage. Eigentümer und Mitglieder dürfen zunächst alle Projektinhalte lesen, erstellen, bearbeiten und löschen. Abgestufte Inhaltsrechte kommen später. Die im [Projektmanagement](PROJECT_MANAGEMENT.md) geplante Admin-Rolle erweitert die Einladungsrechte; die hier implementierten Inhaltsrechte bleiben gleich.

## Prüfungen

- **Projekte:** Beim Erstellen wird der angemeldete Benutzer Eigentümer. Listen und Einzelzugriffe berücksichtigen seine Projektberechtigung.
- **Skripte, Dokumentvorlagen und Render-Konfigurationen:** Vor Lesen, Ändern und Löschen wird die Ressource geladen und ihr gespeichertes Projekt geprüft. Beim Erstellen wird das angegebene Zielprojekt geprüft. Die Projektzuordnung ist über normale Inhalts-Updates nicht änderbar.
- **Verknüpfungen und Vorschauen:** Jede referenzierte Skript- und Render-Konfigurations-ID muss zum selben Projekt wie die Vorlage gehören. Projektübergreifende Verknüpfungen sind auch bei Zugriff auf beide Projekte verboten. Ungespeicherte Vorschauen erhalten dazu einen expliziten Projektkontext.
- **Print Jobs:** Jeder Auftrag erhält beim Anlegen eine eigene, unveränderliche `projectId`. Das Backend leitet sie aus der geprüften Dokumentvorlage ab. Alle Auftrags- und Dateizugriffe werden direkt über dieses Projekt autorisiert. Eine Ergebnisdatei muss zusätzlich zum angegebenen Auftrag gehören. Nach Löschen der Vorlage bleiben bestehende Aufträge und Ergebnisse ihrem Projekt zugeordnet; eine Ausführung ohne benötigte Vorlage wird mit einem verständlichen Fehler abgelehnt.
- **Freie Skriptausführung und Validierung:** Benötigen eine Anmeldung; bei rein mitgesendetem Quelltext ohne gespeicherte Referenzen ist keine zusätzliche Projektprüfung nötig.

## Umsetzung und Fehlerverhalten

Eine kleine gemeinsame Berechtigungsprüfung im Backend bündelt die Projektregeln. Die bestehenden Services wenden sie vor Ausgabe, Seiteneffekten und Dateioperationen an. Die Regeln gelten unabhängig vom Frontend.

Ohne Anmeldung antwortet die API mit `401`. Nicht vorhandene und für den Benutzer unsichtbare Ressourcen liefern einheitlich `404`, damit fremde IDs nicht bestätigt werden. Unzulässige Verknüpfungen werden ohne Offenlegung fremder Inhalte abgelehnt. Das bestehende JSON-Fehlerformat wird weiterverwendet. Abgestufte Schreibrechte und entsprechende `403`-Fälle werden erst mit späteren Rollen benötigt.

## Technischer Ablauf und Performance

Eine gemeinsame Methode wie `requireProjectAccess(projectId, userId)` prüft in MongoDB, ob der Benutzer Eigentümer oder Mitglied ist. Services rufen diese Prüfung vor ihrer fachlichen Aktion auf. Ein Zugriff über eine flache Ressourcen-ID benötigt typischerweise zwei Abfragen: Ressource mit Projektzuordnung laden und Projektzugriff prüfen. Eine vom Client gesendete Projekt-ID ersetzt nie die gespeicherte Zuordnung.

Projektlisten werden bereits in MongoDB nach Eigentümer/Mitgliedschaft gefiltert, statt wie bisher alle Projekte zu laden und in Java zu filtern. Inhalts- und Auftragslisten werden auf erlaubte Projekte eingeschränkt und bei wachsender Datenmenge paginiert. Referenzierte Skripte werden gesammelt geladen und auf dasselbe Projekt geprüft; innerhalb eines Requests wird die Projektberechtigung einmal geprüft, nicht für jede Referenz oder CSV-Zeile erneut.

Die üblichen `_id`-Indizes unterstützen Einzelzugriffe. Für Listen werden gezielte Indizes auf Projektzuordnungen sowie Eigentümer- und Mitgliedschaftsfeldern benötigt. Damit hängt die Prüfung nicht von einem vollständigen Scan aller Ressourcen ab. Zusätzliche Datenbank-Roundtrips bleiben messbarer Aufwand; konkrete Latenzen werden erst unter realer Last bestimmt.

Zunächst kein Cache über Requests hinweg: Änderungen an Eigentümer/Mitgliedschaft werden beim nächsten Request geprüft. Laufende Druckaufträge benötigen eine Prüfung beim Start, nicht pro Datensatz; spätere Downloads werden erneut autorisiert. Ein sofortiger Abbruch bereits laufender Aktionen bei Rechteentzug ist damit nicht zugesagt. Bei Bedarf später anhand von Messungen optimieren. Kein allgemeines ACL-Framework und keine Datenmigration für das Vorproduktionssystem.

`ProjectAccessService` bündelt die Prüfung, `DocumentReferenceService` löst Skripte gesammelt auf und erzwingt Projektgrenzen für Render-Konfiguration und Skripte. `ProjectIndexConfiguration` erstellt gezielt die Listen-Indizes; globale automatische Indexerstellung bleibt deaktiviert. Listen sind aktuell gefiltert, aber noch nicht paginiert.

Bestehende Print Jobs ohne `projectId` werden nicht migriert und geben keinen Zugriff frei. Bei neuen Aufträgen wird die Zuordnung ausschließlich aus der geprüften Vorlage übernommen. Der Hintergrund-Worker erhält die bereits geprüfte, aufgelöste Vorlage als Snapshot und benötigt keinen Session-Kontext.

Einladungen, Mitgliederverwaltung und zusätzliche Rollen gehören zum anschließenden Projektmanagement.

## Abschlusskriterium

Tests mit zwei Benutzern und getrennten Projekten belegen, dass fremde IDs weder Inhalte oder Dateien offenlegen noch Änderungen, Verknüpfungen oder Druckaufträge erlauben. Ergänzend werden erlaubte Eigentümer-/Mitgliederzugriffe sowie fehlende und gelöschte Referenzen geprüft. Abgelehnte Aktionen dürfen keine Seiteneffekte haben.

Die HTTP- und MongoDB-Integrationstests liegen in `ProjectAuthorizationTest`. Der Vorschau-Request im Frontend wird in `DocumentPanel.spec.ts` geprüft. Die OpenAPI-Typen in `src/types/api.ts` und `frontend/src/types/api.ts` sind synchronisiert.
