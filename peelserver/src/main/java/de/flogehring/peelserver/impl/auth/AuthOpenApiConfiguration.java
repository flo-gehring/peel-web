package de.flogehring.peelserver.impl.auth;

import de.flogehring.peelserver.api.data.auth.AuthLinkRequest;
import de.flogehring.peelserver.api.data.auth.CsrfResponse;
import de.flogehring.peelserver.api.data.auth.WhoAmIResponse;
import de.flogehring.peelserver.api.error.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Includes the filter-owned endpoints in the same contract as the controllers. */
@Configuration
public class AuthOpenApiConfiguration {
    @Bean
    public OpenAPI authApi() {
        Components components = new Components();
        for (Class<?> type : new Class<?>[]{AuthLinkRequest.class, CsrfResponse.class, WhoAmIResponse.class, ApiErrorResponse.class}) {
            ModelConverters.getInstance().readAll(type).forEach(components::addSchemas);
        }
        OpenAPI api = new OpenAPI().info(new Info().title("PEEL WEB API").version("1")).components(components);
        api.path("/api/whoami", new PathItem().get(new Operation().operationId("whoAmI")
                .responses(new ApiResponses().addApiResponse("200", jsonResponse("WhoAmIResponse")))));
        api.path("/api/auth/csrf", new PathItem().get(new Operation().operationId("csrf")
                .responses(new ApiResponses().addApiResponse("200", jsonResponse("CsrfResponse")))));
        api.path("/api/auth/link", new PathItem().post(new Operation().operationId("sendLink")
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/AuthLinkRequest")))))
                .responses(authResponses())));
        api.path("/api/auth/login", new PathItem().post(new Operation().operationId("login")
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType("application/x-www-form-urlencoded",
                        new MediaType().schema(new ObjectSchema().addProperty("token", new StringSchema()).addRequiredItem("token")))))
                .responses(authResponses())));
        api.path("/api/auth/logout", new PathItem().post(new Operation().operationId("logout").responses(authResponses())));
        return api;
    }

    private ApiResponses authResponses() {
        ApiResponses responses = new ApiResponses().addApiResponse("204", new ApiResponse().description("Success"));
        for (String status : new String[]{"400", "401", "403", "409", "429", "503"}) {
            responses.addApiResponse(status, jsonResponse("ApiErrorResponse"));
        }
        return responses;
    }

    private ApiResponse jsonResponse(String schema) {
        return new ApiResponse().description("Response").content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + schema))));
    }
}
