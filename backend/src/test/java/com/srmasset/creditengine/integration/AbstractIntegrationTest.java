package com.srmasset.creditengine.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integração: sobe um Postgres real via Testcontainers
 * (não H2) e deixa o Spring Boot 3.1+ conectar a aplicação a ele via
 * {@code @ServiceConnection} — sem precisar declarar
 * {@code @DynamicPropertySource} manualmente. As migrations Flyway
 * (V1-V7, incluindo o trigger append-only e as constraints) rodam de
 * verdade a cada teste, contra o mesmo motor de banco usado em produção.
 *
 * <p>Padrão "singleton container" (deliberadamente sem {@code @Testcontainers}
 * / {@code @Container}): essa extensão do JUnit5 trata um campo estático
 * como escopo *por classe* — inicia antes da classe e para depois da
 * última classe que o usa — não como singleton de JVM. Como o campo é
 * herdado por 9 classes de teste, isso derrubava o container no meio da
 * suíte e recriava um novo em outra porta, enquanto o Spring reaproveitava
 * o {@code ApplicationContext} (e o pool Hikari) cacheado da classe
 * anterior, ainda apontando para a porta antiga — resultando em
 * {@code ConnectException} determinístico a partir da 2ª classe de teste
 * em diante. O start manual em bloco estático garante uma única
 * inicialização por JVM; o Ryuk (reaper do Testcontainers) ainda limpa o
 * container na saída da JVM.
 *
 * <p>{@code app.exchange-rate-provider.enabled=false}: este contexto sobe
 * com {@code webEnvironment=MOCK} (sem porta HTTP real ouvindo), então uma
 * chamada de verdade ao provedor mock (self-HTTP-call) falharia ou, pior,
 * acertaria por acidente um servidor de dev na 8080. Testes que precisam do
 * provedor habilitado (ver {@code ExchangeRateProviderHappyPathIntegrationTest}
 * / {@code ExchangeRateProviderFallbackIntegrationTest}) sobem com
 * {@code DEFINED_PORT} e redeclaram essa propriedade.
 */
@SpringBootTest(properties = "app.exchange-rate-provider.enabled=false")
public abstract class AbstractIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	static {
		POSTGRES.start();
	}
}
