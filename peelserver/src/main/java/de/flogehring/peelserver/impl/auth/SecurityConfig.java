package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.error.ApiErrorResponse;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ott.InMemoryOneTimeTokenService;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ott.OneTimeTokenAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    OutboundMailBudget outboundMailBudget(@Value("${peel.auth.mail-requests-per-hour:100}") int hourlyLimit) {
        return new OutboundMailBudget(hourlyLimit);
    }

    @Bean
    LoginAllowlist loginAllowlist(@Value("${peel.auth.allowlist-file:}") String file,
                                  AuthProperties properties, Environment environment) {
        if (environment.matchesProfiles("prod")) {
            if (file.isBlank()) throw new IllegalStateException("PEEL_AUTH_ALLOWLIST_FILE is required in production");
            if (environment.matchesProfiles("dev")
                    || !"https".equals(properties.publicBaseUrl().getScheme())
                    || !environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)
                    || !environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class, false)) {
                throw new IllegalStateException("Production authentication requires HTTPS, secure HTTP-only cookies and no dev profile");
            }
            for (String key : new String[]{"spring.mail.host", "spring.mail.username", "spring.mail.password"}) {
                if (environment.getRequiredProperty(key).isBlank()) {
                    throw new IllegalStateException("Missing production relay setting: " + key);
                }
            }
        }
        return new LoginAllowlist(file);
    }

    @Bean
    OneTimeTokenService oneTimeTokenService(LoginAllowlist allowlist) {
        var tokens = new InMemoryOneTimeTokenService();
        return new OneTimeTokenService() {
            @Override
            public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
                return tokens.generate(request);
            }

            @Override
            public OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken) {
                var token = tokens.consume(authenticationToken);
                return token != null && allowlist.permits(token.getUsername()) ? token : null;
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OneTimeTokenService tokens, LoginAllowlist allowlist,
                                           ObjectMapper mapper) throws Exception {
        // Reject account switches before the authentication filter can clear an existing session on failure.
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    HttpServletResponse response,
                    jakarta.servlet.FilterChain chain
            ) throws jakarta.servlet.ServletException, IOException {
                var current = SecurityContextHolder.getContext().getAuthentication();
                if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                        && current != null && current.getPrincipal() instanceof SpringUserDetails user
                        && !allowlist.permits(user.getUsername())) {
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    SecurityContextHolder.clearContext();
                    current = null;
                }
                if (request.getMethod().equals("POST") && request.getRequestURI().equals(request.getContextPath() + "/api/auth/login")
                        && current != null && current.isAuthenticated() && !(current instanceof AnonymousAuthenticationToken)) {
                    writeError(mapper, response, 409, "already_authenticated", "Sign out before signing in to another account.");
                    return;
                }
                chain.doFilter(request, response);
            }
        }, OneTimeTokenAuthenticationFilter.class);
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/whoami", "/api/auth/csrf", "/api/auth/link", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(mapper, response, 401, "unauthenticated", "Please sign in to continue."))
                        .accessDeniedHandler((request, response, exception) -> writeError(mapper, response, 403,
                                exception instanceof CsrfException ? "csrf_invalid" : "forbidden",
                                exception instanceof CsrfException ? "The request could not be verified. Please try again." : "Access denied.")))
                .oneTimeTokenLogin(ott -> ott.loginPage("/login").showDefaultSubmitPage(false).tokenService(tokens)
                        .tokenGeneratingUrl("/api/auth/link")
                        // The JSON controller owns validation and delivery; Spring still generates and consumes tokens.
                        .generateRequestResolver(request -> null)
                        .tokenGenerationSuccessHandler((request, response, token) -> response.setStatus(204))
                        .loginProcessingUrl("/api/auth/login")
                        .authenticationConverter(request -> {
                            String token = request.getParameter("token");
                            if (token == null || token.isBlank()) throw new BadCredentialsException("Missing token");
                            return new OneTimeTokenAuthenticationToken(token);
                        })
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof InternalAuthenticationServiceException) {
                                writeError(mapper, response, 503, "auth_unavailable", "Sign-in is temporarily unavailable. Please request a new link later.");
                            } else {
                                writeError(mapper, response, 401, "invalid_link", "This sign-in link is no longer valid. Request a new one.");
                            }
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }

    private static void writeError(
            ObjectMapper mapper,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), ApiErrorResponse.of(code, message));
    }
}
