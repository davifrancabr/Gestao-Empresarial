# Gestão Empresarial

Sistema de gestão com controle de estoque, movimentações auditáveis, clientes,
vendas e relatórios. O projeto é dividido em dois processos independentes:

- **`backend/`** — API Java 21 com Spring Boot, Spring Security, JPA/Hibernate,
  Flyway e PostgreSQL.
- **`frontend/`** — aplicação HTML5, CSS puro e JavaScript ES Modules com Vite.

O backend é a única fonte de dados de negócio. O navegador guarda apenas
preferências de interface (loja ativa, contraste e redução de movimento); não há
mais persistência de produtos ou movimentações em `localStorage`.

## Stack

| Camada          | Tecnologias                                                                                                              |
| --------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Backend         | Java 21, Spring Boot 3.4, Spring Web, Data JPA/Hibernate, Security, Validation, Flyway, PostgreSQL 16, springdoc OpenAPI |
| Testes backend  | JUnit 5, Mockito, Spring Boot Test, Testcontainers, ArchUnit                                                             |
| Frontend        | HTML5, CSS puro, JavaScript ES Modules, Vite 8, pnpm                                                                     |
| Testes frontend | Vitest + jsdom                                                                                                           |

## Estrutura

```text
.
├── backend/
│   ├── src/main/java/com/empresa/gestao/
│   │   ├── GestaoApplication.java
│   │   ├── shared/        kernel: exceções, paginação, utilitários e portas comuns
│   │   ├── auth/          login, sessão e CSRF
│   │   ├── user/          usuários, perfis e vínculos com lojas
│   │   ├── store/         lojas e resolução de acesso
│   │   ├── product/       cadastro, arquivamento e regra de saldo
│   │   ├── movement/      entradas, saídas, ajustes e histórico auditável
│   │   ├── customer/      clientes e histórico de compras
│   │   ├── sale/          vendas com baixa transacional
│   │   ├── stock/         visão consolidada de estoque e indicadores
│   │   ├── dashboard/     indicadores e tendências
│   │   ├── report/        relatórios paginados
│   │   ├── security/      adaptador de autenticação (Spring Security)
│   │   └── bootstrap/     SecurityConfig, OpenAPI e carga de demonstração
│   ├── src/main/resources/db/migration/  Flyway (V1-V3)
│   └── src/test/java/     testes unitários, de integração e de arquitetura
├── frontend/
│   ├── index.html         shell acessível, login e sprite de ícones
│   ├── src/app.js         bootstrap, sessão, rotas e menu mobile
│   ├── src/services/      cliente HTTP central (api.js)
│   ├── src/modules/       painel, produtos, estoque, movimentações, etc.
│   ├── src/components/    modais, notificações, paginação e estados
│   └── src/utils/         DOM seguro, formatadores e validações
├── docker-compose.yml     PostgreSQL
├── .env.example           variáveis do banco e da API
```

Cada módulo de negócio do backend segue a Clean Architecture em quatro camadas:
`domain/` (modelos e portas de repositório em Java puro), `application/`
(casos de uso, DTOs e portas para outros módulos), `infrastructure/`
(persistência JPA e adaptadores) e `presentation/` (controllers e DTOs HTTP). As
dependências apontam sempre para dentro e são verificadas por
`ArchitectureTest` com ArchUnit.

## Pré-requisitos

- Java 21+
- Node.js 20.19+ e pnpm
- Docker (recomendado para o PostgreSQL)

## Como executar

### 1. PostgreSQL

```bash
docker compose up -d
```

O Compose cria o banco `gestao` e expõe o PostgreSQL em
`localhost:5433` (porta configurável por `POSTGRES_PORT`). O volume
`gestao-postgres-data` preserva os dados entre reinícios.

Para criar também o banco usado pelos testes de integração:

```bash
docker exec -it gestao-postgres createdb -U gestao gestao_test
```

### 2. Backend

O backend lê as variáveis de ambiente do processo. Use os valores de
`.env.example` como referência.

**PowerShell (Windows):**

