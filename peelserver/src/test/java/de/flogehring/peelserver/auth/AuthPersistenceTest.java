package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@PeelModuleTest
class AuthPersistenceTest {
    @Autowired SpringUserDetailService accounts;
    @Autowired UserRepository users;

    @Test
    void createsFindsAndReusesAnAccountWithAUniqueEmailIndex() {
        String email = UUID.randomUUID() + "@example.test";
        var first = (SpringUserDetails) accounts.loadUserByUsername(email);
        var again = (SpringUserDetails) accounts.loadUserByUsername(email);
        assertThat(again.getUserId()).isEqualTo(first.getUserId());
        assertThat(users.findByUsername(email).getId()).isEqualTo(first.getUserId().id());
        assertThatThrownBy(() -> users.insert(new UserPersistence(null, new UserPersistenceData(email, null))))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
