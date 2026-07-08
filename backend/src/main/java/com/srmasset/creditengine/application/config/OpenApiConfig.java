package com.srmasset.creditengine.application.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "SRM Credit Engine API",
                        version = "1.0.0",
                        description =
                                "Precificação e liquidação de recebíveis (duplicatas, cheques"
                                        + " pré-datados) com câmbio multimoedas para o fundo SRM"
                                        + " Asset."),
        servers = @Server(url = "/", description = "Servidor atual"))
public class OpenApiConfig {}
