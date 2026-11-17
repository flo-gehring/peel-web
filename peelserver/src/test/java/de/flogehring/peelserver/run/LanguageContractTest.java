package de.flogehring.peelserver.run;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false", "spring.mail.host=localhost",
        "peel.auth.public-base-url=http://localhost:5173", "peel.auth.mail-from=noreply@peel.test",
        "minio.url=http://localhost:9000", "minio.access.name=testing", "minio.access.secret=testing-secret"
})
@AutoConfigureMockMvc
class LanguageContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean(name = "projectAccessIndexes") InitializingBean projectIndexes;
    @MockitoBean(name = "userEmailIndex") InitializingBean userIndex;

    @Test void exportsFullContractWithAnalysisAndRequiredInputs() throws Exception {
        var principal = new de.flogehring.peelserver.impl.users.SpringUserDetails(
                new de.flogehring.peelserver.impl.users.UserId("language-contract"),
                new de.flogehring.peelserver.impl.users.UserPersistenceData("contract@example.test", null));
        var response = mvc.perform(get("/v3/api-docs")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/scripts/analyze'].post").exists())
                .andExpect(jsonPath("$.paths['/api/scripts/validate']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ScriptContent.properties.expectedBindings").exists())
                .andExpect(jsonPath("$.components.schemas.RunRequest.properties.expectedBindings").exists())
                .andExpect(jsonPath("$.components.schemas.DocumentContent.properties.document").exists())
                .andExpect(jsonPath("$.components.schemas.DocumentContent.properties.templateHtml").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/files/{fileId}/blocks'].get").exists())
                .andReturn().getResponse().getContentAsString();
        Files.writeString(Path.of("build/openapi.json"), response);
    }
}