```powershell
cd backend
$env:DB_URL = "jdbc:postgresql://localhost:5433/gestao"
$env:DB_USERNAME = "gestao"
$env:DB_PASSWORD = "gestao"
.\mvnw.cmd spring-boot:run
```

**Bash/Linux/macOS:**

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5433/gestao \
DB_USERNAME=gestao \
DB_PASSWORD=gestao \
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O Flyway aplica as migrações
automaticamente na inicialização e o Hibernate apenas valida o schema
(`ddl-auto=validate`).

Também é possível empacotar e executar o JAR (mantendo as variáveis de
ambiente definidas, pois o padrão de `DB_URL` usa a porta 5432):

```bash
./mvnw clean package
java -jar target/gestao-1.0.0.jar
```

Documentação OpenAPI:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`

### 3. Frontend

```bash
cd frontend
pnpm install
cp .env.example .env       # Windows: Copy-Item .env.example .env
pnpm dev
```

O Vite inicia em `http://localhost:8443` (ou na porta definida em `PORT`).
Configure `VITE_API_BASE_URL` em `frontend/.env` se a API estiver em outro
endereço.

Em produção:

```bash
pnpm build     # gera frontend/dist
pnpm preview   # serve a build
```

### 4. Problemas comuns

| Mensagem                                                        | Causa                                                                 | Correção                                                                 |
| --------------------------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `Connection to localhost:5433 refused`                          | PostgreSQL parado ou container removido                               | `docker compose up -d` e confirme com `docker compose ps`                |
| `Connection to localhost:5432 refused` ao subir o backend       | Padrão de `DB_URL` usa a porta 5432, mas o Compose expõe a 5433        | Defina `DB_URL=jdbc:postgresql://localhost:5433/gestao` antes de iniciar |
| Frontend sem dados e erro de rede no console                    | API não está rodando em `http://localhost:8080`                       | Suba o backend (seção 2); o padrão do frontend já aponta para essa URL   |
| `E-mail ou senha inválidos` para as credenciais de demonstração | Banco recriado sem `DemoDataSeeder`, ou `APP_SEED_DEMO=false`         | Reinicie o backend com `APP_SEED_DEMO=true` sobre um banco vazio         |

O backend mantém o pool de conexões ativo: se o PostgreSQL cair e voltar na
mesma porta, as requisições seguintes se reconectam sem reiniciar a API. Se o
container for recriado com o volume `gestao-postgres-data`, os dados continuam
preservados.

## Credenciais de demonstração

Criadas por `DemoDataSeeder` quando `APP_SEED_DEMO=true`:

| E-mail                  | Perfil   | Lojas           |
| ----------------------- | -------- | --------------- |
| `admin@gestao.local`    | ADMIN    | todas           |
| `gerente@gestao.local`  | GERENTE  | Loja 1 - Matriz |
| `operador@gestao.local` | OPERADOR | Loja 1 - Matriz |

Senha padrão: `gestao123` (configurável por `APP_SEED_PASSWORD`).

> Em produção, defina `APP_SEED_DEMO=false`, troque a senha e configure
> `APP_COOKIE_SECURE=true` atrás de HTTPS.

## Variáveis de ambiente

| Variável                   | Padrão                                        | Descrição                                  |
| -------------------------- | --------------------------------------------- | ------------------------------------------ |
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/gestao`     | JDBC do PostgreSQL                         |
| `DB_USERNAME`              | `gestao`                                      | Usuário do banco                           |
| `DB_PASSWORD`              | `gestao`                                      | Senha do banco                             |
| `DB_POOL_SIZE`             | `10`                                          | Conexões do HikariCP                       |
| `APP_PORT`                 | `8080`                                        | Porta da API                               |
| `APP_SESSION_TIMEOUT`      | `8h`                                          | Expiração da sessão                        |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8443,http://127.0.0.1:8443` | Origens liberadas (separadas por vírgula)  |
| `APP_COOKIE_SECURE`        | `false`                                       | Exige HTTPS nos cookies de sessão/CSRF     |
| `APP_SEED_DEMO`            | `true`                                        | Cria usuários e catálogo de demonstração   |
| `APP_SEED_PASSWORD`        | `gestao123`                                   | Senha inicial dos usuários de demonstração |
| `POSTGRES_PORT`            | `5433`                                        | Porta do PostgreSQL no host (Compose)      |
| `VITE_API_BASE_URL`        | `http://localhost:8080/api/v1`                | URL da API usada pelo frontend             |
| `GESTAO_TEST_DB_URL`       | —                                             | Banco externo para testes de integração    |

