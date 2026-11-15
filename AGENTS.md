# AGENTS.md

Guia para agentes de código (Codex, Claude Code, Copilot, Cursor etc.) que atuam
neste repositório. Leia antes de alterar qualquer arquivo. Para visão de produto,
rotas da API e variáveis de ambiente, consulte o [`README.md`](./README.md); aqui
ficam apenas as regras de trabalho.

## Visão geral

**Gestão Empresarial**: sistema multi-loja de controle de estoque, movimentações
auditáveis, clientes, vendas e relatórios (projeto acadêmico AFYA). Dois processos
independentes, sem build conjunto:

| Pasta        | O que é                                                                | Stack                                                            |
| ------------ | ---------------------------------------------------------------------- | ---------------------------------------------------------------- |
| `backend/`   | API REST em `/api/v1`                                                  | Java 21, Spring Boot 3.4.1, JPA/Hibernate, Spring Security, Flyway, PostgreSQL 16, springdoc |
| `frontend/`  | SPA sem framework                                                      | HTML5, CSS puro, JavaScript ES Modules, Vite, pnpm, Vitest + jsdom |
| `docs/`      | Relatório ABNT e guia do projeto (PDF). Somente leitura.               | —                                                                |

O backend é a **única fonte de dados de negócio**. O navegador guarda só
preferências de UI (loja ativa, contraste, redução de movimento).

## Comandos

### Banco

```bash
docker compose up -d                                   # PostgreSQL em localhost:5433
docker exec -it gestao-postgres createdb -U gestao gestao_test   # banco dos testes
```

### Backend (`cd backend`)

```bash
DB_URL=jdbc:postgresql://localhost:5433/gestao DB_USERNAME=gestao DB_PASSWORD=gestao ./mvnw spring-boot:run
./mvnw test                                                        # usa Testcontainers se houver Docker
./mvnw test "-Dtest.db.url=jdbc:postgresql://localhost:5433/gestao_test"   # sem Docker
./mvnw clean package                                               # gera target/gestao-1.0.0.jar
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. No Windows use `mvnw.cmd`.

### Frontend (`cd frontend`)

```bash
pnpm install && cp .env.example .env
pnpm dev            # http://localhost:8443 (strictPort)
pnpm test           # Vitest
pnpm typecheck      # tsc --noEmit (cobre basicamente a configuração do Vite)
pnpm format:check   # oxfmt; use `pnpm format` para corrigir
pnpm build
```

Node 22 e pnpm 10 (ver `frontend/.mise.toml`).

### Antes de concluir qualquer tarefa

- Mudou backend: `./mvnw test` verde (são 47 testes hoje, incluindo 11 de
  integração e 9 de arquitetura; `ApiIntegrationTest` é ignorado silenciosamente
  se não houver Docker nem `-Dtest.db.url`, então **informe** quando isso
  acontecer).
- Mudou frontend: `pnpm test`, `pnpm format:check` e `pnpm build` verdes.
- Mudou contrato da API: atualize backend, `frontend/src/services/api.js`/módulos
  afetados e as tabelas do `README.md` no mesmo trabalho.

## Estrutura atual

```text
backend/src/main/java/com/empresa/gestao/
├── GestaoApplication.java
├── shared/      kernel: exceções, paginação, utilitários e portas/DTOs comuns
├── auth/        login, sessão, CSRF
├── user/        usuários, perfis (Role) e vínculo com lojas (UserStore)
├── store/       lojas e resolução de acesso do usuário
├── product/     cadastro, arquivamento e regra de saldo
├── movement/    entrada/saída/ajuste e histórico auditável
├── customer/    clientes e compras
├── sale/        vendas com baixa transacional de estoque
├── stock/       visão consolidada de estoque
├── dashboard/   indicadores e tendências
├── report/      relatórios paginados
├── security/    AppUserPrincipal, AuthSessionAdapter, handlers 401/403
└── bootstrap/   SecurityConfig, OpenApiConfig e seed/DemoDataSeeder
backend/src/main/resources/db/migration/   Flyway V1..V3
backend/src/test/java/...                  Mockito (casos de uso), ApiIntegrationTest (MockMvc)
                                           e ArchitectureTest (ArchUnit)

