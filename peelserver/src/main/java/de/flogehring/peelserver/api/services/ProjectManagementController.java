package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.projects.ProjectManagementData.*;
import de.flogehring.peelserver.impl.project.ProjectManagementService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectManagementController {
    private final ProjectManagementService service;

    @GetMapping("/projects/{id}/settings")
    public ProjectSettings settings(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user) {
        return service.settings(id, user);
    }

    @PutMapping("/projects/{id}/settings")
    @ResponseStatus(NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody ProjectUpdate request, @AuthenticationPrincipal SpringUserDetails user) {
        service.update(id, request, user);
    }

    @PostMapping("/projects/{id}/archive")
    @ResponseStatus(NO_CONTENT)
    public void archive(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user) {
        service.archive(id, user, true);
    }

    @PostMapping("/projects/{id}/restore")
    @ResponseStatus(NO_CONTENT)
    public void restore(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user) {
        service.archive(id, user, false);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable String id, @Valid @RequestBody Confirmation request, @AuthenticationPrincipal SpringUserDetails user) {
        service.delete(id, request.confirmation(), user);
    }

    @PostMapping("/projects/{id}/invitations")
    @ResponseStatus(NO_CONTENT)
    public void invite(@PathVariable String id, @Valid @RequestBody EmailRequest request, @AuthenticationPrincipal SpringUserDetails user) {
        service.invite(id, request.email(), user);
    }

    @DeleteMapping("/projects/{id}/invitations/{invitationId}")
    @ResponseStatus(NO_CONTENT)
    public void revoke(@PathVariable String id, @PathVariable String invitationId, @AuthenticationPrincipal SpringUserDetails user) {
        service.revokeInvitation(id, invitationId, user);
    }

    @PostMapping("/projects/{id}/invitations/{invitationId}/resend")
    @ResponseStatus(NO_CONTENT)
    public void resend(@PathVariable String id, @PathVariable String invitationId, @AuthenticationPrincipal SpringUserDetails user) {
        service.resendInvitation(id, invitationId, user);
    }

    @PutMapping("/projects/{id}/members/{memberId}/admin")
    @ResponseStatus(NO_CONTENT)
    public void admin(@PathVariable String id, @PathVariable String memberId, @RequestBody AdminRequest request, @AuthenticationPrincipal SpringUserDetails user) {
        service.admin(id, memberId, request.admin(), user);
    }

    @DeleteMapping("/projects/{id}/members/{memberId}")
    @ResponseStatus(NO_CONTENT)
    public void remove(@PathVariable String id, @PathVariable String memberId, @AuthenticationPrincipal SpringUserDetails user) {
        service.removeMember(id, memberId, user, false);
    }

    @PostMapping("/projects/{id}/leave")
    @ResponseStatus(NO_CONTENT)
    public void leave(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user) {
        service.removeMember(id, user.getUserId().id(), user, true);
    }

    @PostMapping("/projects/{id}/transfer")
    @ResponseStatus(NO_CONTENT)
    public void transfer(@PathVariable String id, @Valid @RequestBody TransferConfirmation request, @AuthenticationPrincipal SpringUserDetails user) {
        service.requestTransfer(id, request, user);
    }

    @DeleteMapping("/projects/{id}/transfer/{requestId}")
    @ResponseStatus(NO_CONTENT)
    public void cancel(@PathVariable String id, @PathVariable String requestId, @AuthenticationPrincipal SpringUserDetails user) {
        service.answerTransfer(id, requestId, user, "cancel");
    }

    @PostMapping("/projects/{id}/transfer/{requestId}/accept")
    @ResponseStatus(NO_CONTENT)
    public void acceptTransfer(@PathVariable String id, @PathVariable String requestId, @AuthenticationPrincipal SpringUserDetails user) {
        service.answerTransfer(id, requestId, user, "accept");
    }

    @PostMapping("/projects/{id}/transfer/{requestId}/decline")
    @ResponseStatus(NO_CONTENT)
    public void declineTransfer(@PathVariable String id, @PathVariable String requestId, @AuthenticationPrincipal SpringUserDetails user) {
        service.answerTransfer(id, requestId, user, "decline");
    }

    @GetMapping("/invitations/{token}")
    public InvitationDetails invitation(@PathVariable String token, @AuthenticationPrincipal SpringUserDetails user) {
        return service.invitation(token, user);
    }

    @PostMapping("/invitations/{token}/accept")
    @ResponseStatus(NO_CONTENT)
    public void acceptInvitation(@PathVariable String token, @AuthenticationPrincipal SpringUserDetails user) {
        service.answerInvitation(token, user, true);
    }

    @PostMapping("/invitations/{token}/decline")
    @ResponseStatus(NO_CONTENT)
    public void declineInvitation(@PathVariable String token, @AuthenticationPrincipal SpringUserDetails user) {
        service.answerInvitation(token, user, false);
    }
}