## API

Base: `/api/v1`. Todas as rotas, exceto `auth/csrf` e `auth/login`, exigem
sessão autenticada. Operações de escrita exigem o token CSRF devolvido por
`GET /auth/csrf` no header `X-XSRF-TOKEN`.

### Autenticação

| Método | Rota           | Descrição                                  |
| ------ | -------------- | ------------------------------------------ |
| `GET`  | `/auth/csrf`   | Devolve `headerName` e `token`             |
| `POST` | `/auth/login`  | Cria a sessão (`GESTAO_SESSION`, HttpOnly) |
| `GET`  | `/auth/me`     | Sessão atual, perfil e lojas vinculadas    |
| `POST` | `/auth/logout` | Encerra a sessão                           |

### Produtos, estoque e movimentações

| Método   | Rota                    | Descrição                                                                                         |
| -------- | ----------------------- | ------------------------------------------------------------------------------------------------- |
| `GET`    | `/products`             | Lista paginada; filtros `search`, `category`, `status`, `sort`, `page`, `size`, `includeInactive` |
| `GET`    | `/products/categories`  | Categorias da loja                                                                                |
| `GET`    | `/products/{id}`        | Detalhe                                                                                           |
| `POST`   | `/products`             | Cria produto; `quantity` inicial gera entrada auditável                                           |
| `PUT`    | `/products/{id}`        | Atualiza cadastro e `version` (lock otimista)                                                     |
| `DELETE` | `/products/{id}`        | Arquiva produto, preservando histórico                                                            |
| `GET`    | `/stock`                | Visão de estoque paginada e indicadores                                                           |
| `GET`    | `/stock/movements`      | Histórico completo com filtros e paginação                                                        |
| `GET`    | `/stock/movements/{id}` | Detalhe da movimentação                                                                           |
| `POST`   | `/stock/movements`      | Entrada, saída ou ajuste                                                                          |

### Clientes, vendas e relatórios

| Método           | Rota                        | Descrição                      |
| ---------------- | --------------------------- | ------------------------------ |
| `GET/POST`       | `/customers`                | Lista e cadastro de clientes   |
| `GET/PUT/DELETE` | `/customers/{id}`           | Detalhe, edição e arquivamento |
| `GET`            | `/customers/{id}/purchases` | Compras do cliente             |
| `GET/POST`       | `/sales`                    | Lista e registro de vendas     |
| `GET`            | `/sales/{id}`               | Detalhe com itens              |
| `GET`            | `/reports/products`         | Relatório de produtos          |
| `GET`            | `/reports/stock`            | Relatório de estoque           |
| `GET`            | `/reports/movements`        | Relatório de movimentações     |
| `GET`            | `/reports/sales`            | Receita e vendas               |
| `GET`            | `/reports/top-products`     | Produtos mais movimentados     |
| `GET`            | `/reports/low-turnover`     | Produtos com baixo giro        |

### Painel, usuários e lojas

| Método     | Rota           | Descrição                                      |
| ---------- | -------------- | ---------------------------------------------- |
| `GET`      | `/dashboard`   | Indicadores, tendência e últimas movimentações |
| `GET/POST` | `/users`       | Usuários (ADMIN)                               |
| `PUT`      | `/users/{id}`  | Atualização de usuário (ADMIN)                 |
| `GET/POST` | `/stores`      | Lojas (ADMIN)                                  |
| `GET/PUT`  | `/stores/{id}` | Detalhe e edição (ADMIN)                       |

