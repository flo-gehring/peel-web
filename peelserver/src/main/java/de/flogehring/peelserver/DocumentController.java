package de.flogehring.peelserver;

import de.flogehring.peelserver.api.DocumentPreviewRequest;
import de.flogehring.peelserver.api.DocumentPreviewResponse;
import de.flogehring.peelserver.api.DocumentResponse;
import de.flogehring.peelserver.api.DocumentSaveRequest;
import de.flogehring.peelserver.api.DocumentSummaryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/documents")
public interface DocumentController {

    @PostExchange
    DocumentResponse saveDocument(@RequestBody DocumentSaveRequest request);

    @GetExchange
    List<DocumentSummaryResponse> listDocuments();

    @GetExchange("/{id}")
    DocumentResponse getDocument(@PathVariable String id);

    @DeleteExchange("/{id}")
    void deleteDocument(@PathVariable String id);

    @PostExchange("/preview")
    DocumentPreviewResponse previewDocument(@RequestBody DocumentPreviewRequest request);
}
