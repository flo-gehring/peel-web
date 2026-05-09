package de.flogehring.peelserver;

import de.flogehring.peelserver.api.document.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange("/api/documents")
public interface DocumentController {

    @PostExchange
    DocumentSaveResponse saveDocument(@RequestBody DocumentSaveRequest request);

    @GetExchange
    List<DocumentSummaryResponse> listDocuments();

    @GetExchange("/{id}")
    DocumentContent getDocument(@PathVariable String id);

    @DeleteExchange("/{id}")
    void deleteDocument(@PathVariable String id);

    @PostExchange("/preview")
    DocumentPreviewResponse previewDocument(@RequestBody DocumentPreviewRequest request);

    @PostExchange("/preview-stored/{id}")
    DocumentPreviewResponse previewDocument(@PathVariable String id, @RequestBody Map<String, Object> bindings);
}
