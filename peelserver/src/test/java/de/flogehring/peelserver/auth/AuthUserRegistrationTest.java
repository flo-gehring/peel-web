package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.impl.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthUserRegistrationTest {
    @Test
    void reusesTheAccountCreatedByAConcurrentVerifiedSignIn() {
        UserRepository users = mock(UserRepository.class);
        var existing = new UserPersistence("existing-user", new UserPersistenceData("one@example.test", null));
        when(users.findByUsername("one@example.test")).thenReturn(null, existing);
        when(users.insert(any(UserPersistence.class))).thenThrow(new DuplicateKeyException("duplicate email"));
        var result = (SpringUserDetails) new SpringUserDetailService(users).loadUserByUsername("one@example.test");
        assertThat(result.getUserId().id()).isEqualTo("existing-user");
    }
}
