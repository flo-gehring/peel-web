package de.flogehring.peelserver.impl.renderconfig;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationUpdateRequest;
import de.flogehring.peelserver.api.services.RenderConfigurationController;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationPersistenceDto;
import de.flogehring.peelserver.api.error.ResourceNotFoundException;
import de.flogehring.peelserver.impl.project.ProjectId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RenderConfigurationService implements RenderConfigurationController {

    private final RenderConfigurationRepository renderConfigurationRepository;

    @Override
    public RenderConfigurationCreateResponse createRenderConfiguration(RenderConfigurationPersistenceDto renderConfigurationDto) {
        String id = UUID.randomUUID().toString();
        renderConfigurationRepository.insert(
                RenderConfigurationPersistence.valueOf(
                        id,
                        renderConfigurationDto.name(),
                        toExpressionRenderConfig(renderConfigurationDto.renderConfigurationDto()),
                        new ProjectId(renderConfigurationDto.projectIdApi().id())
                )
        );
        return new RenderConfigurationCreateResponse(id);
    }

    @Override
    public void updateRenderConfiguration(
            String id,
            RenderConfigurationUpdateRequest updateRequest
    ) {
        log.info("Updating Render Config {}", id);
        RenderConfigurationPersistence renderConfigurationPersistence = renderConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Render configuration with id " + id + " not found"));
        renderConfigurationRepository.save(
                RenderConfigurationPersistence.valueOf(
                        id,
                        updateRequest.name(),
                        toExpressionRenderConfig(updateRequest.renderConfigurationDto()),
                        renderConfigurationPersistence.getProjectId()
                )
        );
    }

    @Override
    public RenderConfigurationDto getDefault() {
        return toDto(ExpressionRenderConfiguration.defaultConfig());
    }

    public static RenderConfigurationDto toDto(ExpressionRenderConfiguration config) {
        return new RenderConfigurationDto(
                config.getDefaultTemplates().templates(),
                config.getNamedOverrides().entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().templates())
                ));
    }

    @Override
    public RenderConfigurationPersistenceDto getById(String id) {
        RenderConfigurationPersistence renderConfig = renderConfigurationRepository.findById(
                id
        ).orElseThrow(() -> new ResourceNotFoundException("Render configuration with id " + id + " not found"));
        return new RenderConfigurationPersistenceDto(
                renderConfig.getName(),
                new ProjectIdApi(renderConfig.getProjectId().id()),
                toDto(renderConfig.getExpressionRenderConfiguration())
        );
    }

    @Override
    public void deleteById(String id) {
        renderConfigurationRepository.deleteById(id);
    }

    private static ExpressionRenderConfiguration toExpressionRenderConfig(RenderConfigurationDto renderConfigurationDto) {
        return ExpressionRenderConfiguration.of(
                renderConfigurationDto.renderConfigurations(),
                renderConfigurationDto.namedOverrides()
        );
    }
}
