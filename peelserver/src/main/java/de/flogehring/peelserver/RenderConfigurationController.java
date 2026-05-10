package de.flogehring.peelserver;

import de.flogehring.peelserver.api.IdNameTuple;
import de.flogehring.peelserver.api.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.RenderConfigurationDto;
import de.flogehring.peelserver.api.RenderConfigurationPersistenceDto;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.renderconfig.RenderConfigurationPersistence;
import de.flogehring.peelserver.renderconfig.RenderConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static de.flogehring.peelserver.documents.DocumentService.toExpressionRenderConfig;

@RestController
@RequiredArgsConstructor
public class RenderConfigurationController implements RenderConfigurationService {

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
        RenderConfigurationPersistence existing = renderConfigurationRepository.findById(id)
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
                config.getTemplates()
        );
    }

    @Override
    public RenderConfigurationPersistenceDto getId(String id) {
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
}
