package de.flogehring.peelserver.impl.project;
import de.flogehring.peelserver.impl.users.UserId;
import java.time.Instant;
public record OwnershipTransfer(String id, UserId from, UserId to, String email, Instant expiresAt) {}
