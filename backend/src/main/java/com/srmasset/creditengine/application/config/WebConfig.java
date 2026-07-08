package com.srmasset.creditengine.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS liberado só para a origem do frontend (dev server Vite por padrão);
// sobrescrevível via app.cors.allowed-origin para o build de produção do
// frontend (ex.: quando servido por outra porta/host no docker compose).
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origin:http://localhost:5173}")
	private String allowedOrigin;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigin)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
