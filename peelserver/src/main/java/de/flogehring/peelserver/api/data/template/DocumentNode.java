package de.flogehring.peelserver.api.data.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.*;

public record DocumentNode(
        @NotBlank String type,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> attrs,
        List<DocumentNode> content,
        String text,
        List<Mark> marks
) {
    public record Mark(
            @NotBlank String type,
            @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> attrs
    ) {
    }

    public DocumentNode {
        attrs = attrs == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attrs));
        content = content == null ? List.of() : List.copyOf(content);
        marks = marks == null ? List.of() : List.copyOf(marks);
    }

    public String attribute(String name) {
        Object value = attrs.get(name);
        return value == null ? "" : String.valueOf(value);
    }

    public Set<String> scriptIds() {
        Set<String> ids = new LinkedHashSet<>();
        collectScripts(ids);
        return ids;
    }

    private void collectScripts(Set<String> ids) {
        if (Set.of("peelCalculation", "peelValue", "peelRepeat", "peelTable").contains(type) && attribute("scope").equals("script")
                && !attribute("scriptId").isBlank()) ids.add(attribute("scriptId"));
        content.forEach(child -> child.collectScripts(ids));
    }

    public void validateDocument() {
        if (!"doc".equals(type)) throw new IllegalArgumentException("The document root must be a doc node");
        validate(false, false, 0, "");
    }

    private void validate(boolean inRepeat, boolean inlineParent, int depth, String parent) {
        if (depth > 50) throw new IllegalArgumentException("Document nesting is too deep");
        if (type == null || !Set.of("doc", "paragraph", "heading", "text", "bulletList", "orderedList",
                "listItem", "blockquote", "codeBlock", "hardBreak", "horizontalRule",
                "peelCalculation", "peelRepeat", "peelValue", "peelTable", "peelTableColumn").contains(type))
            throw new IllegalArgumentException("Unsupported document node: " + type);
        if (depth > 0 && "doc".equals(type)) throw new IllegalArgumentException("Nested document roots are invalid");
        if (type.equals("peelTableColumn")) {
            if (!parent.equals("peelTable")) throw new IllegalArgumentException("Table columns require a table parent");
            if (content.isEmpty() || content.stream().anyMatch(child -> !Set.of("paragraph", "peelCalculation").contains(child.type())))
                throw new IllegalArgumentException("Table cells require paragraphs or calculations");
            if (attrs.get("title") != null && !(attrs.get("title") instanceof String))
                throw new IllegalArgumentException("Table headings must be text");
            if (!attribute("align").isBlank() && !Set.of("left", "center", "right").contains(attribute("align")))
                throw new IllegalArgumentException("Unknown table column alignment");
        }
        if (type.equals("peelTable") && (content.isEmpty() || content.stream().anyMatch(child -> !"peelTableColumn".equals(child.type()))))
            throw new IllegalArgumentException("Tables require one or more columns");
        boolean inline = Set.of("text", "hardBreak", "peelValue").contains(type);
        if (depth > 0 && inline != inlineParent) throw new IllegalArgumentException("Invalid placement of " + type);
        if ("text".equals(type) && text == null) throw new IllegalArgumentException("Text nodes need text");
        if (inline || Set.of("horizontalRule", "peelCalculation").contains(type)) {
            if (!content.isEmpty()) throw new IllegalArgumentException(type + " cannot contain child nodes");
        }
        for (Mark mark : marks) {
            if (mark == null || mark.type() == null || !Set.of("bold", "italic", "strike", "code", "underline", "link").contains(mark.type()))
                throw new IllegalArgumentException("Unsupported text mark");
        }
        if (Set.of("peelCalculation", "peelRepeat", "peelValue", "peelTable").contains(type)) {
            for (String attribute : List.of("scope", "scriptId", "block", "mode", "binding")) {
                if (attrs.get(attribute) != null && !(attrs.get(attribute) instanceof String))
                    throw new IllegalArgumentException("Invalid trace attribute: " + attribute);
            }
            Object decimals = attrs.get("decimals");
            if (decimals != null && (!(decimals instanceof Number number) || number.doubleValue() != number.intValue()
                    || number.intValue() < 0 || number.intValue() > 12))
                throw new IllegalArgumentException("Decimal places must be an integer between 0 and 12");
            String scope = attribute("scope");
            if (!Set.of("script", "current").contains(scope))
                throw new IllegalArgumentException("Choose a trace scope");
            if (scope.equals("current") && !inRepeat)
                throw new IllegalArgumentException("Current trace scope requires a repetition");
            if (scope.equals("script") && attribute("scriptId").isBlank())
                throw new IllegalArgumentException("Choose a script for " + type);
            if (type.equals("peelCalculation") && !Set.of("detailed", "compact", "substituted", "result").contains(attribute("mode")))
                throw new IllegalArgumentException("Unknown calculation display mode");
        }
        boolean childrenInline = Set.of("paragraph", "heading", "codeBlock").contains(type);
        for (DocumentNode child : content) {
            if (Set.of("bulletList", "orderedList").contains(type) && !"listItem".equals(child.type()))
                throw new IllegalArgumentException("Lists must contain list items");
            child.validate(inRepeat || type.equals("peelRepeat") || type.equals("peelTable"), childrenInline, depth + 1, type);
        }
    }
}
