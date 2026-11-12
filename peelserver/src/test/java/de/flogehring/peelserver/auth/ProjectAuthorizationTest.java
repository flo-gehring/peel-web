package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.documenttemplates.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.renderconfig.*;
import de.flogehring.peelserver.impl.scripts.*;
import de.flogehring.peelserver.impl.files.*;
import java.time.Instant;
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
    @Autowired ProjectFileRepository files;
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
        scriptId = UUID.randomUUID().toString();
        insert(scriptId, "Script", new FileContent.ScriptContent("1 + 2;"));
        configId = prefix + "-config";
        var defaults = RenderConfigurationService.toDto(ExpressionRenderConfiguration.defaultConfig());
        insert(configId, "Config", new FileContent.RenderConfigContent(defaults.renderConfigurations(), defaults.namedOverrides()));
        documentId = prefix + "-document";
        insert(documentId, "Document", new FileContent.DocumentContent(Map.of("calculation", scriptId), "<p>Test</p>", "{}", configId));
    }

    @Test
    void filtersProjectsAndRejectsAnonymousAccess() throws Exception {
        mvc.perform(get("/api/projects/list").with(user(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.projectIdApi.id == '" + otherProject.id() + "')]").isEmpty());
        mvc.perform(get("/api/projects/" + project.id()).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + project.id() + "/files/" + scriptId)).andExpect(status().isUnauthorized());
    }

    @Test
    void printJobsHaveTheirOwnProjectAndDoNotExposeForeignFiles() throws Exception {
        var created = mvc.perform(body(post("/api/projects/" + project.id() + "/print-jobs"), Map.of("name", "Job", "documentId", Map.of("id", documentId)))
                .with(user(member))).andExpect(status().isOk()).andReturn();
        String jobId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/projects/" + project.id() + "/print-jobs").with(user(outsider))).andExpect(status().isNotFound());
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
        files.deleteById(documentId);
        mvc.perform(get("/api/projects/" + project.id() + "/print-jobs").with(user(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDocuments[0].jobs[0].projectId.id").value(project.id()));
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
        mvc.perform(get("/api/projects/" + project.id() + "/files/does-not-exist").with(user(owner))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
        files.deleteById(scriptId);
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(member))).andExpect(status().isNotFound());
        verifyNoInteractions(storage, runner);
    }

    @Test
    void checksCurrentMembershipOnEveryRequestAndDispatchesValidJobs() throws Exception {
        mvc.perform(get("/api/projects/" + project.id() + "/files/" + scriptId).with(user(member))).andExpect(status().isOk());
        String jobId = createJob(member);
        mvc.perform(post("/api/print-jobs/" + jobId + "/run").with(user(member)).with(csrf())).andExpect(status().isOk());
        verify(runner).run(argThat(job -> job.getProjectId().equals(project)), argThat(document -> document.scriptNameTags().containsKey("calculation")));
        projects.save(new ProjectPersistence(project.id(), new ProjectPersistenceData(owner.getUserId(), List.of(), "Project", "")));
        mvc.perform(get("/api/projects/" + project.id() + "/files/" + scriptId).with(user(member))).andExpect(status().isNotFound());
        mvc.perform(get("/api/print-jobs/" + jobId + "/data").with(user(member))).andExpect(status().isNotFound());
        verifyNoInteractions(storage);
    }

    private String createJob(SpringUserDetails user) throws Exception {
        var response = mvc.perform(body(post("/api/projects/" + project.id() + "/print-jobs"), Map.of("name", "Job", "documentId", Map.of("id", documentId)))
                .with(user(user))).andExpect(status().isOk()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void storedPreviewAndPrintExecutionRecheckReferenceProjectBoundaries() throws Exception {
        String foreignId = UUID.randomUUID().toString();
        files.insert(new ProjectFile(foreignId,otherProject,"Foreign","",0,new FileContent.ScriptContent("99;"),Instant.now(),Instant.now()));
        var document = files.findById(documentId).orElseThrow();
        files.save(new ProjectFile(document.id(),document.projectId(),document.name(),document.description(),document.version(),
                new FileContent.DocumentContent(Map.of("foreign",foreignId),"<p>Test</p>","{}",configId),document.createdAt(),document.updatedAt()));
        mvc.perform(body(post("/api/documents/preview-stored/" + documentId), Map.of()).with(user(owner))).andExpect(status().isNotFound());
        String job = createJob(owner);
        mvc.perform(post("/api/print-jobs/" + job + "/run").with(user(owner)).with(csrf())).andExpect(status().isNotFound());
        verifyNoInteractions(runner, storage);
    }

    private void insert(String id, String name, FileContent content) {
        files.insert(new ProjectFile(id,project,name,"",0,content,Instant.now(),Instant.now()));
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.with(csrf()).contentType("application/json").content(json.writeValueAsString(body));
    }
    private SpringUserDetails principal(String id) {
        return new SpringUserDetails(new UserId(id), new UserPersistenceData(id + "@example.test", null));
    }
}
