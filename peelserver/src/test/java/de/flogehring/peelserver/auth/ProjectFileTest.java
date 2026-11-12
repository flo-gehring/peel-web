package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PeelModuleTest @AutoConfigureMockMvc
class ProjectFileTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MongoTemplate mongo;
    @Autowired ProjectRepository projects;
    @Autowired ProjectFileRepository files;
    @Autowired ProjectFileService service;
    SpringUserDetails owner, member, outsider;
    String project, id;

    @BeforeEach void setup() {
        project = UUID.randomUUID().toString(); id = UUID.randomUUID().toString();
        owner = principal("owner"); member = principal("member"); outsider = principal("outsider");
        projects.insert(new ProjectPersistence(project, new ProjectPersistenceData(owner.getUserId(), List.of(member.getUserId()), "Project", "")));
    }

    @Test void createsAllTypesWithStableAliasesAndMetadataOnlyLists() throws Exception {
        create(id, "Script", script(""));
        String config = UUID.randomUUID().toString(); create(config, "Script", config());
        String document = UUID.randomUUID().toString(); create(document, "Document", document(id, config));
        mvc.perform(get(base()).with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].content").doesNotExist()).andExpect(jsonPath("$[0].version").value(0));
        mvc.perform(get(base()).param("type", "script").with(user(member)))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].type").value("script"));
        for (String fileId : List.of(id, config, document)) {
            mvc.perform(get(base()+"/"+fileId).with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.content.type").exists());
            var raw = mongo.getCollection("project-files").find(new org.bson.Document("_id", fileId)).first();
            assertThat(raw.getString("_class")).isEqualTo("project-file");
            assertThat(raw.get("content", org.bson.Document.class).getString("_class")).isIn("script", "render-config", "document");
        }
    }

    @Test void createRetriesNeverOverwriteExistingFilesEvenAfterChangingBack() throws Exception {
        var initial = Map.of("id", id, "name", " Script ", "content", script("1"));
        mvc.perform(body(post(base()), initial).with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        var original = files.findById(id).orElseThrow();
        mvc.perform(body(post(base()), initial).with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        assertThat(files.findById(id).orElseThrow()).isEqualTo(original);
        mvc.perform(body(post(base()), Map.of("id",id,"name","Script","content",script("2"))).with(user(member)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("file_already_exists"));
        update(id, 0, "Script", "", script("2"), 200);
        update(id, 1, "Script", "", script("1"), 200);
        mvc.perform(body(post(base()), initial).with(user(member))).andExpect(status().isConflict());
        assertThat(((FileContent.ScriptContent)files.findById(id).orElseThrow().content()).source()).isEqualTo("1");
    }

    @Test void lostUpdateResponsesCanBeRetriedWithoutOverwritingLaterChanges() throws Exception {
        create(id,"Script",script("1"));
        update(id,0,"Script","",script("2"),200);
        var saved = files.findById(id).orElseThrow();
        update(id,0,"Script","",script("2"),200);
        assertThat(files.findById(id).orElseThrow()).isEqualTo(saved);
        update(id,1,"Script","",script("3"),200);
        update(id,0,"Script","",script("2"),409);
        assertThat(files.findById(id).orElseThrow().version()).isEqualTo(2);
        update(id,0,"Script","changed description",script("3"),409);
        update(id,0,"Renamed","",script("3"),409);
        update(id,2,"Script","",config(),400);
    }

    @Test void identicalStateSucceedsWithStaleVersionAndDoesNotIncrementIt() throws Exception {
        create(id,"Script",script("1"));
        update(id,0,"Script","description",script("2"),200);
        var saved = files.findById(id).orElseThrow();
        update(id,0," Script ","description",script("2"),200);
        assertThat(files.findById(id).orElseThrow()).isEqualTo(saved);
    }

    @Test void competingWritersCannotLoseAnUpdate() throws Exception {
        create(id,"Script",script("initial"));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
            var tasks = List.of("first", "second").stream().map(source -> executor.submit(() -> {
                ready.countDown(); start.await();
                try {
                    service.update(project,id,new de.flogehring.peelserver.api.data.files.FileData.UpdateFile(0L,"Script","",new FileContent.ScriptContent(source)),member);
                    return "success";
                } catch (FileConflictException exception) { return exception.code(); }
            })).toList();
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(tasks.get(0).get(10,TimeUnit.SECONDS), tasks.get(1).get(10,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("success","file_version_conflict");
            assertThat(files.findById(id).orElseThrow().version()).isEqualTo(1);
        }
    }

    @Test void databaseCompareAndSetProtectsWritersWithIndependentProjectLocks() throws Exception {
        raceIndependentWriters("first", "second", List.of("success", "file_version_conflict"));
    }

    @Test void databaseRaceWithIdenticalTargetsSucceedsTwiceButWritesOnce() throws Exception {
        raceIndependentWriters("same", "same", List.of("success", "success"));
    }

    private void raceIndependentWriters(String first, String second, List<String> expected) throws Exception {
        create(id,"Script",script("initial"));
        var repository = org.mockito.Mockito.mock(ProjectFileRepository.class, org.mockito.AdditionalAnswers.delegatesTo(files));
        var barrier = new CyclicBarrier(2);
        var reads = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            var value = files.findById(id);
            if (reads.getAndIncrement() < 2) barrier.await(10, TimeUnit.SECONDS);
            return value;
        }).when(repository).findById(id);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = List.of(first, second).stream().map(source -> executor.submit(() -> {
                // Separate locks force the database CAS and its reread path to arbitrate the race.
                var independent = new ProjectFileService(repository, new ProjectAccessService(projects), mongo);
                try {
                    independent.update(project,id,new de.flogehring.peelserver.api.data.files.FileData.UpdateFile(0L,"Script","",new FileContent.ScriptContent(source)),member);
                    return "success";
                } catch (FileConflictException exception) { return exception.code(); }
            })).toList();
            assertThat(List.of(tasks.get(0).get(15,TimeUnit.SECONDS), tasks.get(1).get(15,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrderElementsOf(expected);
            assertThat(files.findById(id).orElseThrow().version()).isEqualTo(1);
        }
    }

    @Test void readsAliasesWithoutAnyPriorSaveOfTheContentType() throws Exception {
        mongo.getCollection("project-files").insertOne(new org.bson.Document("_id",id)
                .append("_class","project-file").append("projectId",new org.bson.Document("_id",project))
                .append("name","Raw script").append("description","").append("version",0L)
                .append("createdAt",new Date()).append("updatedAt",new Date())
                .append("content",new org.bson.Document("_class","script").append("source","42")));
        mvc.perform(get(base()+"/"+id).with(user(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.type").value("script")).andExpect(jsonPath("$.content.source").value("42"));
    }

    @Test void openApiUsesNonRecursiveDiscriminatedContentAndRemovesOldCrudRoutes() throws Exception {
        var result=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var contract=json.readTree(result.getResponse().getContentAsString());
        var schemas=contract.get("components").get("schemas");
        assertThat(schemas.has("ProjectFile")).isTrue();
        assertThat(schemas.has("FileSummary")).isTrue();
        assertThat(contract.get("paths").get("/api/projects/{projectId}/files/{fileId}").get("put")
                .get("responses").get("200").get("content").get("*/*").get("schema").get("$ref").asText())
                .isEqualTo("#/components/schemas/ProjectFile");
        assertThat(schemas.get("FileContent").get("oneOf").size()).isEqualTo(3);
        for(String type:List.of("ScriptContent","RenderConfigContent","DocumentContent")) {
            assertThat(schemas.get(type).has("allOf")).isFalse();
            assertThat(schemas.get(type).get("properties").get("type").get("enum").size()).isEqualTo(1);
        }
        assertThat(contract.get("paths").has("/api/scripts/{id}")).isFalse();
        assertThat(contract.get("paths").has("/api/documents/{id}")).isFalse();
        assertThat(contract.get("paths").has("/api/render-config/{id}")).isFalse();
    }

    @Test void namesAreUniqueWithinProjectAndTypeAndConflictsDoNotWrite() throws Exception {
        create(id,"Same",script("1"));
        mvc.perform(body(post(base()),Map.of("id",UUID.randomUUID(),"name","Same","content",script("2"))).with(user(owner)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("file_name_conflict"));
        String second=UUID.randomUUID().toString(); create(second,"Other",script("2"));
        update(second,0,"Same","",script("2"),409);
        assertThat(files.findById(second).orElseThrow().version()).isZero();
        create(UUID.randomUUID().toString(),"Same",config());
    }

    @Test void deletingUsesVersionAndReportsReferencesAndIsRepeatable() throws Exception {
        create(id,"Script",script("1"));
        String config=UUID.randomUUID().toString(); create(config,"Config",config());
        String doc=UUID.randomUUID().toString(); create(doc,"Document",document(id,config));
        for(String target:List.of(id,config)) mvc.perform(delete(base()+"/"+target).param("expectedVersion","0").with(user(member)).with(csrf()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("file_in_use"))
                .andExpect(jsonPath("$.error.details.usages[0].id").value(doc));
        update(doc,0,"Document","updated",document(id,config),200);
        mvc.perform(delete(base()+"/"+doc).param("expectedVersion","0").with(user(member)).with(csrf())).andExpect(status().isConflict());
        for(int i=0;i<2;i++) mvc.perform(delete(base()+"/"+doc).param("expectedVersion","1").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete(base()+"/"+id).param("expectedVersion","0").with(user(member)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete(base()+"/"+config).param("expectedVersion","0").with(user(member)).with(csrf())).andExpect(status().isNoContent());
    }

    @Test void rejectsForeignAccessAndCrossProjectReferencesAndWrongTypes() throws Exception {
        create(id,"Script",script("1"));
        mvc.perform(get(base()).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(get(base()+"/"+id).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(body(put(base()+"/"+id),Map.of("expectedVersion",0,"name","Script","content",script("2"))).with(user(outsider))).andExpect(status().isNotFound());
        mvc.perform(delete(base()+"/"+id).param("expectedVersion","0").with(user(outsider)).with(csrf())).andExpect(status().isNotFound());
        String config=UUID.randomUUID().toString(); create(config,"Config",config());
        String other=UUID.randomUUID().toString(); projects.insert(new ProjectPersistence(other,new ProjectPersistenceData(owner.getUserId(),List.of(),"Other","")));
        String foreign=UUID.randomUUID().toString();
        mvc.perform(body(post("/api/projects/"+other+"/files"),Map.of("id",foreign,"name","Foreign","content",script("1"))).with(user(owner))).andExpect(status().isOk());
        for(var content:List.of(document(foreign,config),document("missing",config)))
            mvc.perform(body(post(base()),Map.of("id",UUID.randomUUID(),"name","Document","content",content)).with(user(owner))).andExpect(status().isNotFound());
        mvc.perform(body(post(base()),Map.of("id",UUID.randomUUID(),"name","Document","content",document(config,id))).with(user(owner))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/projects/"+other+"/files"),Map.of("id",id,"name","Script","content",script("1"))).with(user(owner))).andExpect(status().isNotFound());
    }

    @Test void validatesBodiesAndRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
        mvc.perform(post(base()).with(user(owner)).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
        for(var invalid:List.of(Map.of("id",id,"name"," ","content",script("")),Map.of("id","bad-id","name","Script","content",script("")),
                Map.of("id",id,"name","Script","content",Map.of("type","unknown")),Map.of("id",id,"name","Script","content",Map.of("type","script"))))
            mvc.perform(body(post(base()),invalid).with(user(owner))).andExpect(status().isBadRequest());
        create(id,"Script",script(""));
        mvc.perform(body(put(base()+"/"+id),Map.of("name","Script","content",script(""))).with(user(owner))).andExpect(status().isBadRequest());
        update(id,-1,"Script","",script(""),400);
        mvc.perform(delete(base()+"/"+id).param("expectedVersion","-1").with(user(owner)).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test void archivedProjectsRemainReadableButRejectEvenNoopWrites() throws Exception {
        create(id,"Script",script("1"));
        mvc.perform(post("/api/projects/"+project+"/archive").with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(base()).with(user(member))).andExpect(status().isOk());
        mvc.perform(get(base()+"/"+id).with(user(member))).andExpect(status().isOk());
        update(id,0,"Script","",script("1"),409);
        mvc.perform(body(post(base()),Map.of("id",id,"name","Script","content",script("1"))).with(user(owner))).andExpect(status().isConflict());
        mvc.perform(delete(base()+"/"+id).param("expectedVersion","0").with(user(owner)).with(csrf())).andExpect(status().isConflict());
    }

    private String base(){return "/api/projects/"+project+"/files";}
    private Map<String,Object> script(String source){return Map.of("type","script","source",source);}
    private Map<String,Object> config(){return Map.of("type","render-config","renderConfigurations",Map.of(),"namedOverrides",Map.of());}
    private Map<String,Object> document(String script,String config){return Map.of("type","document","templateHtml","<p>Test</p>","editorStateJson","{}","scriptNameTags",Map.of("calculation",script),"renderConfigurationId",config);}
    private void create(String id,String name,Object content)throws Exception{
        mvc.perform(body(post(base()),Map.of("id",id,"name",name,"content",content)).with(user(member))).andExpect(status().isOk());
    }
    private void update(String id,long version,String name,String description,Object content,int status)throws Exception{
        mvc.perform(body(put(base()+"/"+id),Map.of("expectedVersion",version,"name",name,"description",description,"content",content)).with(user(member)))
                .andExpect(status().is(status));
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,Object body)throws Exception{
        return request.with(csrf()).contentType("application/json").content(json.writeValueAsString(body));
    }
    private SpringUserDetails principal(String name){return new SpringUserDetails(new UserId(project+name),new UserPersistenceData(project+name+"@test.example",null));}
}
