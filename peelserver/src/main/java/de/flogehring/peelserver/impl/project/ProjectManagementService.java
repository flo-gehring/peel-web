package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.api.data.projects.ProjectManagementData.*;
import de.flogehring.peelserver.impl.auth.AuthLinkService;
import de.flogehring.peelserver.impl.auth.AuthProperties;
import de.flogehring.peelserver.impl.documenttemplates.DocumentTemplatePersistence;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.printjobs.*;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationPersistence;
import de.flogehring.peelserver.impl.scripts.PeelScriptPersistence;
import de.flogehring.peelserver.impl.users.*;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import static de.flogehring.peelserver.impl.project.ProjectAccessService.notFound;

@Service @RequiredArgsConstructor
public class ProjectManagementService {
    private final ProjectRepository projects;
    private final ProjectAccessService access;
    private final ProjectInvitationRepository invitations;
    private final UserRepository users;
    private final MongoTemplate mongo;
    private final PrintJobRepository jobs;
    private final StorageService storage;
    private final MailSender mail;
    private final AuthProperties auth;
    private final ProjectProperties properties;
    private final SecureRandom random = new SecureRandom();

    public ProjectSettings settings(String id, SpringUserDetails user) {
        var project = projects.findAccessibleById(id, user.getUserId()).orElseThrow(ProjectAccessService::notFound);
        var data = project.getPersistenceData();
        if (project.getState() == ProjectState.DELETING) access.requireOwner(project, user.getUserId());
        Set<String> memberIds = new LinkedHashSet<>();
        memberIds.add(data.ownerId().id()); data.members().forEach(member -> memberIds.add(member.id()));
        Map<String,String> emails = new HashMap<>();
        users.findAllById(memberIds).forEach(member -> emails.put(member.getId(), member.getUserPersistenceData().eMail()));
        var members = memberIds.stream().map(member -> new ProjectMember(member, emails.getOrDefault(member, ""),
                data.ownerId().id().equals(member) ? "OWNER" : data.admins().contains(new UserId(member)) ? "ADMIN" : "MEMBER")).toList();
        var pending = access.isAdmin(project, user.getUserId()) ? invitations.findByProjectId(new ProjectId(id)).stream()
                .filter(invite -> invite.expiresAt().isAfter(Instant.now()))
                .map(invite -> new ProjectInvitation(invite.id(), invite.email(), invite.expiresAt())).toList() : List.<ProjectInvitation>of();
        var transfer = project.getTransfer();
        boolean visible = transfer != null && transfer.expiresAt().isAfter(Instant.now())
                && (access.isOwner(project, user.getUserId()) || transfer.to().equals(user.getUserId()));
        return new ProjectSettings(id, data.projectName(), data.projectDescription(), project.getState(),
                access.isOwner(project, user.getUserId()) ? "OWNER" : access.isAdmin(project, user.getUserId()) ? "ADMIN" : "MEMBER",
                members, pending, visible ? new TransferRequest(transfer.id(), transfer.email(), transfer.to().id(), transfer.expiresAt()) : null);
    }

    public void update(String id, ProjectUpdate request, SpringUserDetails user) {
        change(id, user, true, project -> {
            var data = project.getPersistenceData();
            updateData(project, new ProjectPersistenceData(data.ownerId(), data.members(), request.name().strip(), request.description(), data.admins()));
            return null;
        });
    }

    public void archive(String id, SpringUserDetails user, boolean archive) {
        change(id, user, false, project -> {
            access.requireOwner(project, user.getUserId());
            mongo.updateFirst(byId(id), new Update().set("state", archive ? ProjectState.ARCHIVED : ProjectState.ACTIVE), ProjectPersistence.class);
            return null;
        });
    }

