package de.flogehring.peelserver.impl.run;

import java.util.Map;

public final class BindingInputException extends IllegalArgumentException {
    private final String code;
    private final Map<String, Object> details;
    public BindingInputException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }
    public String code() { return code; }
    public Map<String, Object> details() { return details; }
}