A maioria das rotas aceita `storeId`. O backend valida o vínculo do usuário
com a loja em todas as consultas e mutações.

## Regras de negócio e segurança

- **Multi-loja**: ADMIN acessa todas as lojas; GERENTE e OPERADOR acessam
  apenas as lojas vinculadas. A validação ocorre no backend, não somente na UI.
- **Estoque**: saldo e movimentação são gravados na mesma transação, com
  `@Version` no produto e rollback em caso de falha. Saídas nunca deixam saldo
  negativo.
- **Ajuste**: para ADMIN/GERENTE, o valor informado é o saldo final; o histórico
  registra a diferença absoluta e os saldos anterior/posterior.
- **Vendas**: a conclusão grava a venda e as saídas de estoque atomicamente.
  Produtos duplicados no carrinho são rejeitados.
- **Arquivamento**: produtos e clientes com histórico são arquivados, não
  apagados fisicamente. Como a unicidade de nome/SKU vale apenas entre produtos
  ativos, um item arquivado pode ser recadastrado com o mesmo nome.
- **Auditoria**: movimentações guardam snapshot do nome do produto, SKU, loja,
  usuário, motivo, observação e saldos anterior/posterior. Não há limite de
  20 registros.
- **Sessão**: cookie `GESTAO_SESSION` HttpOnly, SameSite=Lax; CSRF por cookie
  `XSRF-TOKEN` e header `X-XSRF-TOKEN`.
- **Senhas**: BCrypt. Nenhum dado sensível é exposto nas respostas.
- **Schema**: Flyway é a única fonte de DDL (`ddl-auto=validate`).

## Testes e qualidade

### Backend

```bash
cd backend

# Com Docker disponível, os testes de integração usam Testcontainers.
./mvnw test

# Alternativa local: banco PostgreSQL já existente (create database gestao_test).
./mvnw test "-Dtest.db.url=jdbc:postgresql://localhost:5433/gestao_test"
```

No PowerShell, use `.\mvnw.cmd` e mantenha a propriedade entre aspas.

A suíte cobre serviços (estoque, vendas, produtos, relatórios) e fluxo HTTP
completo (autenticação, CSRF, isolamento entre lojas, paginação e validações).

### Frontend

```bash
cd frontend
pnpm test            # Vitest + jsdom
pnpm typecheck       # tsc --noEmit (tooling Vite)
pnpm format:check    # oxfmt em src, scripts e configs do Vite/Vitest
pnpm build
```

## Migrações

| Versão                              | Conteúdo                                                                     |
| ----------------------------------- | ---------------------------------------------------------------------------- |
| `V1__schema_inicial.sql`            | Lojas, usuários, vínculos, produtos, movimentações, clientes, vendas e itens |
| `V2__lojas_iniciais.sql`            | Duas lojas de demonstração                                                   |
| `V3__unicidade_produtos_ativos.sql` | Nome/SKU únicos apenas entre produtos ativos                                 |

Para evoluir o schema, crie uma nova migração `V4__descricao.sql` em
`backend/src/main/resources/db/migration/`. Nunca altere uma migração já
aplicada nem use `ddl-auto=update`.

## Decisões e limitações

- Frontend e backend são processos independentes; não há proxy obrigatório nem
  build conjunto.
- A atualização em tempo real (SSE/WebSocket) não foi implementada; cada
  operação refaz a consulta necessária.
- O frontend usa criação segura de DOM (`textContent`/`createElement`) para
  dados vindos da API, evitando XSS.
- Em ambientes com Docker via named pipe quebrado, o Testcontainers pode não
  detectar o daemon; use `-Dtest.db.url` como alternativa.

## Equipe

| Nome                              | Matrícula |
| --------------------------------- | --------- |
| Lívia da Silva Teixeira de Barros | 2653161   |
| Davi Cavalcante França            | 2671244   |
| Tayna Santos Santana Gonzáles     | 2636576   |
| Josué dos Santos Lourenço         | 2628200   |

**Orientador:** Prof. Me. Ícaro Santos Ferreira
