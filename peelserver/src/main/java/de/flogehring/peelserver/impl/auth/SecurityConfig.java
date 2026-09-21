package de.flogehring.peelserver.impl.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            MagicLinkOneTimeTokenGenerationSuccessHandler magicLinkHandler
    ) throws Exception {
        http.formLogin(Customizer.withDefaults())
                .oneTimeTokenLogin(oneTimeTokenLogin -> oneTimeTokenLogin
                        .tokenGenerationSuccessHandler(magicLinkHandler));
        return http.build();
    }
}
