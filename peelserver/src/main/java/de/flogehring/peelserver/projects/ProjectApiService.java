package de.flogehring.peelserver.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flogehring.peelserver.ProjectController;
import de.flogehring.peelserver.api.projects.CreateProjectRequest;
import de.flogehring.peelserver.api.projects.IdeOpenUrlResponse;
import de.flogehring.peelserver.api.projects.PeelProjectInfo;
import de.flogehring.peelserver.api.projects.ProjectCounts;
import de.flogehring.peelserver.api.projects.ProjectDetailResponse;
import de.flogehring.peelserver.api.projects.ProjectHealth;
import de.flogehring.peelserver.api.projects.ProjectListResponse;
import de.flogehring.peelserver.api.projects.ProjectPaths;
import de.flogehring.peelserver.api.projects.ProjectSummaryResponse;
import de.flogehring.peelserver.api.ScriptSummaryResponse;
import de.flogehring.peelserver.api.scripts.ProjectRunRequest;
import de.flogehring.peelserver.api.scripts.RunResponse;
import de.flogehring.peelserver.api.scripts.ScriptDtoResponse;
import de.flogehring.peelserver.api.scripts.ScriptSaveRequest;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class ProjectApiService implements ProjectController {

    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,62}$");
    private static final Set<String> ALLOWED_SORTS = Set.of("updatedAt", "name");

    private static final String SCRIPTS_DIR = "scripts";
    private static final String DOCUMENTS_DIR = "documents";
    private static final String RENDER_CONFIGS_DIR = "render-configs";
    private static final String FIXTURES_DIR = "fixtures";
    private static final String OUTPUT_DIR = "out";
    private static final String PEEL_PROJECT_FILE_NAME = "peel-project.json";

    private final ProjectSettings projectSettings;
    private final ProjectScriptService projectScriptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProjectListResponse listProjects(String q, String sort) {
        String normalizedQuery = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String effectiveSort = normalizeSort(sort);
        Comparator<ProjectSummaryResponse> comparator = comparatorForSort(effectiveSort);

        List<ProjectSummaryResponse> projects = readProjectSnapshots().stream()
                .map(this::toSummaryResponse)
                .filter(project -> matchesQuery(project, normalizedQuery))
                .sorted(comparator)
                .toList();

        return new ProjectListResponse(projects);
    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse createProject(CreateProjectRequest request) {
        String name = requireName(request.name());
        String description = normalizeDescription(request.description());
        String projectId = resolveProjectId(request.id(), name);
        String template = resolveTemplate(request.template());

        Path projectDirectory = resolveProjectDirectory(projectId);
        if (Files.exists(projectDirectory)) {
            throw new ProjectAlreadyExistsException(projectId);
        }

        try {
            Files.createDirectories(projectDirectory);
            createProjectDirectories(projectDirectory);
            createTemplateFiles(projectDirectory, template);
            writePeelProjectFile(projectDirectory, projectId, name, description);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create project '" + projectId + "'.", exception);
        }

        return toDetailResponse(readProjectSnapshot(projectDirectory));
    }

    @Override
    public ProjectDetailResponse getProject(String projectId) {
        Path projectDirectory = resolveExistingProjectDirectory(projectId);
        return toDetailResponse(readProjectSnapshot(projectDirectory));
    }

    @Override
    public IdeOpenUrlResponse getIdeOpenUrl(String projectId) {
        Path projectDirectory = resolveExistingProjectDirectory(projectId);
        String encodedWorkspace = URLEncoder.encode(projectDirectory.toUri().toString(), StandardCharsets.UTF_8);
        String theiaBaseUrl = trimTrailingSlash(projectSettings.effectiveTheiaBaseUrl());
        return new IdeOpenUrlResponse(theiaBaseUrl + "/?workspace=" + encodedWorkspace);
    }

    @Override
    public List<ScriptSummaryResponse> listProjectScripts(String projectId) {
        return projectScriptService.listScripts(projectId);
    }

    @Override
    public ScriptDtoResponse saveProjectScript(String projectId, ScriptSaveRequest request) {
        return projectScriptService.saveScript(projectId, request);
    }

    @Override
    public ScriptDtoResponse getProjectScript(String projectId, String scriptId) {
        return projectScriptService.getScript(projectId, scriptId);
    }

    @Override
    public void deleteProjectScript(String projectId, String scriptId) {
        projectScriptService.deleteScript(projectId, scriptId);
    }

    @Override
    public RunResponse runProjectScript(String projectId, ProjectRunRequest request) {
        return projectScriptService.runScript(projectId, request);
    }

    private List<ProjectSnapshot> readProjectSnapshots() {
        Path root = projectRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        try (Stream<Path> entries = Files.list(root)) {
            return entries
                    .filter(Files::isDirectory)
                    .map(this::tryReadProjectSnapshot)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list projects.", exception);
        }
    }

    private Optional<ProjectSnapshot> tryReadProjectSnapshot(Path projectDirectory) {
        Path peelProjectFile = projectDirectory.resolve(PEEL_PROJECT_FILE_NAME);
        if (!Files.isRegularFile(peelProjectFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(readProjectSnapshot(projectDirectory));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private ProjectSnapshot readProjectSnapshot(Path projectDirectory) {
        Path peelProjectFile = projectDirectory.resolve(PEEL_PROJECT_FILE_NAME);
        if (!Files.isRegularFile(peelProjectFile)) {
            throw new ResourceNotFoundException("Project metadata not found for project: " + projectDirectory.getFileName());
        }

        try {
            JsonNode rootNode = objectMapper.readTree(Files.readString(peelProjectFile, StandardCharsets.UTF_8));
            String id = rootNode.path("id").asText(projectDirectory.getFileName().toString());
            String name = rootNode.path("name").asText(id);
            String description = textOrNull(rootNode.path("description").asText(null));
            int formatVersion = rootNode.path("formatVersion").asInt(1);

            JsonNode pathsNode = rootNode.path("paths");
            String scriptsDir = textOrDefault(pathsNode.path("scriptsDir").asText(null), SCRIPTS_DIR);
            String documentsDir = textOrDefault(pathsNode.path("documentsDir").asText(null), DOCUMENTS_DIR);
            String renderConfigsDir = textOrDefault(pathsNode.path("renderConfigsDir").asText(null), RENDER_CONFIGS_DIR);
            String fixturesDir = textOrDefault(pathsNode.path("fixturesDir").asText(null), FIXTURES_DIR);
            String outputDir = textOrDefault(pathsNode.path("outputDir").asText(null), OUTPUT_DIR);

            Instant createdAt = readCreatedAt(projectDirectory);
            Instant updatedAt = readUpdatedAt(projectDirectory);

            ProjectCounts counts = new ProjectCounts(
                    countRegularFiles(projectDirectory.resolve(scriptsDir)),
                    countRegularFiles(projectDirectory.resolve(documentsDir)),
                    countRegularFiles(projectDirectory.resolve(renderConfigsDir))
            );

            List<String> issues = new ArrayList<>();
            appendMissingDirectoryIssue(projectDirectory, scriptsDir, issues);
            appendMissingDirectoryIssue(projectDirectory, documentsDir, issues);
            appendMissingDirectoryIssue(projectDirectory, renderConfigsDir, issues);
            appendMissingDirectoryIssue(projectDirectory, fixturesDir, issues);
            if (formatVersion < 1) {
                issues.add("formatVersion must be >= 1");
            }

            ProjectHealth health = new ProjectHealth(issues.isEmpty() ? "ok" : "warning", List.copyOf(issues));

            return new ProjectSnapshot(
                    id,
                    name,
                    description,
                    createdAt,
                    updatedAt,
                    formatVersion,
                    scriptsDir,
                    documentsDir,
                    renderConfigsDir,
                    fixturesDir,
                    outputDir,
                    counts,
                    health
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read project metadata for project: " + projectDirectory.getFileName(), exception);
        }
    }

    private void appendMissingDirectoryIssue(Path projectDirectory, String directoryName, List<String> issues) {
        if (!Files.isDirectory(projectDirectory.resolve(directoryName))) {
            issues.add("missing directory: " + directoryName);
        }
    }

    private ProjectSummaryResponse toSummaryResponse(ProjectSnapshot snapshot) {
        return new ProjectSummaryResponse(
                snapshot.id(),
                snapshot.name(),
                snapshot.description(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.counts(),
                snapshot.health()
        );
    }

    private ProjectDetailResponse toDetailResponse(ProjectSnapshot snapshot) {
        ProjectPaths paths = new ProjectPaths(
                normalizePath(projectRoot().resolve(snapshot.id())),
                snapshot.scriptsDir(),
                snapshot.documentsDir(),
                snapshot.renderConfigsDir(),
                snapshot.fixturesDir(),
                snapshot.outputDir()
        );

        return new ProjectDetailResponse(
                snapshot.id(),
                snapshot.name(),
                snapshot.description(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                paths,
                new PeelProjectInfo(snapshot.formatVersion()),
                snapshot.counts(),
                snapshot.health()
        );
    }

    private Comparator<ProjectSummaryResponse> comparatorForSort(String sort) {
        if ("name".equals(sort)) {
            return Comparator.comparing(ProjectSummaryResponse::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProjectSummaryResponse::id);
        }
        return Comparator.comparing(ProjectSummaryResponse::updatedAt)
                .reversed()
                .thenComparing(ProjectSummaryResponse::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProjectSummaryResponse::id);
    }

    private boolean matchesQuery(ProjectSummaryResponse project, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        return project.id().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || project.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || (project.description() != null
                && project.description().toLowerCase(Locale.ROOT).contains(normalizedQuery));
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "updatedAt";
        }
        String normalizedSort = sort.trim();
        if (!ALLOWED_SORTS.contains(normalizedSort)) {
            throw new IllegalArgumentException("sort must be one of: updatedAt, name");
        }
        return normalizedSort;
    }

    private void createProjectDirectories(Path projectDirectory) throws IOException {
        Files.createDirectories(projectDirectory.resolve(SCRIPTS_DIR));
        Files.createDirectories(projectDirectory.resolve(DOCUMENTS_DIR));
        Files.createDirectories(projectDirectory.resolve(RENDER_CONFIGS_DIR));
        Files.createDirectories(projectDirectory.resolve(FIXTURES_DIR));
        Files.createDirectories(projectDirectory.resolve(OUTPUT_DIR));
    }

    private void createTemplateFiles(Path projectDirectory, String template) throws IOException {
        String scriptContent;
        String bindingsContent;
        String documentContent;
        if ("demo".equals(template)) {
            scriptContent = "gross = amount + amount * taxRate\n";
            bindingsContent = "{\n  \"amount\": 1000,\n  \"taxRate\": 0.19\n}\n";
            documentContent = "Gross amount: {{ gross }}";
        } else {
            scriptContent = "";
            bindingsContent = "{}\n";
            documentContent = "";
        }

        Files.writeString(projectDirectory.resolve(SCRIPTS_DIR).resolve("main.peel"), scriptContent, StandardCharsets.UTF_8);
        Files.writeString(projectDirectory.resolve(DOCUMENTS_DIR).resolve("main.pebble"), documentContent, StandardCharsets.UTF_8);
        Files.writeString(projectDirectory.resolve(RENDER_CONFIGS_DIR).resolve("default.json"), "{}\n", StandardCharsets.UTF_8);
        Files.writeString(projectDirectory.resolve(FIXTURES_DIR).resolve("default-bindings.json"), bindingsContent, StandardCharsets.UTF_8);
    }

    private void writePeelProjectFile(Path projectDirectory, String projectId, String name, String description) throws IOException {
        Path peelProjectFile = projectDirectory.resolve(PEEL_PROJECT_FILE_NAME);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("$schema", "https://peel.dev/schema/peel-project.v1.json");
        root.put("formatVersion", 1);
        root.put("id", projectId);
        root.put("name", name);
        if (description != null) {
            root.put("description", description);
        }

        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("scriptsDir", SCRIPTS_DIR);
        paths.put("documentsDir", DOCUMENTS_DIR);
        paths.put("renderConfigsDir", RENDER_CONFIGS_DIR);
        paths.put("fixturesDir", FIXTURES_DIR);
        paths.put("outputDir", OUTPUT_DIR);
        root.put("paths", paths);

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("entryScript", SCRIPTS_DIR + "/main.peel");
        defaults.put("document", DOCUMENTS_DIR + "/main.pebble");
        defaults.put("renderConfig", RENDER_CONFIGS_DIR + "/default.json");
        defaults.put("bindings", FIXTURES_DIR + "/default-bindings.json");
        root.put("defaults", defaults);

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("apiBasePath", "/api");
        runtime.put("executionTimeoutMs", 10000);
        root.put("runtime", runtime);

        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("strictMode", true);
        validation.put("failOnMissingRenderTemplate", true);
        root.put("validation", validation);

        root.put("shared", Map.of("imports", List.of()));

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(peelProjectFile.toFile(), root);
    }

    private Path resolveExistingProjectDirectory(String rawProjectId) {
        String projectId = validateProjectId(rawProjectId);
        Path projectDirectory = resolveProjectDirectory(projectId);
        if (!Files.isDirectory(projectDirectory)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        return projectDirectory;
    }

    private Path resolveProjectDirectory(String projectId) {
        Path root = projectRoot();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare project root directory.", exception);
        }
        Path projectDirectory = root.resolve(projectId).normalize();
        if (!projectDirectory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid project id: " + projectId);
        }
        return projectDirectory;
    }

    private Path projectRoot() {
        return Paths.get(projectSettings.effectiveRoot()).toAbsolutePath().normalize();
    }

    private String resolveProjectId(String requestedId, String name) {
        if (requestedId == null || requestedId.isBlank()) {
            String slug = slugify(name);
            if (slug.length() < 2) {
                throw new IllegalArgumentException("name must contain at least two alphanumeric characters");
            }
            return validateProjectId(slug);
        }
        return validateProjectId(requestedId.trim());
    }

    private String validateProjectId(String projectId) {
        if (!PROJECT_ID_PATTERN.matcher(projectId).matches()) {
            throw new IllegalArgumentException("project id must match ^[a-z0-9][a-z0-9-]{1,62}$");
        }
        return projectId;
    }

    private String requireName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (normalizedName.length() > 80) {
            throw new IllegalArgumentException("name must be <= 80 characters");
        }
        return normalizedName;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 300) {
            throw new IllegalArgumentException("description must be <= 300 characters");
        }
        return normalized;
    }

    private String resolveTemplate(String template) {
        if (template == null || template.isBlank()) {
            return "empty";
        }
        String normalized = template.trim().toLowerCase(Locale.ROOT);
        if (!"empty".equals(normalized) && !"demo".equals(normalized)) {
            throw new IllegalArgumentException("template must be one of: empty, demo");
        }
        return normalized;
    }

    private String slugify(String value) {
        String lowerCase = value.toLowerCase(Locale.ROOT);
        String slug = lowerCase.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        if (slug.length() > 63) {
            return slug.substring(0, 63);
        }
        return slug;
    }

    private int countRegularFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            return Math.toIntExact(entries.filter(Files::isRegularFile).count());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to count files in " + directory, exception);
        }
    }

    private Instant readCreatedAt(Path projectDirectory) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(projectDirectory, BasicFileAttributes.class);
            return attributes.creationTime().toInstant();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read creation time for " + projectDirectory.getFileName(), exception);
        }
    }

    private Instant readUpdatedAt(Path projectDirectory) {
        try (Stream<Path> entries = Files.walk(projectDirectory)) {
            return entries
                    .map(this::lastModified)
                    .max(Comparator.naturalOrder())
                    .orElse(lastModified(projectDirectory));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read update time for " + projectDirectory.getFileName(), exception);
        }
    }

    private Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read last modified time for " + path, exception);
        }
    }

    private String textOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String textOrDefault(String value, String defaultValue) {
        String normalized = textOrNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizePath(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private record ProjectSnapshot(
            String id,
            String name,
            String description,
            Instant createdAt,
            Instant updatedAt,
            int formatVersion,
            String scriptsDir,
            String documentsDir,
            String renderConfigsDir,
            String fixturesDir,
            String outputDir,
            ProjectCounts counts,
            ProjectHealth health
    ) {
    }
}
