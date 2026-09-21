package de.flogehring.peelserver.impl.users;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends MongoRepository<UserPersistence, String> {

    @Query("'userPersistenceData.eMail' : ?0")
    UserPersistence findByUsername(String username);
}
