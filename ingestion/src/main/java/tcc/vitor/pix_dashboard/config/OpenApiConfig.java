package tcc.vitor.pix_dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pixDashboardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PIX Dashboard - Ingestion API")
                        .description("API para ingestão de dados do PIX (BACEN), população e PIB (IBGE) por município.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vitor")
                        )
                )
                .tags(List.of(
                        new Tag().name("BACEN PIX").description("Ingestão de dados de transações PIX do Banco Central"),
                        new Tag().name("IBGE").description("Ingestão de dados populacionais e de PIB do IBGE")
                ));
    }
}
