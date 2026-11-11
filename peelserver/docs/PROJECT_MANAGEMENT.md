# Konzept: Projektmanagement

Status: Fachlich abgestimmt; Grundlage für die Umsetzung von Ticket #3.

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

## Eigentümerschaft übertragen

Nur der aktuelle Eigentümer darf die Eigentümerschaft an einen anderen Benutzer übertragen. Vor der Übertragung muss er beide Bestätigungen eingeben:

- die E-Mail-Adresse des neuen Eigentümers;
- `TRANSFER <Projektname>` mit dem tatsächlichen Projektnamen.

Beide Eingaben und die Berechtigung werden serverseitig geprüft. Der Wechsel muss atomar erfolgen, sodass das Projekt genau einen Eigentümer behält. Ob das Ziel bereits Projektmitglied sein muss und welche Rolle der bisherige Eigentümer anschließend erhält, ist noch festzulegen.

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

## Noch technisch auszuarbeiten

- Sicherer Ablauf bei Fehlern während der Löschung über MongoDB und MinIO hinweg sowie bei gleichzeitig eintreffenden Start-, Archivierungs- und Löschanfragen.
- Gültigkeitsdauer, erneutes Senden und Widerruf von Einladungen sowie Behandlung mehrfacher Einladungen und bereits bestehender Mitgliedschaft. Der Empfängerablauf und die Vertraulichkeit des Registrierungsstatus sind oben festgelegt.
- Eigentümerübertragung: Muss das Ziel bereits Projektmitglied sein? Welche Rolle erhält der bisherige Eigentümer? Wie werden Admin-Rechte wieder entzogen?
- Konkrete API-Verträge und Gestaltung der Verwaltungsseite.

Dies sind offene Umsetzungsdetails; die oben festgelegten Rechte und Lebenszyklusregeln bleiben die fachliche Grundlage.
