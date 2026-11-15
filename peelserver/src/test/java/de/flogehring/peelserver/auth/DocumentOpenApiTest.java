package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PeelModuleTest @AutoConfigureMockMvc
class DocumentOpenApiTest {
    @Autowired MockMvc mvc;
    @Test void exportsTheStructuredDocumentContract() throws Exception {
        var user = new SpringUserDetails(new UserId("contract"), new UserPersistenceData("contract@example.test", null));
        var result = mvc.perform(get("/v3/api-docs").with(user(user))).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.DocumentContent.properties.document").exists())
                .andExpect(jsonPath("$.components.schemas.DocumentContent.properties.templateHtml").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.DocumentNode.properties.content").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/files/{fileId}/blocks'].get").exists())
                .andExpect(jsonPath("$.components.schemas.BlockDefinition.properties.reachableBlocks").exists())
                .andReturn();
        Files.writeString(Path.of("build/openapi.json"), result.getResponse().getContentAsString());
    }
}
