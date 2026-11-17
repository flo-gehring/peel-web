package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.data.auth.WhoAmIResponse;
import de.flogehring.peelserver.api.services.AccountController;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountService implements AccountController {

    @Override
    public WhoAmIResponse whoAmI(@AuthenticationPrincipal @Nullable SpringUserDetails springUserDetails) {
        if (springUserDetails == null) {
            return new WhoAmIResponse(false, null, null);
        }
        return new WhoAmIResponse(
                true,
                springUserDetails.getUserId().id(),
                springUserDetails.getUsername()
        );
    }
}
