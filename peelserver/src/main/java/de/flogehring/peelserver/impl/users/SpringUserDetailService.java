package de.flogehring.peelserver.impl.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

@Service
@RequiredArgsConstructor
public class SpringUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return loadOrCreateVerifiedUser(username);
        } catch (DataAccessException exception) {
            throw new InternalAuthenticationServiceException("Account service unavailable", exception);
        }
    }

    private UserDetails loadOrCreateVerifiedUser(String username) {
        // Only the OTT provider calls this service, after consuming a verified token.
        UserPersistence userPersistence = userRepository.findByUsername(username);
        if (userPersistence == null) {
            try {
                userPersistence = userRepository.insert(new UserPersistence(null, new UserPersistenceData(username, null)));
            } catch (DuplicateKeyException exception) {
                userPersistence = userRepository.findByUsername(username);
                if (userPersistence == null) throw new UsernameNotFoundException("Account could not be loaded", exception);
            }
        }
        return new SpringUserDetails(new UserId(userPersistence.getId()), userPersistence.getUserPersistenceData());
    }
}
