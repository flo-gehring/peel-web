package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.data.auth.AuthLinkRequest;
import de.flogehring.peelserver.api.data.auth.CsrfResponse;
import de.flogehring.peelserver.api.services.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthService implements AuthController {
    private final AuthLinkService links;
    @Override
    public void sendLink(AuthLinkRequest body, HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            throw new AuthRequestException(409, "already_authenticated", "Sign out before signing in to another account.");
        }
        // Forwarded headers require an explicitly trusted deployment proxy.
        links.send(body, request.getRemoteAddr());
    }
    @Override
    public CsrfResponse csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }
}
