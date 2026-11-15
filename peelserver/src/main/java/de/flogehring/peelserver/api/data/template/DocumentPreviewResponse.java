package de.flogehring.peelserver.api.data.template;

public record DocumentPreviewResponse(
        byte[] pdfData,
        java.util.List<String> warnings
) {
}
