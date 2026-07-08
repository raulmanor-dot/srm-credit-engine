# Uso de IA neste projeto

Este projeto foi desenvolvido com Claude Code (Anthropic) como copiloto em várias etapas: geração de massa de dados, refatoração, scaffolding de documentação e revisão de endpoints. Este documento descreve como a IA foi usada, onde ela errou de forma relevante e o que isso ensinou, e uma análise crítica de custo/benefício.

## Prompts estratégicos utilizados

A divisão de trabalho foi clara ao longo do projeto: o autor definiu a arquitetura, a stack tecnológica e o plano de como cada parte deveria ser construída; o Claude foi quem produziu o código na prática, seguindo essa direção. Toda entrega passou por teste manual do autor — via Postman e navegador — para confirmar que o resultado gerado pela IA realmente correspondia ao que fazia sentido para o negócio, e não apenas ao que compilava.

Prompts estratégicos concretos incluíram:

**Montagem do ambiente de desenvolvimento**: o Claude ajudou a baixar, instalar e configurar as ferramentas necessárias (incluindo via WSL), reduzindo o atrito inicial de setup.

**Implementação seguindo o plano de tecnologias**: tanto no backend quanto no frontend, o Claude seguiu as escolhas de stack já decididas pelo autor (Spring Boot/Java no backend, React/TypeScript no frontend), implementando a lógica de negócio elaborada previamente — motor de precificação, regras de liquidação, componentes de UI.

**Montagem de observabilidade e documentação de API**: dashboards do Grafana e configuração do Swagger/OpenAPI, seguindo a orientação do autor sobre o que deveria ser observado e documentado.

**Geração de massa de dados de demonstração** (migration `V8__seed_demo_data.sql`): pedido para gerar 10 cedentes, 45 recebíveis (mix de PENDING/SETTLED/CANCELED) e ~30 liquidações, incluindo uma fração cross-currency BRL↔USD, usando a mesma fórmula de precificação do motor (não valores arbitrários). O prompt exigiu explicitamente que os valores calculados na seed batessem com a fórmula real do domínio, para que a massa de demonstração fosse coerente com o próprio sistema, não apenas visualmente plausível.

**Scaffolding de documentação de arquitetura**: diagramas ER e C4 (Mermaid) gerados a partir da leitura direta das migrations Flyway e dos controllers existentes — a IA não inventou entidades ou relacionamentos, apenas traduziu o schema já existente para uma notação visual.

**Refatoração de formatação**: adoção do Spotless (Google Java Format) e reformatação de todo o código-fonte backend existente, para padronizar estilo sem alterar comportamento.

**Revisão de endpoints contra o enunciado**: comparação sistemática entre os requisitos funcionais do desafio (item 3.5 — extrato filtrável por período, cedente e moeda) e o que estava implementado, que revelou a ausência do filtro por moeda de pagamento no endpoint de relatório.

## Onde a IA errou e como foi corrigido

Ao longo do processo, surgiram vários pequenos erros — em massas de teste, em lógica de backend, na apresentação do frontend, na configuração do Grafana. Nenhum desses erros isoladamente foi grave; o que fez diferença foi o papel do autor em identificar rapidamente qual era o problema real e redirecionar o Claude de forma técnica e específica, em vez de deixá-lo insistir repetidamente numa mesma tentativa mal-sucedida — o que teria consumido tempo e tokens sem necessidade, num problema que o ser humano já havia entendido.

Os dois exemplos mais relevantes, tecnicamente:

**Datas de liquidação no futuro na massa de demonstração.** Na primeira geração da seed V8, algumas liquidações receberam `settled_at` posterior à data atual do sistema — um erro sutil porque os valores numéricos e os relacionamentos estavam corretos, só a linha do tempo é que não fazia sentido de negócio (uma liquidação não pode ocorrer antes de o recebível existir, nem no futuro em relação ao "agora" da demonstração). Detectado por revisão manual do domínio (não por teste automatizado), corrigido ajustando as datas de referência da geração.

**Erro 500 mascarando uma regra de domínio.** Durante uma validação manual end-to-end da aplicação já em execução, simular um recebível com vencimento anterior à data de referência devolvia um HTTP 500 genérico com stack trace no corpo da resposta, em vez de um erro de negócio tratado. A causa era uma `IllegalArgumentException` de uma classe de domínio (`TermCalculator`) sem handler dedicado no `GlobalExceptionHandler` — o único caso de erro de domínio que não tinha sido mapeado para um código HTTP semântico. Corrigido adicionando um handler que traduz essa exceção para 422, mais um handler catch-all que garante que nenhuma exceção não mapeada vaze detalhes internos numa resposta HTTP (o erro completo continua indo para o log estruturado).

**Ambiente de execução local instável entre sessões.** Em mais de uma ocasião, comandos que dependiam de Docker/Testcontainers falharam por o daemon não estar de pé, ou por incompatibilidade entre a API do Docker Desktop e a do Podman quando ambos coexistiam na máquina — não um erro de raciocínio da IA, mas um lembrete de que "os testes passam" só é uma afirmação válida quando o ambiente por trás dela foi de fato verificado, não assumido.

## Análise crítica

O Claude foi o principal impulsionador que possibilitou desenvolver este projeto, com essa profundidade, no tempo disponível. Gerar massa de dados volumosa e internamente consistente, traduzir o schema existente para diagramas Mermaid, e fazer uma varredura sistemática do enunciado contra o código para achar requisitos não atendidos (o filtro de moeda ausente, por exemplo) são tarefas onde a velocidade da IA é uma vantagem real e difícil de igualar manualmente.

Mas a visão humana sobre os problemas — saber quando insistir e quando redirecionar, guiar a IA em relação aos requisitos do enunciado, à arquitetura já decidida e às regras de negócio do domínio financeiro — foi o que manteve o custo de tokens baixo, economizou tempo e permitiu produzir algo com qualidade e velocidade ao mesmo tempo. Mudanças de formatação em escala (Spotless em todo o código-fonte) são um exemplo de decisão de baixo risco técnico mas alto volume de diff, que merecia ter sido confirmada antes de aplicada, não só depois. Os dois erros mais relevantes encontrados (datas incoerentes na seed, exceção de domínio sem tratamento HTTP) só foram corrigidos porque alguém com entendimento do negócio revisou o resultado — a IA não os identificou sozinha.
