package de.flogehring.peelserver.impl.renderconfig;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
import de.flogehring.peelserver.api.services.RenderConfigurationController;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
@RestController
public class RenderConfigurationService implements RenderConfigurationController {
    @Override
    public RenderConfigurationDto getDefault() { return toDto(ExpressionRenderConfiguration.defaultConfig()); }
    public static RenderConfigurationDto toDto(ExpressionRenderConfiguration config) {
        return new RenderConfigurationDto(config.getDefaultTemplates().templates(), config.getNamedOverrides().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().templates())));
    }
}
