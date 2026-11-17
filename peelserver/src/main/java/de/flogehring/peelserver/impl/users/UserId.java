package de.flogehring.peelserver.impl.users;

public record UserId(String id) {
    public static final UserId PLACEHOLDER_UNTIL_PROPER_IMPL = new UserId("placeholder");


}
