package de.flogehring.peelserver.api.data;

import jakarta.validation.constraints.NotBlank;

public record ProjectIdApi(@NotBlank String id) {
}
