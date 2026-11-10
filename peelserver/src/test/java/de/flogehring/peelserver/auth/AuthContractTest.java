package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.impl.auth.AuthOpenApiConfiguration;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class AuthContractTest {
    @Test
    void exportsAuthenticationContractIncludingFilterEndpoints() throws Exception {
        var api = new AuthOpenApiConfiguration().authApi();
        assertThat(api.getPaths()).containsKeys("/api/auth/login", "/api/auth/logout", "/api/auth/link", "/api/auth/csrf", "/api/whoami");
        Files.writeString(Path.of("build/auth-openapi.json"), Json.pretty(api));
    }
}
