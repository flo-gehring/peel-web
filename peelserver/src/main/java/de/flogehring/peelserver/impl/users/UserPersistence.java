package de.flogehring.peelserver.impl.users;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Indexed;

@Document("users")
@Getter
public class UserPersistence {


    @Id
    private String id;
    private UserPersistenceData userPersistenceData;

    public UserPersistence(String id, UserPersistenceData userPersistenceData) {
        this.id = id;
        this.userPersistenceData = userPersistenceData;
    }
}
