package de.flogehring.peelserver.impl.files;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class FileOpenApiConfiguration {
    @Bean
    OpenApiCustomizer fileContentSchemas() {
        return api -> {
            var schemas = api.getComponents().getSchemas();
            // Swagger models Java inheritance with allOf. With a sealed oneOf base this creates
            // recursive schemas; variants must instead be independent objects with a literal type.
            Map.of("ScriptContent", "script", "RenderConfigContent", "render-config", "DocumentContent", "document")
                    .forEach((name, type) -> {
                        io.swagger.v3.oas.models.media.Schema<?> generated = schemas.get(name);
                        if (generated == null) return;
                        var variant = new ObjectSchema();
                        if (generated.getProperties() != null) variant.setProperties(generated.getProperties());
                        if (generated.getAllOf() != null) generated.getAllOf().forEach(rawPart -> {
                            io.swagger.v3.oas.models.media.Schema<?> part = rawPart;
                            if (part.getProperties() != null) part.getProperties().forEach(variant::addProperty);
                        });
                        variant.setRequired(generated.getRequired());
                        variant.addProperty("type", new StringSchema()._enum(java.util.List.of(type)));
                        variant.addRequiredItem("type");
                        schemas.put(name, variant);
                    });
        };
    }
}
