package de.flogehring.peelserver.renderconfig;

import de.flogehring.peelserver.RenderConfigurationController;
import de.flogehring.peelserver.api.IdNameTuple;
import de.flogehring.peelserver.api.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.RenderConfigurationDto;
import de.flogehring.peelserver.api.RenderConfigurationPersistenceDto;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
                        toExpressionRenderConfig(renderConfigurationDto.renderConfigurationDto())
                )

        );
        return new RenderConfigurationCreateResponse(id);
    }

    @Override
    public void updateRenderConfiguration(
            String id,
            RenderConfigurationPersistenceDto persistenceDto
    ) {
        log.info("Updating Render Config {}", id);
        renderConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Render configuration with id " + id + " not found"));
        renderConfigurationRepository.save(
                RenderConfigurationPersistence.valueOf(
                        id,
                        persistenceDto.name(),
                        toExpressionRenderConfig(persistenceDto.renderConfigurationDto())
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
                toDto(renderConfig.getExpressionRenderConfiguration())
        );
    }

    @Override
    public List<IdNameTuple> listAll() {
        return renderConfigurationRepository.findAll().stream().map(
                renderConfig -> new IdNameTuple(renderConfig.getId(), renderConfig.getName())
        ).toList();
    }

    private static ExpressionRenderConfiguration toExpressionRenderConfig(RenderConfigurationDto renderConfigurationDto) {
        return ExpressionRenderConfiguration.of(
                renderConfigurationDto.renderConfigurations(),
                renderConfigurationDto.namedOverrides()
        );
    }
}
