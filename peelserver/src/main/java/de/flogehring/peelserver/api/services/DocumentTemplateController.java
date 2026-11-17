package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.template.DocumentPreviewRequest;
import de.flogehring.peelserver.api.data.template.DocumentPreviewResponse;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping(path = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public interface DocumentTemplateController {

    @PostMapping(path = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentPreviewResponse previewDocument(@Valid @RequestBody DocumentPreviewRequest request, @AuthenticationPrincipal SpringUserDetails user);

    @PostMapping(path = "/preview-stored/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    DocumentPreviewResponse previewDocument(
            @PathVariable String id,
            @RequestBody @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
            Map<String, Object> bindings,
            @AuthenticationPrincipal SpringUserDetails user
    );
}
