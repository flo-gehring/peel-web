package de.flogehring.peelserver.run;

import de.flogehring.peelserver.impl.run.*;
import de.flogehring.peelserver.impl.scripts.PeelScriptService;
import de.flogehring.peelserver.impl.auth.SecurityConfig;
import de.flogehring.peelserver.impl.users.*;
import de.flogehring.peelserver.impl.project.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.function.Supplier;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PeelScriptService.class, properties = {
        "peel.auth.public-base-url=http://localhost:5173", "peel.auth.mail-from=noreply@peel.test"
})
@Import({SecurityConfig.class, AnalysisService.class, RunService.class, PeelExecutionService.class, BindingsMapper.class})
class ScriptLanguageApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProjectAccessService access;
    @MockitoBean SpringUserDetailService users;
    final SpringUserDetails principal = new SpringUserDetails(new UserId("user"), new UserPersistenceData("user@example.test", "User"));

    @BeforeEach void setup() {
        when(access.active(any(), any(), any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(2)).get());
    }
    @Test void analyzesDraftsWithExternalNamesAndAllowsEmptySource() throws Exception {
        mvc.perform(post("/api/scripts/analyze").with(user(principal)).with(csrf()).contentType("application/json")
                .content("""
                        {"projectId":{"id":"project"},"script":"amount + 1;","expectedBindings":[{"name":"amount","description":"Input"}]}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.diagnostics").isEmpty())
                .andExpect(jsonPath("$.symbols[?(@.name=='amount')].origin").value("external"));
        verify(access).requireProjectAccess(new ProjectId("project"), principal.getUserId());
        mvc.perform(post("/api/scripts/analyze").with(user(principal)).with(csrf()).contentType("application/json")
                .content("""
                        {"projectId":{"id":"project"},"script":"","expectedBindings":[]}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.diagnostics").isEmpty());
    }
    @Test void enforcesProjectAccessAndAuthenticationForAnalysis() throws Exception {
        var body = "{\"projectId\":{\"id\":\"foreign\"},\"script\":\"\",\"expectedBindings\":[]}";
        mvc.perform(post("/api/scripts/analyze").with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        doThrow(ProjectAccessService.notFound()).when(access).requireProjectAccess(new ProjectId("foreign"), principal.getUserId());
        mvc.perform(post("/api/scripts/analyze").with(user(principal)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isNotFound());
    }
    @Test void checksRequiredBindingsAndExecutesTheProvidedDraft() throws Exception {
        var body = """
                {"projectId":{"id":"project"},"script":"amount * 2;","expectedBindings":[{"name":"amount","description":""}],"bindings":%s}
                """;
        mvc.perform(put("/api/scripts/run").with(user(principal)).with(csrf()).contentType("application/json")
                .content(body.formatted("{}"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("missing_bindings"))
                .andExpect(jsonPath("$.error.details.missingBindings[0]").value("amount"));
        mvc.perform(put("/api/scripts/run").with(user(principal)).with(csrf()).contentType("application/json")
                .content(body.formatted("{\"amount\":5}"))).andExpect(status().isOk());
    }
}