    public void invite(String id, String address, SpringUserDetails user) {
        change(id, user, true, project -> {
            access.requireAdmin(project, user.getUserId());
            String email = AuthLinkService.normalizeEmail(address);
            var previous = invitations.findByProjectId(new ProjectId(id)).stream().filter(invite -> invite.email().equals(email)).toList();
            if (previous.stream().anyMatch(invite -> invite.expiresAt().minus(properties.invitationValidity()).plusSeconds(60).isAfter(Instant.now())))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before sending another invitation.");
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            var invitation = new ProjectInvitationPersistence(UUID.randomUUID().toString(), new ProjectId(id), email,
                    user.getUserId(), hash(token), Instant.now().plus(properties.invitationValidity()));
            invitations.insert(invitation);
            try { send(email, "Invitation to " + project.getPersistenceData().projectName(),
                    "You have been invited to a PEEL WEB project. Sign in or register with this email address, then explicitly accept the invitation.\n\n"
                            + link("/invitations/" + token) + "\n\nValid for " + properties.invitationValidity().toDays() + " days."); }
            catch (RuntimeException exception) { invitations.deleteById(invitation.id()); throw exception; }
            previous.forEach(invite -> invitations.deleteById(invite.id()));
            return null;
        });
    }

    public void revokeInvitation(String id, String invitationId, SpringUserDetails user) {
        change(id, user, false, project -> {
            access.requireAdmin(project, user.getUserId());
            var invitation = invitations.findById(invitationId).filter(value -> value.projectId().id().equals(id)).orElseThrow(ProjectAccessService::notFound);
            invitations.deleteById(invitation.id()); return null;
        });
    }

    public void resendInvitation(String id, String invitationId, SpringUserDetails user) {
        var invitation = invitations.findById(invitationId).filter(value -> value.projectId().id().equals(id)).orElseThrow(ProjectAccessService::notFound);
        invite(id, invitation.email(), user);
    }

    public InvitationDetails invitation(String token, SpringUserDetails user) {
        var invite = recipientInvitation(token, user);
        var project = projects.findById(invite.projectId().id()).filter(value -> value.getState() != ProjectState.DELETING).orElseThrow(ProjectAccessService::notFound);
        return new InvitationDetails(project.getPersistenceData().projectName(), invite.email(), invite.expiresAt());
    }

    public void answerInvitation(String token, SpringUserDetails user, boolean accept) {
        answerInvitation(() -> recipientInvitation(token, user), user, accept);
    }

    public List<ReceivedInvitation> receivedInvitations(SpringUserDetails user) {
        var pending = invitations.findByEmailAndExpiresAtAfter(user.getUsername(), Instant.now());
        Map<String, ProjectPersistence> invitedProjects = new HashMap<>();
        projects.findAllById(pending.stream().map(invite -> invite.projectId().id()).distinct().toList())
                .forEach(project -> invitedProjects.put(project.getId(), project));
        Map<String, String> inviterEmails = new HashMap<>();
        users.findAllById(pending.stream().map(invite -> invite.invitedBy().id()).distinct().toList())
                .forEach(inviter -> inviterEmails.put(inviter.getId(), inviter.getUserPersistenceData().eMail()));
        return pending.stream().filter(invite -> {
            var project = invitedProjects.get(invite.projectId().id());
            return project != null && project.getState() == ProjectState.ACTIVE && access.isAdmin(project, invite.invitedBy());
        }).sorted(Comparator.comparing(ProjectInvitationPersistence::expiresAt))
                .map(invite -> new ReceivedInvitation(invite.id(), invitedProjects.get(invite.projectId().id()).getPersistenceData().projectName(),
                        inviterEmails.getOrDefault(invite.invitedBy().id(), ""), invite.expiresAt())).toList();
    }

    public void answerReceivedInvitation(String id, SpringUserDetails user, boolean accept) {
        answerInvitation(() -> invitations.findById(id).filter(invite -> invite.email().equals(user.getUsername())
                && invite.expiresAt().isAfter(Instant.now())).orElseThrow(ProjectAccessService::notFound), user, accept);
    }

    private void answerInvitation(java.util.function.Supplier<ProjectInvitationPersistence> lookup, SpringUserDetails user, boolean accept) {
        var invite = lookup.get();
        access.locked(invite.projectId(), () -> {
            var fresh = lookup.get();
            var project = projects.findById(fresh.projectId().id()).filter(value -> value.getState() != ProjectState.DELETING).orElseThrow(ProjectAccessService::notFound);
            if (accept) {
                access.requireActive(project);
                if (!access.isAdmin(project, fresh.invitedBy())) throw new ResponseStatusException(HttpStatus.CONFLICT, "The inviter no longer has invitation rights.");
                var data = project.getPersistenceData();
                Set<UserId> members = new LinkedHashSet<>(data.members());
                if (!data.ownerId().equals(user.getUserId())) members.add(user.getUserId());
                updateData(project, new ProjectPersistenceData(data.ownerId(), List.copyOf(members), data.projectName(), data.projectDescription(), data.admins()));
            }
            invitations.deleteById(fresh.id()); return null;
        });
    }

