package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.auth.AuthLinkRequest;
import de.flogehring.peelserver.api.data.auth.CsrfResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping(path = "/api/auth", produces = "application/json")
public interface AuthController {
    @PostMapping("/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendLink(@RequestBody AuthLinkRequest body, HttpServletRequest request);

    @GetMapping("/csrf")
    CsrfResponse csrf(HttpServletRequest request);
}
