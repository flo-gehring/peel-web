# Konzept: Projektmanagement

Status: Implementiert. Die Verwaltungsseite ist über „Manage“ in der Projektübersicht und „Project settings“ im Arbeitsbereich erreichbar.

## Oberfläche und Rechte

Eine eigene Projektverwaltungsseite ist aus der Projektübersicht und dem Arbeitsbereich erreichbar. Sie bündelt:

- **Allgemein:** Name und Beschreibung bearbeiten. Eigentümer und Mitglieder dürfen beides bei aktiven Projekten ändern.
- **Mitglieder:** Mitglieder und Rollen anzeigen; Eigentümer und Admins können weitere Mitglieder einladen. Der Eigentümer kann Mitglieder entfernen sowie Admins ernennen. Mitglieder können selbst austreten.
- **Projektstatus:** Archivieren, Wiederherstellen und endgültiges Löschen. Diese Aktionen bleiben dem Eigentümer vorbehalten.

Bei aktiven Projekten haben Eigentümer und Mitglieder volle Inhaltsrechte, wie in [AUTHORIZATION.md](AUTHORIZATION.md) beschrieben. Projektübergreifende Verknüpfungen bleiben ausgeschlossen.

## Eigentümer, Admins und Mitglieder

Admins sind Projektmitglieder mit zusätzlichen Einladungsrechten. Sie dürfen zunächst keine weiteren Admins ernennen. Die Rolle erweitert die Projektverwaltung, nicht die Inhaltsrechte.

| Aktion | Eigentümer | Admin | Mitglied |
| --- | --- | --- | --- |
| Inhalte sowie Name und Beschreibung bearbeiten (aktives Projekt) | Ja | Ja | Ja |
| Weitere Mitglieder per E-Mail einladen | Ja | Ja | Nein |
| Mitglieder entfernen | Ja | Nein | Nein |
| Admins ernennen | Ja | Nein | Nein |
| Eigentümerschaft übertragen | Ja | Nein | Nein |
| Archivieren, Wiederherstellen, endgültig Löschen | Ja | Nein | Nein |

## Einladungen

1. Eigentümer oder Admin gibt die E-Mail-Adresse des Empfängers ein.
2. An diese Adresse wird ein Einladungslink gesendet, unabhängig davon, ob bereits ein PEEL-Konto existiert. Der Einladende erhält weder in der Oberfläche noch über die API eine Auskunft über den bisherigen Registrierungsstatus.
3. Der Empfänger öffnet den Link und meldet sich mit der eingeladenen E-Mail-Adresse an. Falls noch kein Konto existiert, kann er sich im selben Ablauf über den bestehenden Magic-Link-Login registrieren.
4. Nach der Anmeldung muss der Empfänger die Projekteinladung ausdrücklich annehmen. Das Öffnen des Links, die Registrierung oder die Anmeldung allein erzeugt keine Mitgliedschaft.
5. Erst nach erfolgreicher Annahme wird der Empfänger als normales Mitglied aufgenommen. Eine Einladung ernennt keinen Admin.

Die Einladung ist an das Projekt und die eingeladene E-Mail-Adresse gebunden. Eine Anmeldung mit einer anderen Adresse erlaubt keine Annahme. Versandbestätigung und Fehlerverhalten dürfen nicht zwischen bereits registrierten und neuen Empfängern unterscheiden; technische Versandfehler dürfen als solche angezeigt werden.

Die Startseite zeigt offene, noch gültige Einladungen an die angemeldete E-Mail-Adresse mit Projektname, E-Mail-Adresse des Einladenden und Ablaufdatum. Der Empfänger kann sie dort ausdrücklich annehmen oder ablehnen; nach Annahme wird die Projektliste aktualisiert. Einladungen zu archivierten oder gelöschten Projekten sowie Einladungen ohne fortbestehende Einladungsrechte des Absenders werden nicht angezeigt. Nach Wiederherstellung eines Projekts erscheint eine noch gültige Einladung wieder.

`GET /api/invitations/received` sucht über einen Index auf E-Mail-Adresse und Ablaufdatum und lädt Projekte und Einladende gesammelt. `POST /api/invitations/received/{id}/accept` und `/decline` prüfen Empfänger und Gültigkeit erneut und verwenden dieselbe Verarbeitung wie der E-Mail-Link. Die API gibt weder den Link-Token noch dessen Hash aus.

## Eigentümerschaft übertragen

Nur der aktuelle Eigentümer darf die Eigentümerschaft an einen anderen Benutzer übertragen. Vor der Übertragung muss er beide Bestätigungen eingeben:

- die E-Mail-Adresse des neuen Eigentümers;
- `TRANSFER <Projektname>` mit dem tatsächlichen Projektnamen.

Beide Eingaben und die Berechtigung werden serverseitig geprüft. Der Empfänger muss bereits Mitglied sein und erhält eine E-Mail mit einem Link zur Verwaltungsseite. Die Übertragung wird erst durch seine ausdrückliche Annahme wirksam. Er kann sie auch ablehnen; der bisherige Eigentümer kann die Anfrage zurückziehen. Bis dahin bleiben beide Rollen unverändert.

Bei Annahme werden Eigentümer, Mitgliedschaft und Admin-Rolle in einem atomaren MongoDB-Update geändert: Der Empfänger wird Eigentümer, der bisherige Eigentümer wird Mitglied und Admin. Das Backend prüft erneut den bisherigen Eigentümer, den konkreten Auftrag und die fortbestehende Mitgliedschaft des Empfängers. Pro Projekt gibt es höchstens eine offene Anfrage. Eine neue Anfrage erfordert das Zurückziehen der alten; abgelaufene Anfragen können ersetzt werden.

## Lebenszyklus

