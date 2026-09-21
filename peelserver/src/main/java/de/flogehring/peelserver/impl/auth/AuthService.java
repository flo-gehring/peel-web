package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.services.AuthController;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthService implements AuthController {

    @Override
    public String ottSent() {
        return "{ \"message\": \"OTT sent successfully!\" }";
    }
}
