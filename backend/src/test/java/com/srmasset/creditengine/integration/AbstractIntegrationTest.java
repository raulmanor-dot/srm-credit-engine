package com.srmasset.creditengine.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração: sobe um Postgres real via Testcontainers
 * (não H2) e deixa o Spring Boot 3.1+ conectar a aplicação a ele via
 * {@code @ServiceConnection} — sem precisar declarar
 * {@code @DynamicPropertySource} manualmente. As migrations Flyway
 * (V1-V7, incluindo o trigger append-only e as constraints) rodam de
 * verdade a cada teste, contra o mesmo motor de banco usado em produção.
 *
 * <p>O container é {@code static} para ser reaproveitado entre todas as
 * classes de teste da JVM (Testcontainers reusa a mesma instância).
 */
@Testcontainers
@SpringBootTest
public abstract class AbstractIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