| Zustand | Verhalten |
| --- | --- |
| Aktiv | Projekt und Inhalte bearbeiten, Skripte ausführen und Druckaufträge starten. |
| Archiviert | Inhalte und Ergebnisse ansehen und herunterladen. Bearbeitung und neue Ausführungen sind gesperrt. Wiederherstellen ist möglich. |
| Gelöscht | Projekt und zugehörige Daten sind endgültig entfernt; es bleibt kein wiederherstellbarer Projektzustand bestehen. |

Archivierte Projekte sind über eine eigene Ansicht oder einen Filter erreichbar. Wiederherstellen versetzt ein Projekt zurück in den aktiven Zustand.

Laufende Druckaufträge dürfen nach dem Archivieren fertig werden. Solange ein Auftrag läuft, ist endgültiges Löschen gesperrt. Die Prüfung muss serverseitig erfolgen; archivierte Projekte dürfen auch über direkte API-Aufrufe keine neuen Ausführungen starten.

## Endgültiges Löschen

Löschen ist nur für archivierte Projekte möglich. Die Oberfläche verlangt die Eingabe des Projektnamens zur Bestätigung und erklärt den endgültigen Datenverlust.

Entfernt werden das Projekt, seine Skripte, Dokumentvorlagen, Render-Konfigurationen, Druckaufträge und zugehörigen Einladungen sowie hochgeladene CSV- und erzeugte PDF-Dateien in MinIO. Archivieren allein löscht keine Inhalte oder Ergebnisse.

Die Löschung darf erst als erfolgreich gemeldet werden, wenn die zugehörigen Datenbankeinträge und Dateien entfernt sind. Ein Hintergrundauftrag darf nach erfolgreicher Löschung keine Daten oder Dateien neu erzeugen.

## Einladungs- und Übertragungsfristen

Einladungen und Übertragungsanfragen gelten jeweils sieben Tage. Die Fristen sind über `peel.projects.invitation-validity` und `peel.projects.transfer-validity` konfigurierbar. Eigentümer und Admins können Einladungen erneut senden und widerrufen. Ein erneuter Versand macht den vorherigen Link erst nach erfolgreichem Versand ungültig. Pro Adresse und Projekt ist zwischen zwei Sendungen mindestens eine Minute Abstand erforderlich.

Einladungstokens bestehen aus 32 zufälligen Bytes; gespeichert wird ausschließlich ihr SHA-256-Hash. Die Empfängeransicht erfordert eine Anmeldung mit der eingeladenen E-Mail-Adresse. Der vorhandene Magic-Link-Login führt nach Anmeldung oder Registrierung zur Einladung zurück. GET-Aufrufe nehmen niemals Einladungen an. Eine bestehende Mitgliedschaft wird bei Annahme nicht dupliziert; auch bei bereits registrierten Empfängern bleibt die Versandantwort gleich.

Der Eigentümer kann Admin-Rechte wieder entziehen. Dabei werden noch offene, von diesem Admin erstellte Einladungen widerrufen. Entfernen oder Austreten eines Mitglieds zieht dessen offene Einladungen sowie eine an dieses Mitglied gerichtete Eigentumsübertragung zurück. Beim Austritt des Eigentümers wird zunächst eine Übertragung verlangt.

## Technischer Ablauf und Prüfungen

Die Endpunkte liegen unter `/api/projects/{id}/settings`, `/members`, `/invitations` und `/transfer`; Empfänger verwenden `/api/invitations/{token}`. Archivierung und Wiederherstellung sind eigene POST-Aktionen. Die endgültige Löschung verwendet DELETE mit bestätigtem Projektnamen.

Änderungen, Ausführungsstarts und Lebenszyklusaktionen werden innerhalb der aktuellen einzelnen Backend-Instanz über dieselbe Projekt-Sperre koordiniert. Ein Druckauftrag erhält den Status `CALCULATING` bereits vor der asynchronen Übergabe. Dadurch kann eine Löschung keinen gerade gestarteten Auftrag übersehen. Bereits laufende Aufträge dürfen nach dem Archivieren fertig werden. Alle neuen Änderungen, Uploads, Vorschauen und Ausführungen sind serverseitig gesperrt. Freie Skriptausführungen enthalten dafür ebenfalls den Projektkontext.

Die Löschung setzt zunächst den dauerhaften Zustand `DELETING`. Danach werden CSVs, PDFs und auch nicht mehr im Ergebnisverzeichnis verzeichnete PDF-Teilergebnisse der Projektaufträge aus MinIO entfernt. Erst danach werden Inhalte, Aufträge, Einladungen und zuletzt das Projekt aus MongoDB gelöscht. Bei Fehlern bleibt die Zuordnung für einen erneuten Löschversuch erhalten. Die Verwaltungsseite zeigt dem Eigentümer dafür „Retry permanent deletion“ an; andere Zugriffe auf das Projekt sind gesperrt.

Die Koordination der laufenden Operationen gilt für **eine Backend-Instanz**. Ein Betrieb mit mehreren gleichzeitig schreibenden Backend-Instanzen benötigt vorher eine gemeinsame Sperr-/Transaktionsstrategie. Einladungen, Zustände und Übertragungsanfragen selbst sind persistent; ein Serverneustart verliert sie nicht. Für das Vorproduktionssystem ist keine Datenmigration enthalten.

`ProjectManagementTest` prüft Rechte, Einladungen, Ablauf/Widerruf, Eigentümerwechsel und wiederholbare Löschung. `ProjectDeletionStorageTest` prüft die tatsächliche Dateilöschung im isolierten Testcontainer und erhält Dateien anderer Aufträge. Frontend-Tests prüfen explizite Annahme, Rollen und Bestätigungseingaben. Die OpenAPI-Typen beider Arbeitsverzeichnisse sind synchronisiert.
