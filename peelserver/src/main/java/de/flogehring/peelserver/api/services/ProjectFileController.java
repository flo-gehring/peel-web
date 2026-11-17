package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.files.FileData.CreateFile;
import de.flogehring.peelserver.api.data.files.FileData.FileSummary;
import de.flogehring.peelserver.api.data.files.FileData.UpdateFile;
import de.flogehring.peelserver.api.error.ApiErrorResponse;
import de.flogehring.peelserver.impl.files.FileContent;
import de.flogehring.peelserver.impl.files.ProjectFile;
import de.flogehring.peelserver.impl.files.ProjectFileService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/files")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Invalid CSRF token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Resource unavailable", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Version, name, creation or reference conflict; or archived project", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ProjectFileController {
    private final ProjectFileService files;
    private final de.flogehring.peelserver.impl.run.AnalysisService analysis;

    @GetMapping("/{fileId}/blocks")
    @ApiResponse(responseCode = "200", description = "Static block catalog", content = @Content(array = @ArraySchema(schema = @Schema(implementation = de.flogehring.peelserver.api.data.scripts.BlockDefinition.class))))
    @Operation(summary = "Analyze named blocks and possible trace descendants without executing the saved script")
    public List<de.flogehring.peelserver.api.data.scripts.BlockDefinition> blocks(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        var file = files.get(projectId, fileId, user);
        if (!(file.content() instanceof FileContent.ScriptContent script))
            throw new IllegalArgumentException("Block selection requires a script");
        if (!script.source().isBlank()) {
            var diagnostics = analysis.analyze(new de.flogehring.peelserver.api.data.scripts.AnalyzeRequest(
                    new de.flogehring.peelserver.api.data.ProjectIdApi(projectId), script.source(), script.expectedBindings()))
                    .diagnostics().stream().filter(diagnostic -> diagnostic.severity().equals("error")).toList();
            if (!diagnostics.isEmpty()) throw new IllegalArgumentException(diagnostics.getFirst().message());
        }
        return de.flogehring.peel.parse.PeelGrammar.parseWithBlocks(script.source()).blockCatalog().blocks().stream()
                .map(block -> new de.flogehring.peelserver.api.data.scripts.BlockDefinition(
                        block.name(), block.parentName(), block.reachableBlocks())).toList();
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "File metadata", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FileSummary.class))))
    public List<FileSummary> list(
            @PathVariable String projectId,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        return files.list(projectId, type, user);
    }

    @GetMapping("/{fileId}")
    @ApiResponse(responseCode = "200", description = "Current file", content = @Content(schema = @Schema(implementation = ProjectFile.class)))
    public ProjectFile get(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        return files.get(projectId, fileId, user);
    }

    @PostMapping
    @ApiResponse(responseCode = "200", description = "Created or identical version-0 file", content = @Content(schema = @Schema(implementation = ProjectFile.class)))
    @Operation(summary = "Create a file at version 0", description = "A repeated creation succeeds only for an identical file still at version 0. Never overwrites an existing file.")
    public ProjectFile create(
            @PathVariable String projectId,
            @Valid @RequestBody CreateFile request,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        return files.create(projectId, request, user);
    }

    @PutMapping("/{fileId}")
    @ApiResponse(responseCode = "200", description = "Updated or identical current file", content = @Content(schema = @Schema(implementation = ProjectFile.class)))
    @Operation(summary = "Replace a file state using its expected version", description = "An identical target succeeds without writing at any expected version. Other updates atomically require the expected version and increment it.")
    public ProjectFile update(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @Valid @RequestBody UpdateFile request,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        return files.update(projectId, fileId, request, user);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an unreferenced file using its expected version", description = "Already absent files succeed. Changed files or blocking document references return 409.")
    public void delete(
            @PathVariable String projectId,
            @PathVariable String fileId,
            @RequestParam @Min(0) long expectedVersion,
            @AuthenticationPrincipal SpringUserDetails user
    ) {
        files.delete(projectId, fileId, expectedVersion, user);
    }
}
