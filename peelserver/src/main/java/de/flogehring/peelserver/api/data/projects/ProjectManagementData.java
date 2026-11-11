package de.flogehring.peelserver.api.data.projects;

import de.flogehring.peelserver.impl.project.ProjectState;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ProjectManagementData {
    public record ProjectSettings(
            String id,
            String name,
            String description,
            ProjectState state,
            String role,
            List<ProjectMember> members,
            List<ProjectInvitation> invitations,
            TransferRequest transfer
    ) {
    }

    public record ProjectMember(String id, String email, String role) {
    }

    public record ProjectInvitation(String id, String email, Instant expiresAt) {
    }

    public record InvitationDetails(String projectName, String email, Instant expiresAt) {
    }

    public record ReceivedInvitation(String id, String projectName, String invitedByEmail, Instant expiresAt) {
    }

    public record TransferRequest(String id, String email, String recipientId, Instant expiresAt) {
    }

    public record ProjectUpdate(@NotBlank @Size(max = 200) String name,
                                @NotNull @Size(max = 10000) String description) {
    }

    public record EmailRequest(@NotBlank String email) {
    }

    public record AdminRequest(boolean admin) {
    }

    public record Confirmation(@NotBlank String confirmation) {
    }

    public record TransferConfirmation(@NotBlank String email, @NotBlank String confirmation) {
    }
}
