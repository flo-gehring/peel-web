package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.template.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange("/api/documents")
public interface DocumentTemplateController {

    @PostExchange
    DocumentTemplateSaveResponse saveDocument(@RequestBody DocumentTemplateSaveRequest request);

    @GetExchange
    List<DocumentTemplateSummaryResponse> listDocuments();

    @GetExchange("/{id}")
    DocumentTemplate getDocument(@PathVariable String id);

    @DeleteExchange("/{id}")
    void deleteDocument(@PathVariable String id);

    @PostExchange("/preview")
    DocumentPreviewResponse previewDocument(@RequestBody DocumentPreviewRequest request);

    @PostExchange("/preview-stored/{id}")
    DocumentPreviewResponse previewDocument(@PathVariable String id, @RequestBody Map<String, Object> bindings);
}
