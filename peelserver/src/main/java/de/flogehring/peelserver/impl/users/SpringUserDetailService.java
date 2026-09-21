package de.flogehring.peelserver.impl.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SpringUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserPersistence userPersistence = Objects.requireNonNull(userRepository.findByUsername(username), () -> "User not found: " + username);
        return new SpringUserDetails(new UserId(userPersistence.getId()), userPersistence.getUserPersistenceData());
    }
}
