package de.flogehring.peelserver.api.data.print.jobs;

import de.flogehring.peelserver.impl.project.ProjectState;
import org.jspecify.annotations.Nullable;

public record ProjectPrintJobDetails(PrintJobSummary job, String projectName, ProjectState projectState,
                                    @Nullable String currentDocumentName) {}
