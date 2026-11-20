package de.flogehring.peelserver.impl.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class LoginAllowlist {
    private final Path file;

    public LoginAllowlist(String configuredPath) {
        file = configuredPath.isBlank() ? null : Path.of(configuredPath);
        if (file != null) {
            try {
                readAddresses();
            } catch (IOException | AuthRequestException exception) {
                throw new IllegalStateException("The login allowlist must be readable and contain valid email addresses");
            }
        }
    }

    public boolean permits(String email) {
        if (file == null) return true;
        try {
            return readAddresses().contains(email);
        } catch (IOException | AuthRequestException exception) {
            // An unreadable or malformed replacement must never open registration.
            return false;
        }
    }

    private Set<String> readAddresses() throws IOException {
        Set<String> emails = new HashSet<>();
        for (String line : Files.readAllLines(file)) {
            String entry = line.strip();
            if (!entry.isEmpty() && !entry.startsWith("#")) {
                emails.add(AuthLinkService.normalizeEmail(entry));
            }
        }
        return emails;
    }
}