    public void admin(String id, String memberId, boolean admin, SpringUserDetails user) {
        change(id, user, true, project -> {
            access.requireOwner(project, user.getUserId());
            var data = project.getPersistenceData(); UserId member = new UserId(memberId);
            if (!data.members().contains(member) || data.ownerId().equals(member)) throw notFound();
            Set<UserId> admins = new LinkedHashSet<>(data.admins());
            if (admin) admins.add(member); else admins.remove(member);
            updateData(project, new ProjectPersistenceData(data.ownerId(), data.members(), data.projectName(), data.projectDescription(), List.copyOf(admins)));
            if (!admin) revokeInvitedBy(id, member);
            return null;
        });
    }

    public void removeMember(String id, String memberId, SpringUserDetails user, boolean self) {
        change(id, user, true, project -> {
            if (!self) access.requireOwner(project, user.getUserId());
            UserId member = self ? user.getUserId() : new UserId(memberId);
            var data = project.getPersistenceData();
            if (data.ownerId().equals(member)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfer ownership before leaving the project.");
            if (!data.members().contains(member)) throw notFound();
            updateData(project, new ProjectPersistenceData(data.ownerId(), data.members().stream().filter(value -> !value.equals(member)).toList(),
                    data.projectName(), data.projectDescription(), data.admins().stream().filter(value -> !value.equals(member)).toList()));
            revokeInvitedBy(id, member);
            if (project.getTransfer() != null && project.getTransfer().to().equals(member)) mongo.updateFirst(byId(id), new Update().unset("transfer"), ProjectPersistence.class);
            return null;
        });
    }

    public void requestTransfer(String id, TransferConfirmation request, SpringUserDetails user) {
        change(id, user, true, project -> {
            access.requireOwner(project, user.getUserId());
            var data = project.getPersistenceData();
            if (!("TRANSFER " + data.projectName()).equals(request.confirmation())) throw new IllegalArgumentException("Type TRANSFER followed by the exact project name.");
            if (project.getTransfer() != null && project.getTransfer().expiresAt().isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdraw the pending transfer first.");
            String email = AuthLinkService.normalizeEmail(request.email());
            var target = users.findByUsername(email);
            if (target == null || !data.members().contains(new UserId(target.getId())) || data.ownerId().id().equals(target.getId()))
                throw new IllegalArgumentException("Choose an existing project member and confirm their email address.");
            var transfer = new OwnershipTransfer(UUID.randomUUID().toString(), data.ownerId(), new UserId(target.getId()), email, Instant.now().plus(properties.transferValidity()));
            mongo.updateFirst(byId(id), new Update().set("transfer", transfer), ProjectPersistence.class);
            try { send(email, "Project ownership transfer request", "You can accept or decline ownership of " + data.projectName()
                    + ". Your role only changes after you accept.\n\n" + link("/projects/" + id + "/settings")
                    + "\n\nValid for " + properties.transferValidity().toDays() + " days."); }
            catch (RuntimeException exception) { mongo.updateFirst(byId(id), new Update().unset("transfer"), ProjectPersistence.class); throw exception; }
            return null;
        });
    }

    public void answerTransfer(String id, String requestId, SpringUserDetails user, String action) {
        change(id, user, action.equals("accept"), project -> {
            var transfer = project.getTransfer();
            if (transfer == null || !transfer.id().equals(requestId) || !transfer.expiresAt().isAfter(Instant.now())) throw notFound();
            if (action.equals("cancel")) access.requireOwner(project, user.getUserId());
            else if (!transfer.to().equals(user.getUserId())) throw notFound();
            if (!action.equals("accept")) { mongo.updateFirst(byId(id), new Update().unset("transfer"), ProjectPersistence.class); return null; }
            var data = project.getPersistenceData();
            if (!data.ownerId().equals(transfer.from()) || !data.members().contains(transfer.to())) throw new ResponseStatusException(HttpStatus.CONFLICT, "The transfer is no longer valid.");
            Set<UserId> members = new LinkedHashSet<>(data.members()); members.remove(transfer.to()); members.add(transfer.from());
            Set<UserId> admins = new LinkedHashSet<>(data.admins()); admins.remove(transfer.to()); admins.add(transfer.from());
            var query = Query.query(Criteria.where("id").is(id).and("persistenceData.ownerId").is(transfer.from()).and("transfer.id").is(requestId).and("state").is(ProjectState.ACTIVE));
            var result = mongo.updateFirst(query, new Update().set("persistenceData", new ProjectPersistenceData(transfer.to(), List.copyOf(members),
                    data.projectName(), data.projectDescription(), List.copyOf(admins))).unset("transfer"), ProjectPersistence.class);
            if (result.getModifiedCount() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "The project changed. Reload and try again.");
            return null;
        });
    }

    public void delete(String id, String confirmation, SpringUserDetails user) {
        access.locked(new ProjectId(id), () -> {
            var project = projects.findAccessibleById(id, user.getUserId()).orElseThrow(ProjectAccessService::notFound);
            access.requireOwner(project, user.getUserId());
            if (!project.getPersistenceData().projectName().equals(confirmation)) throw new IllegalArgumentException("Type the exact project name to delete it.");
            if (project.getState() == ProjectState.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Archive the project before deleting it.");
            ProjectId projectId = new ProjectId(id);
            var projectJobs = jobs.findByProjectIdIn(List.of(projectId));
            if (projectJobs.stream().anyMatch(job -> List.of(PrintJobStatus.CALCULATING, PrintJobStatus.PRINTING).contains(job.getPrintJobPersistenceData().status())))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for running print jobs to finish before deleting this project.");
            mongo.updateFirst(byId(id), new Update().set("state", ProjectState.DELETING), ProjectPersistence.class);
            // Keep records until all object deletion succeeds so a failed cleanup can be retried safely.
            try { for (var job : projectJobs) storage.deleteFilesWithPrefix(PrintJobService.BUCKET_NAME, job.getId()); }
            catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "File cleanup failed. Retry project deletion."); }
            for (Class<?> type : new Class<?>[]{PeelScriptPersistence.class, DocumentTemplatePersistence.class, RenderConfigurationPersistence.class, PrintJobPersistence.class})
                mongo.remove(Query.query(Criteria.where("projectId").is(projectId)), type);
            invitations.deleteByProjectId(projectId);
            projects.deleteById(id);
            return null;
        });
    }

    private <T> T change(String id, SpringUserDetails user, boolean active, Function<ProjectPersistence,T> action) {
        return access.locked(new ProjectId(id), () -> {
            var project = access.requireProjectAccess(new ProjectId(id), user.getUserId());
            if (active) access.requireActive(project);
            return action.apply(project);
        });
    }
    private void updateData(ProjectPersistence project, ProjectPersistenceData data) {
        mongo.updateFirst(byId(project.getId()), new Update().set("persistenceData", data), ProjectPersistence.class);
    }
    private void revokeInvitedBy(String id, UserId user) {
        invitations.findByProjectId(new ProjectId(id)).stream().filter(invite -> invite.invitedBy().equals(user)).forEach(invite -> invitations.deleteById(invite.id()));
    }
    private ProjectInvitationPersistence recipientInvitation(String token, SpringUserDetails user) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw notFound();
        return invitations.findByTokenHash(hash(token)).filter(invite -> invite.expiresAt().isAfter(Instant.now())
                && invite.email().equals(user.getUsername())).orElseThrow(ProjectAccessService::notFound);
    }
    private Query byId(String id) { return Query.query(Criteria.where("id").is(id)); }
    private String link(String path) { return UriComponentsBuilder.fromUri(auth.publicBaseUrl()).replacePath(path).build().encode().toUriString(); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private void send(String email, String subject, String body) {
        var message = new SimpleMailMessage(); message.setFrom(auth.mailFrom()); message.setTo(email); message.setSubject(subject); message.setText(body);
        try { mail.send(message); }
        catch (MailException exception) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The email could not be sent. Please try again later."); }
    }
}
