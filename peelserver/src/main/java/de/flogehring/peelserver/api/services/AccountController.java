package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.auth.WhoAmIResponse;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/api", produces = "application/json")
public interface AccountController {

    @GetMapping("/whoami")
    WhoAmIResponse whoAmI(@AuthenticationPrincipal SpringUserDetails springUserDetails);
}
