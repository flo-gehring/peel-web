package de.flogehring.peelserver.impl.run;

import jakarta.validation.constraints.NotNull;

public record ExpectedBinding(@NotNull String name, @NotNull String description) {}
