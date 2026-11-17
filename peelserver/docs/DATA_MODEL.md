# Datenmodell

Die gemeinsame Dateiverwaltung ist in [FILE_MANAGEMENT.md](FILE_MANAGEMENT.md)
beschrieben. Skripte, Renderkonfigurationen und Dokumentvorlagen verwenden
`ProjectFile` und dieselbe MongoDB-Collection `project-files`.

```mermaid
classDiagram
    class ProjectFile {
        String id
        ProjectId projectId
        String name
        String description
        long version
        Instant createdAt
        Instant updatedAt
    }
    class FileContent {
        <<sealed interface>>
    }
    class ScriptContent {
        String source
    }
    class RenderConfigContent {
        Map renderConfigurations
        Map namedOverrides
    }
    class DocumentContent {
        Map scriptNameTags
        String templateHtml
        String editorStateJson
        String renderConfigurationId
    }
    ProjectFile *-- FileContent
    FileContent <|.. ScriptContent
    FileContent <|.. RenderConfigContent
    FileContent <|.. DocumentContent
    DocumentContent --> ProjectFile : IDs anderer Dateien im selben Projekt
```

Dateien werden für Ausführung und Rendering in fachliche Objekte aufgelöst.
Ein `Document` enthält die referenzierten `PeelScript`-Werte und eine
`ExpressionRenderConfiguration`. Diese aufgelösten Objekte besitzen keine eigene
CRUD-Verwaltung oder Collection. Ein Print-Job-Worker erhält den aufgelösten Stand
beim Start; eine persistente historische Dateiversion ist damit noch nicht eingeführt.

Print Jobs besitzen eine eigene unveränderliche Projektzuordnung. CSVs und PDFs
liegen weiterhin in MinIO. Benutzer, Projektmitgliedschaften, Einladungen und
Projektzustände bleiben eigenständige Datenmodelle.
