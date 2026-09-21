package de.flogehring.peelserver.impl.users;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class SpringUserDetails implements UserDetails {

    private final UserId userId;
    private final UserPersistenceData userPersistenceData;

    public SpringUserDetails(UserId userId, UserPersistenceData userPersistenceData) {
        this.userId = userId;
        this.userPersistenceData = userPersistenceData;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userPersistenceData.eMail();
    }
}
