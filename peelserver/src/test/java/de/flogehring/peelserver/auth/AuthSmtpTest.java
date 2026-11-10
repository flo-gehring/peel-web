package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.api.data.auth.AuthLinkRequest;
import de.flogehring.peelserver.impl.auth.AuthLinkService;
import de.flogehring.peelserver.impl.auth.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.ott.InMemoryOneTimeTokenService;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class AuthSmtpTest {
    @Test
    void deliversTheSignInEmailToTheDevelopmentSmtpServer() throws Exception {
        try (var mailpit = new GenericContainer<>("axllent/mailpit:v1.31.4").withExposedPorts(1025, 8025)) {
            mailpit.start();
            var sender = new JavaMailSenderImpl();
            sender.setHost(mailpit.getHost());
            sender.setPort(mailpit.getMappedPort(1025));
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "5000");
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
            sender.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "5000");
            var links = new AuthLinkService(new InMemoryOneTimeTokenService(), sender,
                    new AuthProperties(URI.create("http://localhost:5173"), "noreply@peel.test",
                            Duration.ofMinutes(15), Duration.ofSeconds(60), 20));
            links.send(new AuthLinkRequest("developer@example.test", "/projects/one"), "127.0.0.1");
            try (var client = HttpClient.newHttpClient()) {
                var request = HttpRequest.newBuilder(URI.create("http://" + mailpit.getHost() + ":"
                        + mailpit.getMappedPort(8025) + "/api/v1/message/latest"))
                        .timeout(Duration.ofSeconds(10)).build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("developer@example.test", "noreply@peel.test",
                        "Sign in to PEEL WEB", "http://localhost:5173/login/confirm?token=", "15 minutes");
            }
        }
    }
}
