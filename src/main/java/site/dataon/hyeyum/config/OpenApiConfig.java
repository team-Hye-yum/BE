package site.dataon.hyeyum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hyeyumOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hyeyum API")
                        .description("Hyeyum backend REST API documentation")
                        .version("v1")
                        .license(new License().name("Internal")))
                .servers(List.of(new Server().url("/api").description("Default API context path")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public GroupedOpenApi supportProgramsApi() {
        return GroupedOpenApi.builder()
                .group("support-programs")
                .pathsToMatch("/support-programs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi companyDashboardApi() {
        return GroupedOpenApi.builder()
                .group("company-dashboard")
                .pathsToMatch("/companies/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminImportsApi() {
        return GroupedOpenApi.builder()
                .group("admin-imports")
                .pathsToMatch("/admin/imports/**")
                .build();
    }
}
