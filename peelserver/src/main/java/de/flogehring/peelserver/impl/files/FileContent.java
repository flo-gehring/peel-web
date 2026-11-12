package de.flogehring.peelserver.impl.files;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.flogehring.peel.core.trace.TraceExpressionKind;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.TypeAlias;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FileContent.ScriptContent.class, name = "script"),
    @JsonSubTypes.Type(value = FileContent.RenderConfigContent.class, name = "render-config"),
    @JsonSubTypes.Type(value = FileContent.DocumentContent.class, name = "document")
})
@Schema(oneOf = {FileContent.ScriptContent.class, FileContent.RenderConfigContent.class, FileContent.DocumentContent.class},
        discriminatorProperty = "type", discriminatorMapping = {
        @DiscriminatorMapping(value = "script", schema = FileContent.ScriptContent.class),
        @DiscriminatorMapping(value = "render-config", schema = FileContent.RenderConfigContent.class),
        @DiscriminatorMapping(value = "document", schema = FileContent.DocumentContent.class)})
public sealed interface FileContent {
    default String kind() {
        return switch (this) {
            case ScriptContent ignored -> "script";
            case RenderConfigContent ignored -> "render-config";
            case DocumentContent ignored -> "document";
        };
    }

    @TypeAlias("script")
    record ScriptContent(@NotNull String source) implements FileContent {}

    @TypeAlias("render-config")
    record RenderConfigContent(
            @NotNull Map<TraceExpressionKind, @NotNull String> renderConfigurations,
            @NotNull Map<String, @NotNull Map<TraceExpressionKind, @NotNull String>> namedOverrides
    ) implements FileContent {
        public ExpressionRenderConfiguration configuration() {
            return ExpressionRenderConfiguration.of(renderConfigurations, namedOverrides);
        }
    }

    @TypeAlias("document")
    record DocumentContent(
            @NotNull Map<@NotBlank String, @NotBlank String> scriptNameTags,
            @NotBlank String templateHtml,
            @NotNull String editorStateJson,
            @NotBlank String renderConfigurationId
    ) implements FileContent {}
}
