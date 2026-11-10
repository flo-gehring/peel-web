package de.flogehring.peelserver.api.data.scripts;

import jakarta.validation.constraints.NotBlank;

public record PeelScriptId(@NotBlank String id) {
}
