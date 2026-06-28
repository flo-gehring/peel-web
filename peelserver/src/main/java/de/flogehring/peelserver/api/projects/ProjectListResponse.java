package de.flogehring.peelserver.api.projects;

import java.util.List;

public record ProjectListResponse(List<ProjectSummaryResponse> projects) {
}
