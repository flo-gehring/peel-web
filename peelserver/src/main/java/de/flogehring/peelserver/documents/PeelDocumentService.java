package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.DocumentController;
import de.flogehring.peelserver.api.DocumentPreviewRequest;
import de.flogehring.peelserver.api.DocumentPreviewResponse;
import de.flogehring.peelserver.api.DocumentResponse;
import de.flogehring.peelserver.api.DocumentSaveRequest;
import de.flogehring.peelserver.api.DocumentSummaryResponse;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PeelDocumentService implements DocumentController {

    private final PeelDocumentRepository peelDocumentRepository;
    private final DocumentPreviewService documentPreviewService;

    @Override
    public DocumentResponse saveDocument(DocumentSaveRequest request) {
        String script = requireScript(request.script());
        String template = requireTemplate(request.template());
        Map<String, Object> bindings = request.exampleBindings();
        if (request.id() != null && !request.id().isBlank()) {
            PeelDocument existing = peelDocumentRepository.findById(request.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + request.id()));
            PeelDocument saved = peelDocumentRepository.save(existing.update(request.name(), script, template, bindings));
            return toResponse(saved);
        }
        PeelDocument created = PeelDocument.newDocument(request.name(), script, template, bindings);
        PeelDocument saved = peelDocumentRepository.save(created);
        return toResponse(saved);
    }

    @Override
    public List<DocumentSummaryResponse> listDocuments() {
        return peelDocumentRepository.findAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(DocumentSummaryResponse::updatedAt).reversed())
                .toList();
    }

    @Override
    public DocumentResponse getDocument(String id) {
        PeelDocument document = peelDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return toResponse(document);
    }

    @Override
    public void deleteDocument(String id) {
        if (!peelDocumentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found: " + id);
        }
        peelDocumentRepository.deleteById(id);
    }

    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request) {
        return documentPreviewService.preview(request);
    }

    private DocumentResponse toResponse(PeelDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getName(),
                document.getScript(),
                document.getTemplate(),
                document.getExampleBindings(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private DocumentSummaryResponse toSummary(PeelDocument document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getName(),
                document.getUpdatedAt()
        );
    }

    private String requireScript(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return script;
    }

    private String requireTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        return template;
    }
}
