package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.documenttemplates.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.renderconfig.*;
import de.flogehring.peelserver.impl.scripts.*;
import de.flogehring.peelserver.impl.users.*;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.printjobs.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PeelModuleTest
@AutoConfigureMockMvc
class ProjectAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired PeelScriptRepository scripts;
    @Autowired PeelDocumentRepository documents;
    @Autowired RenderConfigurationRepository configs;
    @Autowired PrintJobRepository jobs;
    @MockitoBean StorageService storage;
    @MockitoBean PrintJobRunner runner;
    SpringUserDetails owner, member, outsider;
    ProjectId project, otherProject;
    String scriptId, configId, documentId;

    @BeforeEach
    void setUp() {
        String prefix = UUID.randomUUID().toString();
        owner = principal(prefix + "-owner");
        member = principal(prefix + "-member");
        outsider = principal(prefix + "-outsider");
        project = new ProjectId(prefix + "-project");
        otherProject = new ProjectId(prefix + "-other");
        projects.save(new ProjectPersistence(project.id(), new ProjectPersistenceData(owner.getUserId(), List.of(member.getUserId()), "Project", "")));
        projects.save(new ProjectPersistence(otherProject.id(), new ProjectPersistenceData(outsider.getUserId(), List.of(owner.getUserId()), "Other", "")));
        scriptId = scripts.save(PeelScriptPersistence.newScript(project, "Script", "1 + 2;")).getId();
        configId = prefix + "-config";
        configs.save(RenderConfigurationPersistence.valueOf(configId, "Config", ExpressionRenderConfiguration.defaultConfig(), project));
        documentId = prefix + "-document";
        documents.save(DocumentTemplatePersistence.newDocument(documentId, "Document", Map.of("calculation", new PeelScriptId(scriptId)),
                "<p>Test</p>", "{}", new RenderConfigurationId(configId), project));
    }

    @Test
    void hidesForeignResourcesForReadsUpdatesAndDeletesWithoutSideEffects() throws Exception {
        for (String path : List.of("/api/scripts/" + scriptId, "/api/documents/" + documentId, "/api/render-config/" + configId)) {
            mvc.perform(get(path).with(user(outsider))).andExpect(status().isNotFound());
            mvc.perform(delete(path).with(user(outsider)).with(csrf())).andExpect(status().isNotFound());
        }
        mvc.perform(body(put("/api/scripts/" + scriptId), Map.of("name", "Changed", "scriptContent", "42")).with(user(outsider)))
                .andExpect(status().isNotFound());
        var spoofedProject = documentBody();
        spoofedProject.put("projectId", Map.of("id", otherProject.id()));
        mvc.perform(body(put("/api/documents/" + documentId), spoofedProject).with(user(outsider)))
                .andExpect(status().isNotFound());
        mvc.perform(body(put("/api/render-config/" + configId), Map.of("name", "Changed", "renderConfigurationDto", Map.of())).with(user(outsider)))
                .andExpect(status().isNotFound());
        assertThat(scripts.findById(scriptId).orElseThrow().getPeelScript().script()).isEqualTo("1 + 2;");
        assertThat(documents.existsById(documentId)).isTrue();
        assertThat(configs.existsById(configId)).isTrue();
        assertThat(documents.findById(documentId).orElseThrow().getData().templateHtml()).isEqualTo("<p>Test</p>");
        assertThat(configs.findById(configId).orElseThrow().getName()).isEqualTo("Config");
        verifyNoInteractions(storage);
    }

    @Test
    void membersCanCreateReadUpdateAndDeleteProjectContents() throws Exception {
        var created = mvc.perform(body(post("/api/scripts"), Map.of("projectIdApi", Map.of("id", project.id()), "name", "New", "script", "1"))
                .with(user(member))).andExpect(status().isOk()).andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/scripts/" + id).with(user(member))).andExpect(status().isOk());
        mvc.perform(body(put("/api/scripts/" + id), Map.of("name", "Updated", "scriptContent", "2")).with(user(member))).andExpect(status().isOk());
        mvc.perform(delete("/api/scripts/" + id).with(user(member)).with(csrf())).andExpect(status().isOk());
        assertThat(scripts.existsById(id)).isFalse();
        var createdDocument = mvc.perform(body(post("/api/documents"), documentBody()).with(user(member)))
                .andExpect(status().isOk()).andReturn();
        String createdDocumentId = json.readTree(createdDocument.getResponse().getContentAsString()).get("id").asText();
        assertThat(documents.findById(createdDocumentId).orElseThrow().getProjectId()).isEqualTo(project);
        mvc.perform(body(put("/api/documents/" + documentId), documentBody()).with(user(member))).andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + documentId).with(user(member))).andExpect(status().isOk());
        mvc.perform(delete("/api/documents/" + documentId).with(user(member)).with(csrf())).andExpect(status().isOk());
        var defaultConfig = mvc.perform(get("/api/render-config/default").with(user(member))).andReturn().getResponse().getContentAsString();
        mvc.perform(body(post("/api/render-config/"), Map.of("name", "Created", "projectIdApi", Map.of("id", project.id()),
                "renderConfigurationDto", json.readTree(defaultConfig))).with(user(member))).andExpect(status().isOk());
        mvc.perform(body(put("/api/render-config/" + configId), Map.of("name", "Updated", "renderConfigurationDto", json.readTree(defaultConfig)))
                .with(user(member))).andExpect(status().isOk());
        mvc.perform(get("/api/render-config/" + configId).with(user(member))).andExpect(status().isOk());
        mvc.perform(delete("/api/render-config/" + configId).with(user(member)).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void rejectsCreatingContentsInAForeignProject() throws Exception {
        mvc.perform(body(post("/api/scripts"), Map.of("projectIdApi", Map.of("id", project.id()), "name", "Foreign", "script", "1"))
                .with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(body(post("/api/documents"), documentBody()).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(body(post("/api/render-config/"), Map.of("projectIdApi", Map.of("id", project.id()), "name", "Foreign", "renderConfigurationDto", Map.of()))
                .with(user(outsider))).andExpect(status().isNotFound());
    }

    @Test
    void rejectsCrossProjectLinksEvenWhenBothProjectsAreAccessible() throws Exception {
        String otherScript = scripts.save(PeelScriptPersistence.newScript(otherProject, "Other", "99")).getId();
        var body = documentBody();
        body.put("scriptNameTags", Map.of("foreign", otherScript));
        mvc.perform(body(put("/api/documents/" + documentId), body).with(user(owner))).andExpect(status().isNotFound());
        assertThat(documents.findById(documentId).orElseThrow().getData().scriptNameTags()).containsOnlyKeys("calculation");
        mvc.perform(body(post("/api/documents/preview"), Map.of("projectId", Map.of("id", project.id()),
                        "scriptTags", Map.of("foreign", Map.of("id", otherScript)), "renderConfigId", configId, "bindings", Map.of(), "template", "<p>Test</p>"))
                .with(user(owner))).andExpect(status().isNotFound());
        String foreignConfig = UUID.randomUUID().toString();
        configs.save(RenderConfigurationPersistence.valueOf(foreignConfig, "Other config", ExpressionRenderConfiguration.defaultConfig(), otherProject));
        var crossConfig = documentBody();
        crossConfig.put("renderConfigurationId", foreignConfig);
        mvc.perform(body(post("/api/documents"), crossConfig).with(user(owner))).andExpect(status().isNotFound());
        // Old or externally modified records must not bypass checks during stored previews or print execution.
        var persisted = documents.findById(documentId).orElseThrow();
        documents.save(persisted.update("Document", Map.of("foreign", new PeelScriptId(otherScript)), "<p>Test</p>", "{}", new RenderConfigurationId(configId)));
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(owner))).andExpect(status().isNotFound());
        String job = createJob(owner);
        mvc.perform(post("/api/print-jobs/" + job + "/run").with(user(owner)).with(csrf())).andExpect(status().isNotFound());
        verifyNoInteractions(runner, storage);
    }

    @Test
    void filtersProjectsAndRejectsAnonymousAccess() throws Exception {
        mvc.perform(get("/api/projects/list").with(user(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.projectIdApi.id == '" + otherProject.id() + "')]").isEmpty());
        mvc.perform(get("/api/projects/" + project.id()).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(get("/api/scripts/" + scriptId)).andExpect(status().isUnauthorized());
    }

    @Test
    void printJobsHaveTheirOwnProjectAndDoNotExposeForeignFiles() throws Exception {
        var created = mvc.perform(body(post("/api/print-jobs"), Map.of("name", "Job", "documentId", Map.of("id", documentId)))
                .with(user(member))).andExpect(status().isOk()).andReturn();
        String jobId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/print-jobs/list").with(user(outsider))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.printJobId.id == '" + jobId + "')]").isEmpty());
        mvc.perform(post("/api/print-jobs/" + jobId + "/run").with(user(outsider)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/print-jobs/" + jobId + "/data").with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(get("/api/print-jobs/" + jobId + "/file/guessed.pdf").with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/print-jobs/" + jobId + "/data").file(new MockMultipartFile("file", "test.csv", "text/csv", "a\n1".getBytes()))
                .with(request -> { request.setMethod("PUT"); return request; }).with(user(outsider)).with(csrf())).andExpect(status().isNotFound());
        verifyNoInteractions(storage);
        verifyNoInteractions(runner);
        assertThat(jobs.findById(jobId).orElseThrow().getPrintJobPersistenceData().status()).isEqualTo(de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus.CREATED);
        var storedJob = jobs.findById(jobId).orElseThrow();
        storedJob.updateFileName("data.csv");
        storedJob.setDocumentIds(List.of("result.pdf"));
        jobs.save(storedJob);
        documents.deleteById(documentId);
        mvc.perform(get("/api/print-jobs/list").with(user(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.printJobId.id == '" + jobId + "')].projectId.id").value(org.hamcrest.Matchers.hasItem(project.id())));
        mvc.perform(post("/api/print-jobs/" + jobId + "/run").with(user(member)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/print-jobs/" + jobId + "/file/guessed.pdf").with(user(member))).andExpect(status().isNotFound());
        verifyNoInteractions(storage);
        when(storage.downloadFile("print-jobs", "result.pdf")).thenReturn("pdf".getBytes());
        mvc.perform(get("/api/print-jobs/" + jobId + "/file/result.pdf").with(user(member)))
                .andExpect(status().isOk()).andExpect(content().bytes("pdf".getBytes()));
        when(storage.downloadFile("print-jobs", jobId)).thenReturn("csv".getBytes());
        mvc.perform(get("/api/print-jobs/" + jobId + "/data").with(user(owner)))
                .andExpect(status().isOk()).andExpect(content().bytes("csv".getBytes()));
    }

    @Test
    void rendersOwnProjectPreviewsAndRejectsForeignPreviewContexts() throws Exception {
        mvc.perform(body(post("/api/documents/preview"), Map.of("projectId", Map.of("id", project.id()),
                "scriptTags", Map.of("calculation", Map.of("id", scriptId)), "bindings", Map.of(), "renderConfigId", configId, "template", "<p>Test</p>"))
                .with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.pdfData").isNotEmpty());
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pdfData").isNotEmpty());
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(outsider)))
                .andExpect(status().isNotFound());
        mvc.perform(body(post("/api/documents/preview"), Map.of("projectId", Map.of("id", project.id()),
                "scriptTags", Map.of(), "bindings", Map.of(), "renderConfigId", configId, "template", "<p>Test</p>"))
                .with(user(outsider))).andExpect(status().isNotFound());
    }

    @Test
    void validatesPreviewContextAndMissingReferences() throws Exception {
        mvc.perform(body(post("/api/documents/preview"), Map.of("scriptTags", Map.of(), "bindings", Map.of(), "renderConfigId", configId, "template", "<p>Test</p>"))
                .with(user(owner))).andExpect(status().isBadRequest());
        var missingScript = documentBody();
        missingScript.put("scriptNameTags", Map.of("missing", "does-not-exist"));
        mvc.perform(body(put("/api/documents/" + documentId), missingScript).with(user(owner))).andExpect(status().isNotFound());
        mvc.perform(get("/api/scripts/does-not-exist").with(user(owner))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        scripts.deleteById(scriptId);
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(member))).andExpect(status().isNotFound());
        verifyNoInteractions(storage, runner);
    }

    @Test
    void checksCurrentMembershipOnEveryRequestAndDispatchesValidJobs() throws Exception {
        mvc.perform(get("/api/scripts/" + scriptId).with(user(member))).andExpect(status().isOk());
        String jobId = createJob(member);
        mvc.perform(post("/api/print-jobs/" + jobId + "/run").with(user(member)).with(csrf())).andExpect(status().isOk());
        verify(runner).run(argThat(job -> job.getProjectId().equals(project)), argThat(document -> document.scriptNameTags().containsKey("calculation")));
        projects.save(new ProjectPersistence(project.id(), new ProjectPersistenceData(owner.getUserId(), List.of(), "Project", "")));
        mvc.perform(get("/api/scripts/" + scriptId).with(user(member))).andExpect(status().isNotFound());
        mvc.perform(get("/api/print-jobs/" + jobId + "/data").with(user(member))).andExpect(status().isNotFound());
        verifyNoInteractions(storage);
    }

    private String createJob(SpringUserDetails user) throws Exception {
        var response = mvc.perform(body(post("/api/print-jobs"), Map.of("name", "Job", "documentId", Map.of("id", documentId)))
                .with(user(user))).andExpect(status().isOk()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }

    private Map<String, Object> documentBody() {
        return new HashMap<>(Map.of("projectId", Map.of("id", project.id()), "name", "Document", "templateHtml", "<p>Changed</p>",
                "editorStateJson", "{}", "scriptNameTags", Map.of("calculation", scriptId), "renderConfigurationId", configId));
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.with(csrf()).contentType("application/json").content(json.writeValueAsString(body));
    }
    private SpringUserDetails principal(String id) {
        return new SpringUserDetails(new UserId(id), new UserPersistenceData(id + "@example.test", null));
    }
}