frontend/src/
├── app.js        bootstrap, sessão, rotas (objeto ROUTES), menu mobile
├── context.js    estado compartilhado (state) e helpers de permissão
├── services/     api.js: único cliente HTTP (cookie de sessão + CSRF)
├── modules/      uma tela por arquivo (dashboard, products, stock, ...)
├── components/   modal, notificações, paginação, estados
└── utils/        dom (h, svgIcon), formatters, validators
```

Cada módulo de negócio do backend segue a Clean Architecture em quatro camadas:

```text
<modulo>/
├── domain/          model (Java puro) e repository (portas de saída)
├── application/     usecase (um por ação), port, dto (records sem framework)
├── infrastructure/  persistence/{entity,repository,mapper,adapter}
└── presentation/    web/{Controller,request,response}
```

Os módulos `stock`, `dashboard` e `report` são somente leitura e não têm
`domain/model` próprio. `shared` concentra exceções de domínio, `PageQuery`/
`PageResult`, utilitários, `PageResponse`, `ApiError`/`GlobalExceptionHandler`
e as portas compartilhadas (`CurrentUserPort`).

### Direção arquitetural

A migração para **Clean Architecture** está concluída; o plano original está em
[`docs/REFATORACAO_CLEANARCHITECTURE.md`](./docs/REFATORACAO_CLEANARCHITECTURE.md).
As regras são verificadas por `ArchitectureTest` e qualquer violação quebra o
build:

- `domain` só depende de JDK e `shared.domain`; nada de Spring, Jakarta,
  Jackson ou outras camadas.
- `application` depende do `domain` do próprio módulo e de `shared`; de outros
  módulos, apenas `application` (casos de uso, portas e DTOs).
- `infrastructure` implementa as portas de `domain`/`application`.
- `presentation` usa `application` e `shared`; nunca `domain.model` nem
  `infrastructure`.
- `bootstrap` e `security` são os adaptadores de composição/autenticação.
- O grafo entre módulos é acíclico.

## Convenções do backend

- Pacote raiz `com.empresa.gestao`. Injeção por **construtor** (sem `@Autowired`
  em campo). Entidades com getters/setters explícitos (sem Lombok).
- Identificadores em inglês; **mensagens ao usuário em português**
  (`"Estoque insuficiente para ..."`). Enums mistos já existem: `MovementType`
  (`ENTRY`, `EXIT`, `ADJUSTMENT`), `SaleStatus` (`CONCLUIDA`), `Role` (`ADMIN`,
  `GERENTE`, `OPERADOR`). Não renomeie valores persistidos sem migração.
- IDs são `UUID`. Datas/horas em `Instant`/UTC (`jdbc.time_zone: UTC`).
- DTOs são `record`; Bean Validation (`jakarta.validation`) existe apenas nos
  `presentation/web/request`, nunca em `application/dto`.
- Erros: lance `BusinessRuleException`, `NotFoundException`,
  `ConflictException`, `ForbiddenException` ou `UnauthorizedException`;
  `GlobalExceptionHandler` (`shared.presentation.web`) converte tudo em
  `ApiError`. Não retorne `ResponseEntity` de erro manualmente nos controllers.
- Listagens: `PageQuery.of(page, size, sort, whitelist, default)` com
  **whitelist de campos de ordenação** e filtros via `*Specifications`. Resposta
  em `PageResponse<T>`.
- Transações: `@Transactional` nos casos de uso; `readOnly = true` nas
  consultas. `open-in-view` está **desligado**: carregue relações com
  `@EntityGraph`/query explícita.
- Schema: **só Flyway**. Nova mudança = nova migração `V4__descricao.sql` em
  `src/main/resources/db/migration/`. Nunca edite migração aplicada, nunca use
  `ddl-auto=update` (o padrão é `validate`).

## Regras de negócio que não podem quebrar

1. **Multi-loja**: toda consulta/mutação é filtrada por `storeId`. O controller
   obtém a loja com `ResolveStoreAccessUseCase.execute(storeId)`, que valida o
   vínculo do usuário via `CurrentUserPort`. ADMIN acessa todas as lojas; GERENTE
   e OPERADOR só as vinculadas. Nunca confie em `storeId` vindo do cliente sem
   essa resolução.
2. **Estoque**: saldo do produto e registro da movimentação são gravados na
   **mesma transação**; `@Version` em `ProductJpaEntity` (lock otimista); a regra
   de saldo vive em `product.domain.model.Product` (`StockChange`); saída nunca
   deixa saldo negativo; ajuste: o valor informado é o saldo final e o histórico
   guarda a diferença absoluta, saldo anterior e posterior; só ADMIN/GERENTE
   ajustam.
3. **Venda**: grava a venda e as saídas de estoque atomicamente; produto repetido
   no carrinho é rejeitado; permissão de criar venda: ADMIN/GERENTE.
4. **Arquivamento, não exclusão**: produtos e clientes com histórico são
   arquivados (`active = false`). Unicidade de nome/SKU vale **só entre ativos**
   (V3).
5. **Auditoria**: movimentação guarda snapshot de nome do produto, SKU, loja,
   usuário, motivo, observação e saldos. Nunca apague nem edite movimentações.
6. **Segurança**: sessão por cookie `GESTAO_SESSION` (HttpOnly, SameSite=Lax),
   CSRF via `XSRF-TOKEN`/`X-XSRF-TOKEN` em todo método de escrita, senhas com
   BCrypt, autorização por `@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")` ou
   `hasRole('ADMIN')`. Rotas públicas: apenas `auth/csrf`, `auth/login` e a
   documentação OpenAPI. Nunca exponha hash de senha ou dados sensíveis em DTO.

