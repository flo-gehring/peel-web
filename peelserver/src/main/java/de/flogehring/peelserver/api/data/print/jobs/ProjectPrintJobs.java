package de.flogehring.peelserver.api.data.print.jobs;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.impl.project.ProjectState;
import java.util.List;

public record ProjectPrintJobs(ProjectIdApi projectId, String projectName, ProjectState state,
                               List<DocumentJobs> documents, List<DocumentJobs> deletedDocuments) {
    public record DocumentJobs(String documentId, String name, List<PrintJobSummary> jobs) {}
}
