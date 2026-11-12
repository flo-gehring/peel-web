package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.users.*;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import de.flogehring.peelserver.impl.printjobs.*;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.documenttemplates.*;
import de.flogehring.peelserver.impl.renderconfig.*;
import de.flogehring.peelserver.api.data.print.jobs.*;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PeelModuleTest @AutoConfigureMockMvc
class ProjectManagementTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired UserRepository users;
    @Autowired ProjectInvitationRepository invitations;
    @Autowired MongoTemplate mongo;
    @Autowired PrintJobRepository jobs;
    @Autowired ProjectFileRepository scripts;
    @MockitoBean MailSender mail;
    @MockitoBean StorageService storage;
    SpringUserDetails owner, member, guest;
    String id;

    @BeforeEach void setup() {
        String prefix = UUID.randomUUID().toString();
        owner = account(prefix + "-owner"); member = account(prefix + "-member"); guest = account(prefix + "-guest");
        var project = ProjectPersistence.newProject(owner.getUserId(), "Test project");
        id = projects.save(project).getId();
    }
    private SpringUserDetails account(String name) {
        var user = users.insert(new UserPersistence(null, new UserPersistenceData(name + "@example.test", name)));
        return new SpringUserDetails(new UserId(user.getId()), user.getUserPersistenceData());
    }
    private String body(Object value) { return json.writeValueAsString(value); }
    private String invite(SpringUserDetails inviter, String email) throws Exception {
        clearInvocations(mail);
        mvc.perform(post("/api/projects/" + id + "/invitations").with(user(inviter)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", email)))).andExpect(status().isNoContent());
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        return captor.getValue().getText().split("/invitations/")[1].split("\\s")[0];
    }
    private void join(SpringUserDetails user) throws Exception {
        String token = invite(owner, user.getUsername());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(user)).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void invitationRequiresMatchingAccountAndExplicitAcceptance() throws Exception {
        String token = invite(owner, member.getUsername());
        mvc.perform(get("/api/invitations/" + token).with(user(member))).andExpect(status().isOk());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isNotFound());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(guest)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isOk());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void receivedInvitationsArePrivateAndRequireExplicitAcceptance() throws Exception {
        invite(owner, member.getUsername());
        var invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        mvc.perform(get("/api/invitations/received")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/invitations/received").with(user(guest))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/invitations/received").with(user(member)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(invitation.id()))
                .andExpect(jsonPath("$[0].projectName").value("Test project"))
                .andExpect(jsonPath("$[0].invitedByEmail").value(owner.getUsername()))
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isNotFound());
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(guest)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(member))).andExpect(status().isForbidden());
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isOk());
        mvc.perform(get("/api/invitations/received").with(user(member))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void receivedInvitationsCanBeDeclinedAndHideExpiredArchivedOrRevokedEntries() throws Exception {
        String token = invite(owner, member.getUsername());
        var invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        mongo.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().set("state", ProjectState.ARCHIVED), ProjectPersistence.class);
        mvc.perform(get("/api/invitations/received").with(user(member))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(member)).with(csrf())).andExpect(status().isConflict());
        mongo.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().set("state", ProjectState.ACTIVE), ProjectPersistence.class);
        mongo.updateFirst(Query.query(Criteria.where("id").is(invitation.id())), new Update().set("expiresAt", Instant.now().minusSeconds(1)), ProjectInvitationPersistence.class);
        mvc.perform(get("/api/invitations/received").with(user(member))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
        token = invite(owner, member.getUsername());
        invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/decline").with(user(guest)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/invitations/received/" + invitation.id() + "/decline").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/invitations/" + token).with(user(member))).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isNotFound());
        invite(owner, guest.getUsername());
        invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        mvc.perform(delete("/api/projects/" + id + "/invitations/" + invitation.id()).with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/invitations/received").with(user(guest))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void invitesUnregisteredAddressesWithoutAccountDisclosure() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String token = invite(owner, email);
        assertThat(users.findByUsername(email)).isNull();
        var created = users.insert(new UserPersistence(null, new UserPersistenceData(email, null)));
        var recipient = new SpringUserDetails(new UserId(created.getId()), created.getUserPersistenceData());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(recipient)).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void onlyOwnerAppointsAdminsAndAdminsCanInvite() throws Exception {
        join(member);
        mvc.perform(put("/api/projects/" + id + "/members/" + member.getUserId().id() + "/admin").with(user(member)).with(csrf())
                .contentType("application/json").content(body(Map.of("admin", true)))).andExpect(status().isForbidden());
        mvc.perform(put("/api/projects/" + id + "/members/" + member.getUserId().id() + "/admin").with(user(owner)).with(csrf())
                .contentType("application/json").content(body(Map.of("admin", true)))).andExpect(status().isNoContent());
        invite(member, guest.getUsername());
        mvc.perform(put("/api/projects/" + id + "/members/" + member.getUserId().id() + "/admin").with(user(member)).with(csrf())
                .contentType("application/json").content(body(Map.of("admin", true)))).andExpect(status().isForbidden());
    }
    @Test void transferNeedsBothConfirmationsAndRecipientConsent() throws Exception {
        join(member);
        mvc.perform(post("/api/projects/" + id + "/transfer").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", member.getUsername(), "confirmation", "wrong")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/projects/" + id + "/transfer").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", member.getUsername(), "confirmation", "TRANSFER Test project")))).andExpect(status().isNoContent());
        assertThat(projects.findById(id).orElseThrow().getPersistenceData().ownerId()).isEqualTo(owner.getUserId());
        var settings = mvc.perform(get("/api/projects/" + id + "/settings").with(user(member))).andReturn();
        String requestId = json.readTree(settings.getResponse().getContentAsString()).get("transfer").get("id").asText();
        mvc.perform(post("/api/projects/" + id + "/transfer/" + requestId + "/accept").with(user(guest)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/projects/" + id + "/transfer/" + requestId + "/accept").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        assertThat(projects.findById(id).orElseThrow().getPersistenceData().ownerId()).isEqualTo(member.getUserId());
        mvc.perform(get("/api/projects/" + id + "/settings").with(user(owner))).andExpect(jsonPath("$.role").value("ADMIN"));
    }
    @Test void archivedProjectsAreReadOnlyAndDeletionRequiresConfirmation() throws Exception {
        mvc.perform(post("/api/projects/" + id + "/archive").with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + id).with(user(owner))).andExpect(status().isOk());
        mvc.perform(post("/api/projects/" + id + "/files").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("id", UUID.randomUUID(), "name", "Blocked", "content", Map.of("type", "script", "source", ""))))).andExpect(status().isConflict());
        mvc.perform(delete("/api/projects/" + id).with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("confirmation", "wrong")))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/projects/" + id).with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("confirmation", "Test project")))).andExpect(status().isNoContent());
        assertThat(projects.existsById(id)).isFalse();
    }
    @Test void exportsCurrentOpenApiContract() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        java.nio.file.Files.writeString(java.nio.file.Path.of("build/openapi.json"), response.getResponse().getContentAsString());
    }

    @Test void membersEditMetadataButCannotManageOtherMembersOrArchive() throws Exception {
        join(member);
        mvc.perform(put("/api/projects/" + id + "/settings").with(user(member)).with(csrf()).contentType("application/json")
                .content(body(Map.of("name", "Updated", "description", "By a member")))).andExpect(status().isNoContent());
        mvc.perform(post("/api/projects/" + id + "/archive").with(user(member)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/projects/" + id + "/members/" + owner.getUserId().id()).with(user(member)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/settings").with(user(guest))).andExpect(status().isNotFound());
        mvc.perform(post("/api/projects/" + id + "/leave").with(user(owner)).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/projects/" + id + "/leave").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + id).with(user(member))).andExpect(status().isNotFound());
    }

    @Test void revokedExpiredAndDeclinedInvitationsCannotGrantMembership() throws Exception {
        String token = invite(owner, member.getUsername());
        var invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        assertThat(invitation.tokenHash()).isNotEqualTo(token);
        mvc.perform(delete("/api/projects/" + id + "/invitations/" + invitation.id()).with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
        token = invite(owner, member.getUsername());
        invitation = invitations.findByProjectId(new ProjectId(id)).getFirst();
        mongo.updateFirst(Query.query(Criteria.where("id").is(invitation.id())), new Update().set("expiresAt", Instant.now().minusSeconds(1)), ProjectInvitationPersistence.class);
        mvc.perform(post("/api/invitations/" + token + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
        token = invite(owner, guest.getUsername());
        mvc.perform(post("/api/invitations/" + token + "/decline").with(user(guest)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + id).with(user(guest))).andExpect(status().isNotFound());
    }

    @Test void failedEmailDoesNotLeaveAnInvitationOrRevealWhetherAnAccountExists() throws Exception {
        doThrow(new org.springframework.mail.MailSendException("unavailable")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post("/api/projects/" + id + "/invitations").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", UUID.randomUUID() + "@example.test")))).andExpect(status().isServiceUnavailable());
        assertThat(invitations.findByProjectId(new ProjectId(id))).isEmpty();
    }

    private String transfer() throws Exception {
        mvc.perform(post("/api/projects/" + id + "/transfer").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", member.getUsername(), "confirmation", "TRANSFER Test project")))).andExpect(status().isNoContent());
        return projects.findById(id).orElseThrow().getTransfer().id();
    }
    @Test void transferCanBeDeclinedWithdrawnOrExpireAndOnlyOneCanBePending() throws Exception {
        join(member);
        String request = transfer();
        mvc.perform(post("/api/projects/" + id + "/transfer").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("email", member.getUsername(), "confirmation", "TRANSFER Test project")))).andExpect(status().isConflict());
        mvc.perform(post("/api/projects/" + id + "/transfer/" + request + "/decline").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        assertThat(projects.findById(id).orElseThrow().getPersistenceData().ownerId()).isEqualTo(owner.getUserId());
        request = transfer();
        mvc.perform(delete("/api/projects/" + id + "/transfer/" + request).with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        request = transfer();
        mongo.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().set("transfer.expiresAt", Instant.now().minusSeconds(1)), ProjectPersistence.class);
        mvc.perform(post("/api/projects/" + id + "/transfer/" + request + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
        request = transfer();
        mvc.perform(delete("/api/projects/" + id + "/members/" + member.getUserId().id()).with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/projects/" + id + "/transfer/" + request + "/accept").with(user(member)).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void deletionWaitsForJobsAndCanRetryFailedFileCleanupWithoutLosingRecords() throws Exception {
        var project = new ProjectId(id);
        var script = scripts.insert(new ProjectFile(UUID.randomUUID().toString(), project, "Script", "", 0, new FileContent.ScriptContent("1;"), Instant.now(), Instant.now()));
        var job = PrintJobPersistence.init(new PrintJobId(UUID.randomUUID().toString()), new DocumentId("template"), project, "Job");
        job.updateStatus(PrintJobStatus.CALCULATING); jobs.save(job);
        mvc.perform(post("/api/projects/" + id + "/archive").with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/projects/" + id).with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("confirmation", "Test project")))).andExpect(status().isConflict());
        verifyNoInteractions(storage);
        job.updateStatus(PrintJobStatus.COMPLETED); jobs.save(job);
        doThrow(new IllegalStateException("MinIO offline")).doNothing().when(storage).deleteFilesWithPrefix("print-jobs", job.getId());
        mvc.perform(delete("/api/projects/" + id).with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("confirmation", "Test project")))).andExpect(status().isServiceUnavailable());
        assertThat(projects.findById(id).orElseThrow().getState()).isEqualTo(ProjectState.DELETING);
        assertThat(scripts.existsById(script.id())).isTrue();
        mvc.perform(post("/api/projects/" + id + "/files").with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("id", UUID.randomUUID(), "name", "No recreation", "content", Map.of("type", "script", "source", ""))))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/projects/" + id).with(user(owner)).with(csrf()).contentType("application/json")
                .content(body(Map.of("confirmation", "Test project")))).andExpect(status().isNoContent());
        assertThat(projects.existsById(id)).isFalse(); assertThat(scripts.existsById(script.id())).isFalse(); assertThat(jobs.existsById(job.getId())).isFalse();
    }
}