## Convenções do frontend

- JavaScript ES Modules **sem framework** e sem dependências de runtime
  (`dependencies` está vazio). Não adicione bibliotecas sem necessidade clara.
- Cada tela é um módulo em `src/modules/` que exporta `render` e é registrado em
  `ROUTES` (`src/app.js`). Estado global só em `src/context.js`.
- Toda chamada HTTP passa por `src/services/api.js` (`credentials: "include"`,
  CSRF automático, `ApiError`). Não use `fetch` direto nos módulos.
- **Segurança de DOM**: construa nós com o helper `h()`/`createElement` e
  `textContent`. Nunca `innerHTML` com dado vindo da API (previne XSS).
- Permissões de UI (`canManage`, `canAdjust`, ...) são só conveniência; a regra
  real está no backend.
- Não persista dados de negócio em `localStorage`/`sessionStorage`.
- Formatação com oxfmt (`pnpm format`); sem ponto e vírgula, aspas duplas, como
  no código existente. Textos da interface em português do Brasil, com
  acessibilidade (rótulos, foco, contraste, redução de movimento).
- Testes ao lado do arquivo: `x.js` → `x.test.js` (Vitest + jsdom).
- Não edite `frontend/.figma/` (gerado pelo Figma Make) nem
  `frontend/pnpm-lock.yaml` manualmente. `vite.config.ts` contém plugins do
  Figma; altere com cuidado.

## Testes

- Casos de uso: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) +
  AssertJ, mockando as portas.
- Integração: `ApiIntegrationTest` (`@SpringBootTest` + MockMvc + PostgreSQL via
  Testcontainers ou `-Dtest.db.url`). Cobre autenticação, CSRF, isolamento entre
  lojas, paginação e validações.
- Arquitetura: `ArchitectureTest` (ArchUnit) valida as regras de dependência dos
  módulos e camadas.
- Toda regra de negócio nova ou alterada precisa de teste de caso de uso; todo
  endpoint novo precisa de cenário de integração (incluindo 401/403 e acesso a
  outra loja). Corrija o código, não o teste, quando uma regra do bloco acima
  falhar.
- O fuso dos testes é fixado em UTC no `pom.xml`.

## Segredos e ambiente

- Valores de `.env.example` são apenas de desenvolvimento. Nunca commite `.env`,
  senhas reais ou tokens.
- Em produção: `APP_SEED_DEMO=false`, trocar `APP_SEED_PASSWORD` e
  `APP_COOKIE_SECURE=true` atrás de HTTPS. Não ative `DemoDataSeeder` em
  ambiente real.
- Credenciais de demonstração (`admin@gestao.local`, `gerente@gestao.local`,
  `operador@gestao.local`, senha `gestao123`) existem só quando `APP_SEED_DEMO=true`.

## Como trabalhar aqui

- Faça mudanças pequenas e focadas; não reformate arquivos inteiros nem renomeie
  pacotes fora do escopo pedido.
- Leia o código vizinho e siga o padrão existente antes de criar algo novo.
- Não toque em `docs/`, `LICENSE`, `.idea/` ou `target/`/`dist/`.
- Commits curtos e focados, um assunto por commit; ao mover arquivos use
  `git mv` e não misture a movimentação com mudança de comportamento.
- Se uma regra deste arquivo conflitar com o pedido do usuário, avise antes de
  agir. Se o código real divergir deste documento, siga o código e proponha a
  correção do `AGENTS.md`.