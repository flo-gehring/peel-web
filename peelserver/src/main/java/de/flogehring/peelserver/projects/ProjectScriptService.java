package de.flogehring.peelserver.projects;

import de.flogehring.peelserver.api.ScriptSummaryResponse;
import de.flogehring.peelserver.api.scripts.ProjectRunRequest;
import de.flogehring.peelserver.api.scripts.RunResponse;
import de.flogehring.peelserver.api.scripts.ScriptDtoResponse;
import de.flogehring.peelserver.api.scripts.ScriptSaveRequest;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import de.flogehring.peelserver.run.PeelExecutionService;
import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectScriptService {

    private static final String DEFAULT_SCRIPT_NAME = "Untitled script";

    private final ProjectSettings projectSettings;
    private final PeelExecutionService peelExecutionService;

    public List<ScriptSummaryResponse> listScripts(String projectId) {
        Path scriptsDir = scriptsDirectory(projectId);
        if (!Files.isDirectory(scriptsDir)) {
            return List.of();
        }

        try (Stream<Path> entries = Files.walk(scriptsDir)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".peel"))
                    .map(path -> toSummary(scriptsDir, path))
                    .sorted(Comparator.comparing(ScriptSummaryResponse::name, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(ScriptSummaryResponse::id))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list scripts for project: " + projectId, exception);
        }
    }

    public ScriptDtoResponse saveScript(String projectId, ScriptSaveRequest request) {
        String scriptContent = requireScript(request.script());
        Path scriptsDir = scriptsDirectory(projectId);
        String scriptId = normalizeScriptId(request.id(), request.name());
        Path scriptPath = resolveScriptPath(scriptsDir, scriptId);

        try {
            Path parent = scriptPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(scriptPath, scriptContent, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save script '" + scriptId + "' for project: " + projectId, exception);
        }

        String displayName = deriveDisplayName(scriptPath, request.name());
        return new ScriptDtoResponse(scriptId, displayName, scriptContent);
    }

    public ScriptDtoResponse getScript(String projectId, String rawScriptId) {
        Path scriptsDir = scriptsDirectory(projectId);
        String scriptId = normalizeExistingScriptId(rawScriptId);
        Path scriptPath = resolveScriptPath(scriptsDir, scriptId);
        if (!Files.isRegularFile(scriptPath)) {
            throw new ResourceNotFoundException("Script not found: " + scriptId);
        }

        try {
            String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
            return new ScriptDtoResponse(scriptId, deriveDisplayName(scriptPath, null), content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read script '" + scriptId + "' for project: " + projectId, exception);
        }
    }

    public void deleteScript(String projectId, String rawScriptId) {
        Path scriptsDir = scriptsDirectory(projectId);
        String scriptId = normalizeExistingScriptId(rawScriptId);
        Path scriptPath = resolveScriptPath(scriptsDir, scriptId);
        if (!Files.isRegularFile(scriptPath)) {
            throw new ResourceNotFoundException("Script not found: " + scriptId);
        }

        try {
            Files.delete(scriptPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete script '" + scriptId + "' for project: " + projectId, exception);
        }
    }

    public RunResponse runScript(String projectId, ProjectRunRequest request) {
        String scriptContent = resolveScriptContent(projectId, request);
        Map<String, Object> bindings = request.bindings() == null ? Map.of() : request.bindings();
        TraceProgram traceProgram = peelExecutionService.execute(scriptContent, bindings);

        return new RunResponse(
                TraceMapOutput.fromProgram(traceProgram),
                TraceMapOutput.fromValue(traceProgram.result())
        );
    }

    private ScriptSummaryResponse toSummary(Path scriptsDir, Path scriptPath) {
        String scriptId = scriptsDir.relativize(scriptPath).toString().replace('\\', '/');
        return new ScriptSummaryResponse(scriptId, deriveDisplayName(scriptPath, null));
    }

    private String resolveScriptContent(String projectId, ProjectRunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("run request is required");
        }
        if (request.script() != null && !request.script().isBlank()) {
            return request.script();
        }
        if (request.scriptId() == null || request.scriptId().isBlank()) {
            throw new IllegalArgumentException("Either script or scriptId must be provided");
        }
        return getScript(projectId, request.scriptId()).script();
    }

    private String requireScript(String script) {
        if (script == null) {
            throw new IllegalArgumentException("script is required");
        }
        return script;
    }

    private String normalizeScriptId(String requestedId, String requestedName) {
        if (requestedId != null && !requestedId.isBlank()) {
            return normalizeExistingScriptId(requestedId);
        }
        String fromName = requestedName == null ? DEFAULT_SCRIPT_NAME : requestedName.trim();
        if (fromName.isEmpty()) {
            fromName = DEFAULT_SCRIPT_NAME;
        }
        String slug = slugify(fromName);
        if (slug.length() < 1) {
            slug = "untitled-script";
        }
        return slug + ".peel";
    }

    private String normalizeExistingScriptId(String rawScriptId) {
        if (rawScriptId == null || rawScriptId.isBlank()) {
            throw new IllegalArgumentException("script id must not be blank");
        }
        String normalized = rawScriptId.replace('\\', '/').trim();
        if (normalized.startsWith("/")
                || normalized.contains("..")
                || normalized.contains(":")
                || normalized.contains("//")
                || normalized.contains("/")) {
            throw new IllegalArgumentException("invalid script id: " + rawScriptId);
        }
        return normalized;
    }

    private Path resolveScriptPath(Path scriptsDir, String scriptId) {
        Path scriptPath = scriptsDir.resolve(scriptId).normalize();
        if (!scriptPath.startsWith(scriptsDir)) {
            throw new IllegalArgumentException("invalid script id: " + scriptId);
        }
        return scriptPath;
    }

    private Path scriptsDirectory(String projectId) {
        String normalizedProjectId = requireProjectId(projectId);
        Path root = Paths.get(projectSettings.effectiveRoot()).toAbsolutePath().normalize();
        Path projectDirectory = root.resolve(normalizedProjectId).normalize();
        if (!projectDirectory.startsWith(root) || !Files.isDirectory(projectDirectory)) {
            throw new ResourceNotFoundException("Project not found: " + normalizedProjectId);
        }
        Path scriptsDir = projectDirectory.resolve("scripts").normalize();
        if (!scriptsDir.startsWith(projectDirectory)) {
            throw new IllegalArgumentException("invalid scripts directory for project: " + normalizedProjectId);
        }
        try {
            Files.createDirectories(scriptsDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare scripts directory for project: " + normalizedProjectId, exception);
        }
        return scriptsDir;
    }

    private String requireProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("project id must not be blank");
        }
        return projectId.trim();
    }

    private String deriveDisplayName(Path scriptPath, String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        String filename = Objects.requireNonNull(scriptPath.getFileName()).toString();
        if (filename.toLowerCase(Locale.ROOT).endsWith(".peel")) {
            return filename.substring(0, filename.length() - ".peel".length());
        }
        return filename;
    }

    private String slugify(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String slug = lower.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        if (slug.length() > 80) {
            return slug.substring(0, 80);
        }
        return slug;
    }
}
