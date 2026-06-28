package de.flogehring.peelserver.projects;

public class ProjectAlreadyExistsException extends RuntimeException {

    public ProjectAlreadyExistsException(String projectId) {
        super("Project with id '" + projectId + "' already exists.");
    }
}
