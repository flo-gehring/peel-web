package de.flogehring.peelserver.impl.documenttemplates;

import jakarta.validation.constraints.NotBlank;

public record DocumentId(@NotBlank String id) {
}
