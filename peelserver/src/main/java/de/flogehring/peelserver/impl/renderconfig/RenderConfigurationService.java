package de.flogehring.peelserver.impl.renderconfig;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationUpdateRequest;
import de.flogehring.peelserver.api.services.RenderConfigurationController;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationPersistenceDto;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
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
    private final ProjectAccessService projectAccess;

    @Override
    public RenderConfigurationCreateResponse createRenderConfiguration(RenderConfigurationPersistenceDto renderConfigurationDto, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(renderConfigurationDto.projectIdApi().id()), user.getUserId(), () -> {
            ProjectId projectId = new ProjectId(renderConfigurationDto.projectIdApi().id());
            projectAccess.requireProjectAccess(projectId, user.getUserId());
            String id = UUID.randomUUID().toString();
            renderConfigurationRepository.insert(
                    RenderConfigurationPersistence.valueOf(
                            id,
                            renderConfigurationDto.name(),
                            toExpressionRenderConfig(renderConfigurationDto.renderConfigurationDto()),
                            projectId
                    )
            );
            return new RenderConfigurationCreateResponse(id);
        });
    }

    @Override
    public void updateRenderConfiguration(
            String id,
            RenderConfigurationUpdateRequest updateRequest,
            SpringUserDetails user
    ) {
        projectAccess.active(requireConfigAccess(id, user).getProjectId(), user.getUserId(), () -> {
            log.info("Updating Render Config {}", id);
            RenderConfigurationPersistence renderConfigurationPersistence = requireConfigAccess(id, user);
            renderConfigurationRepository.save(
                    RenderConfigurationPersistence.valueOf(
                            id,
                            updateRequest.name(),
                            toExpressionRenderConfig(updateRequest.renderConfigurationDto()),
                            renderConfigurationPersistence.getProjectId()
                    )
            );
            return null;
        });
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
    public RenderConfigurationPersistenceDto getById(String id, SpringUserDetails user) {
        RenderConfigurationPersistence renderConfig = requireConfigAccess(id, user);
        return new RenderConfigurationPersistenceDto(
                renderConfig.getName(),
                new ProjectIdApi(renderConfig.getProjectId().id()),
                toDto(renderConfig.getExpressionRenderConfiguration())
        );
    }

    @Override
    public void deleteById(String id, SpringUserDetails user) {
        projectAccess.active(requireConfigAccess(id, user).getProjectId(), user.getUserId(), () -> {
            requireConfigAccess(id, user);
            renderConfigurationRepository.deleteById(id);
            return null;
        });
    }

    private RenderConfigurationPersistence requireConfigAccess(String id, SpringUserDetails user) {
        var config = renderConfigurationRepository.findById(id).orElseThrow(ProjectAccessService::notFound);
        projectAccess.requireProjectAccess(config.getProjectId(), user.getUserId());
        return config;
    }

    private static ExpressionRenderConfiguration toExpressionRenderConfig(RenderConfigurationDto renderConfigurationDto) {
        return ExpressionRenderConfiguration.of(
                renderConfigurationDto.renderConfigurations(),
                renderConfigurationDto.namedOverrides()
        );
    }
}
