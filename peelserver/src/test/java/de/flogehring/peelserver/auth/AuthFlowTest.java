package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.impl.auth.*;
import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthService.class, AccountService.class}, properties = {
        "peel.auth.public-base-url=http://localhost:5173", "peel.auth.mail-from=noreply@peel.test"
})
@Import({SecurityConfig.class, AuthLinkService.class, SpringUserDetailService.class})
class AuthFlowTest {
    @Autowired MockMvc mvc;
    @Autowired OneTimeTokenService tokens;
    @MockitoBean UserRepository users;
    @MockitoBean MailSender mail;

    @BeforeEach
    void setUp() {
        when(users.findByUsername(anyString())).thenReturn(null);
        when(users.insert(any(UserPersistence.class))).thenAnswer(invocation -> {
            UserPersistence user = invocation.getArgument(0);
            return new UserPersistence("user-1", user.getUserPersistenceData());
        });
    }

    @Test
    void sendsLinkWithoutCreatingAccountAndLogsInOnlyOnce() throws Exception {
        mvc.perform(post("/api/auth/link").with(csrf()).contentType("application/json")
                        .content("{\"email\":\" Test@Example.test \",\"redirect\":\"/projects/p1\"}"))
                .andExpect(status().isNoContent());
        verify(users, never()).insert(any(UserPersistence.class));
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        String body = message.getValue().getText();
        assertThat(body).contains("http://localhost:5173/login/confirm?token=", "redirect=");
        String token = body.split("token=")[1].split("[&\\s]")[0];
        var login = mvc.perform(post("/api/auth/login").with(csrf()).param("token", token))
                .andExpect(status().isNoContent()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/whoami").session(session))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.userId").value("user-1"));
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).param("token", "another-token"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/whoami").session(session))
                .andExpect(jsonPath("$.authenticated").value(true));
        mvc.perform(post("/api/auth/login").with(csrf()).param("token", token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("invalid_link"));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/whoami"))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void rejectsMissingExpiredAndInvalidTokensWithoutCreatingAccounts() throws Exception {
        String expired = tokens.generate(new GenerateOneTimeTokenRequest("expired@example.test", Duration.ofSeconds(-1))).getTokenValue();
        for (String token : new String[]{"", "invalid", expired}) {
            mvc.perform(post("/api/auth/login").with(csrf()).param("token", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("invalid_link"));
        }
        verify(users, never()).insert(any(UserPersistence.class));
    }

    @Test
    void requiresCsrfAndReturnsJsonInsteadOfRedirects() throws Exception {
        mvc.perform(post("/api/auth/link").contentType("application/json").content("{\"email\":\"a@example.test\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("csrf_invalid"));
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("unauthenticated"));
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void acceptsRealSessionCsrfAndRefreshesItAfterLogin() throws Exception {
        var bootstrap = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var mapper = new tools.jackson.databind.json.JsonMapper();
        String token = mapper.readTree(bootstrap.getResponse().getContentAsString()).get("token").asText();
        MockHttpSession session = (MockHttpSession) bootstrap.getRequest().getSession(false);
        String loginToken = tokens.generate(new GenerateOneTimeTokenRequest("csrf@example.test")).getTokenValue();
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token).param("token", loginToken))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isForbidden());
        var fresh = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        String freshToken = mapper.readTree(fresh.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", freshToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void validatesAddressHandlesMailFailuresAndLimitsResends() throws Exception {
        mvc.perform(post("/api/auth/link").with(csrf()).contentType("application/json").content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post("/api/auth/link").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"failure@example.test\",\"redirect\":\"https://evil.example\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("mail_unavailable"));
        var failedMail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(failedMail.capture());
        assertThat(failedMail.getValue().getText()).doesNotContain("evil.example");
        String failedToken = failedMail.getValue().getText().split("token=")[1].split("[&\\s]")[0];
        assertThat(tokens.consume(new org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken(failedToken))).isNull();
        mvc.perform(post("/api/auth/link").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"failure@example.test\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }

    @Test
    void reportsAccountDatabaseFailureAsServiceUnavailable() throws Exception {
        when(users.findByUsername("database@example.test")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        String token = tokens.generate(new GenerateOneTimeTokenRequest("database@example.test")).getTokenValue();
        mvc.perform(post("/api/auth/login").with(csrf()).param("token", token))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("auth_unavailable"));
    }
}
