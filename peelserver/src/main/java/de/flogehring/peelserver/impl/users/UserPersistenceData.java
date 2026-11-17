package de.flogehring.peelserver.impl.users;

import com.mongodb.lang.Nullable;

public record UserPersistenceData(
        String eMail,
        @Nullable String name
) {
}
