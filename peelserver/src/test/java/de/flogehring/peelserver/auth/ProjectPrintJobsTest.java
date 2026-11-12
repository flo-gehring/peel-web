package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.printjobs.*;
import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PeelModuleTest @AutoConfigureMockMvc
class ProjectPrintJobsTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired ProjectFileRepository files;
    @Autowired PrintJobRepository jobs;
    @Autowired MongoTemplate mongo;
    String project, document;
    SpringUserDetails owner, outsider;

    @BeforeEach void setup() {
        project = UUID.randomUUID().toString(); document = UUID.randomUUID().toString();
        owner = principal("owner"); outsider = principal("outsider");
        projects.insert(new ProjectPersistence(project, new ProjectPersistenceData(owner.getUserId(), List.of(), "Project", "")));
        insertDocument(document, "Invoice");
    }

    @Test void groupsAllDocumentsIncludingEmptyOnesAndKeepsDeletedDocumentsSeparate() throws Exception {
        insertDocument(UUID.randomUUID().toString(), "Empty");
        String first = create("First");
        mongo.updateFirst(Query.query(Criteria.where("id").is(first)), new Update().set("createdAt", Instant.now().minusSeconds(60)), PrintJobPersistence.class);
        String second = create("Second");
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.length()").value(2))
                .andExpect(jsonPath("$.documents[0].name").value("Empty"))
                .andExpect(jsonPath("$.documents[0].jobs.length()").value(0))
                .andExpect(jsonPath("$.documents[1].jobs[0].printJobId.id").value(second));
        files.deleteById(document);
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.documents.length()").value(1))
                .andExpect(jsonPath("$.deletedDocuments[0].documentId").value(document))
                .andExpect(jsonPath("$.deletedDocuments[0].name").value("Invoice"))
                .andExpect(jsonPath("$.deletedDocuments[0].jobs.length()").value(2));
        String replacement=UUID.randomUUID().toString(); insertDocument(replacement,"Invoice");
        mvc.perform(get(base()).with(user(owner))).andExpect(jsonPath("$.documents[1].documentId").value(replacement))
                .andExpect(jsonPath("$.documents[1].jobs.length()").value(0))
                .andExpect(jsonPath("$.deletedDocuments[0].documentId").value(document));
    }

    @Test void snapshotsDocumentNameAndDateAndPreservesThemDuringJobUpdates() throws Exception {
        String id=create(" Job ");
        var job=jobs.findById(id).orElseThrow();
        assertThat(job.getCreatedAt()).isNotNull(); assertThat(job.getDocumentName()).isEqualTo("Invoice");
        job.updateFileName("data.csv"); jobs.save(job);
        mongo.updateFirst(Query.query(Criteria.where("id").is(document)),new Update().set("name","Renamed"),ProjectFile.class);
        mvc.perform(get(base()).with(user(owner))).andExpect(jsonPath("$.documents[0].name").value("Renamed"))
                .andExpect(jsonPath("$.documents[0].jobs[0].documentName").value("Invoice"));
        mvc.perform(get(base()+"/"+id).with(user(owner))).andExpect(jsonPath("$.currentDocumentName").value("Renamed"))
                .andExpect(jsonPath("$.job.name").value("Job"));
        files.deleteById(document);
        mvc.perform(get(base()+"/"+id).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDocumentName").isEmpty()).andExpect(jsonPath("$.job.documentName").value("Invoice"));
        mvc.perform(post("/api/print-jobs/"+id+"/run").with(user(owner)).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void checksProjectBoundariesAndArchiveStateForListDetailAndCreation() throws Exception {
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
        mvc.perform(get(base()).with(user(outsider))).andExpect(status().isNotFound());
        String id=create("Job");
        mvc.perform(get(base()+"/"+id).with(user(outsider))).andExpect(status().isNotFound());
        String other=UUID.randomUUID().toString();
        projects.insert(new ProjectPersistence(other,new ProjectPersistenceData(owner.getUserId(),List.of(),"Other","")));
        mvc.perform(post("/api/projects/"+other+"/print-jobs").with(user(owner)).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name","Foreign","documentId",Map.of("id",document))))).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/"+other+"/print-jobs/"+id).with(user(owner))).andExpect(status().isNotFound());
        mvc.perform(post("/api/projects/"+project+"/archive").with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ARCHIVED"));
        mvc.perform(post(base()).with(user(owner)).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name","Blocked","documentId",Map.of("id",document))))).andExpect(status().isConflict());
    }
    @Test void handlesJobsWithMissingDatesAndDocumentNamesWithoutChangingStoredData() throws Exception {
        String older = create("Older"), newer = create("Newer");
        mongo.updateFirst(Query.query(Criteria.where("id").is(older)), new Update().unset("createdAt").unset("documentName"), PrintJobPersistence.class);
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].jobs[0].printJobId.id").value(newer))
                .andExpect(jsonPath("$.documents[0].jobs[1].printJobId.id").value(older));
        files.deleteById(document);
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDocuments[0].name").value("Invoice"));
        mongo.updateFirst(Query.query(Criteria.where("id").is(newer)), new Update().unset("createdAt").unset("documentName"), PrintJobPersistence.class);
        mvc.perform(get(base()).with(user(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDocuments[0].name").value("Gelöschte Dokumentvorlage"));
        assertThat(mongo.getCollection("printjobs").find(new org.bson.Document("_id", older)).first().containsKey("createdAt")).isFalse();
    }
    private String base(){return "/api/projects/"+project+"/print-jobs";}
    @Test void signInRedirectPreservesProjectPrintJobRoutes() {
        String path="/projects/"+project+"/print-jobs/job";
        assertThat(de.flogehring.peelserver.impl.auth.AuthLinkService.safeRedirect(path)).isEqualTo(path);
        assertThat(de.flogehring.peelserver.impl.auth.AuthLinkService.safeRedirect("/print-jobs")).isEqualTo("/");
        assertThat(de.flogehring.peelserver.impl.auth.AuthLinkService.safeRedirect("https://example.test")).isEqualTo("/");
    }
    private String create(String name)throws Exception{
        var result=mvc.perform(post(base()).with(user(owner)).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name",name,"documentId",Map.of("id",document)))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    private void insertDocument(String id,String name){files.insert(new ProjectFile(id,new ProjectId(project),name,"",0,
            new FileContent.DocumentContent(Map.of(),"<p>Test</p>","{}","config"),Instant.now(),Instant.now()));}
    private SpringUserDetails principal(String suffix){return new SpringUserDetails(new UserId(project+suffix),new UserPersistenceData(project+suffix+"@test.example",null));}
}
