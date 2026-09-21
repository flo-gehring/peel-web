package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.template.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping(path = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public interface DocumentTemplateController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentTemplateSaveResponse saveDocument(@RequestBody DocumentTemplateSaveRequest request);

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentTemplateSaveResponse updateDocument(@PathVariable("id") String id, @RequestBody DocumentTemplateUpdateRequest request);

    @GetMapping("/{id}")
    DocumentTemplateDto getDocument(@PathVariable String id);

    @DeleteMapping("/{id}")
    void deleteDocument(@PathVariable String id);

    @PostMapping(path = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentPreviewResponse previewDocument(@RequestBody DocumentPreviewRequest request);

    @PostMapping(path = "/preview-stored/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentPreviewResponse previewDocument(
            @PathVariable String id,
            @RequestBody @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
            Map<String, Object> bindings
    );
}
