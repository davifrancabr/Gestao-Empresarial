# Gestão Empresarial

Sistema web multi-loja para cadastro de produtos e clientes, controle de estoque,
movimentações auditáveis, vendas, indicadores e relatórios (com exportação CSV
pela interface). O projeto é dividido em dois processos independentes:

- **`backend/`** — API Java 21 com Spring Boot, Spring Security, JPA/Hibernate,
  Flyway e PostgreSQL.
- **`frontend/`** — aplicação HTML5, CSS puro e JavaScript ES Modules com Vite.

O backend é a única fonte de dados de negócio. O navegador guarda apenas
preferências de interface (loja ativa, contraste e redução de movimento); não há
mais persistência de produtos ou movimentações em `localStorage`.

## Stack

| Camada          | Tecnologias                                                                                                                |
|-----------------|----------------------------------------------------------------------------------------------------------------------------|
| Backend         | Java 21, Spring Boot 3.4.1, Spring Web, Data JPA/Hibernate, Security, Validation, Flyway, PostgreSQL 16, springdoc OpenAPI |
| Testes backend  | JUnit 5, Mockito, Spring Boot Test, Testcontainers, ArchUnit                                                               |
| Frontend        | HTML5, CSS puro, JavaScript ES Modules, Vite 8, pnpm                                                                       |
| Testes frontend | Vitest + jsdom, TypeScript (verificação de configuração)                                                                   |

## Estrutura

```text
.
├── backend/
│   ├── src/main/java/com/empresa/gestao/
│   │   ├── GestaoApplication.java
│   │   ├── shared/        # kernel: exceções, paginação, utilitários e portas comuns
│   │   ├── auth/          # login, sessão e CSRF
│   │   ├── user/          # usuários, perfis e vínculos com lojas
│   │   ├── store/         # lojas e resolução de acesso
│   │   ├── product/       # cadastro, arquivamento e regra de saldo
│   │   ├── movement/      # entradas, saídas, ajustes e histórico auditável
│   │   ├── customer/      # clientes e histórico de compras
│   │   ├── sale/          # vendas com baixa transacional
│   │   ├── stock/         # visão consolidada de estoque e indicadores
│   │   ├── dashboard/     # indicadores e tendências
│   │   ├── report/        # relatórios paginados
│   │   ├── security/      # adaptador de autenticação (Spring Security)
│   │   └── bootstrap/     # SecurityConfig, OpenAPI e carga de demonstração
│   ├── src/main/resources/db/migration/  # Flyway (V1-V3)
│   └── src/test/java/     # testes unitários, de integração e de arquitetura
├── frontend/
│   ├── index.html         # shell acessível, login e sprite de ícones
│   ├── src/app.js         # bootstrap, sessão, rotas e menu mobile
│   ├── src/context.js     # estado compartilhado e seleção de loja
│   ├── src/services/      # cliente HTTP central (api.js)
│   ├── src/modules/       # painel, produtos, estoque, movimentações, etc.
│   ├── src/components/    # modais, notificações, paginação e estados
│   ├── src/utils/         # DOM seguro, formatadores e validações
│   ├── scripts/           # formatador oxfmt (format.mjs)
│   └── .figma/            # arquivos gerados pelo Figma Make (não editar)
├── docs/                  # documentos acadêmicos
├── docker-compose.yml     # PostgreSQL
├── .env.example           # variáveis do banco e da API
```

Cada módulo de negócio do backend segue a Clean Architecture em quatro camadas:
`domain/` (modelos e portas de repositório em Java puro), `application/`
(casos de uso, DTOs e portas para outros módulos), `infrastructure/`
(persistência JPA e adaptadores) e `presentation/` (controllers e DTOs HTTP). As
dependências apontam sempre para dentro e são verificadas por
`ArchitectureTest` com ArchUnit.

## Funcionalidades implementadas

- **Painel:** indicadores de estoque, histórico recente, tendência de movimentações
  e listas de produtos com estoque baixo ou zerado.
- **Produtos e estoque:** cadastro, edição, arquivamento, saldo por loja, entrada,
  saída, ajuste de inventário e histórico auditável.
- **Clientes e vendas:** gestão de clientes, consulta de compras e registro de
  vendas com baixa transacional do estoque.
- **Relatórios:** produtos, estoque, movimentações e vendas, além de produtos
  com maior movimentação e baixo giro. A interface exporta CSV da **página
  atualmente carregada**, não de todos os resultados paginados.
- **Acesso:** autenticação por sessão, perfis ADMIN/GERENTE/OPERADOR e autorização
  por loja aplicada no backend.
- **Configurações:** gestão de usuários e lojas (ADMIN) e preferências de
  interface (contraste e redução de movimento).

## Pré-requisitos

- **JDK 21** (ou versão posterior compatível com o projeto);
- o Maven Wrapper (`backend/mvnw` ou `mvnw.cmd`) baixa o Maven na primeira execução, se necessário.
- **Node.js 20.19+** (ou versão recente compatível com Vite 8) e **pnpm**. O
  toolchain recomendado está em `frontend/.mise.toml` (Node 22 e pnpm 10.34.3).
- **Docker com Compose** (recomendado para iniciar o PostgreSQL) **ou** uma
  instância PostgreSQL 16 acessível por JDBC.

## Como executar

### 1. PostgreSQL

A partir da **raiz do repositório**:

```bash
docker compose up -d
docker compose ps
```

O Compose cria o banco `gestao` e expõe o PostgreSQL em
`localhost:5433` (porta configurável por `POSTGRES_PORT`). O volume
`gestao-postgres-data` preserva os dados entre reinícios. Se quiser
personalizar as credenciais ou portas do Compose, copie **na raiz**
`.env.example` para `.env` e edite `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD` e `POSTGRES_PORT` **antes** de iniciar o contêiner.

O `.env` na raiz é lido pelo **Docker Compose**, mas **não é carregado
automaticamente pelo Spring Boot**: as variáveis `DB_*` devem ser fornecidas
na sessão do terminal, pela IDE ou pelo ambiente de execução da API.

Para usar um PostgreSQL local nos testes de integração, crie um banco **separado** (com os valores padrão do Compose):

```bash
docker exec gestao-postgres createdb -U gestao gestao_test
```

Esse banco de testes é dispensável quando o Testcontainers consegue iniciar
seu próprio PostgreSQL. **Nunca use um banco com dados reais para testes.**

### 2. Backend

O backend lê as variáveis de ambiente do **processo**. Use os valores de
`.env.example` como referência e ajuste `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`
se tiver personalizado o Compose. Execute os comandos a seguir **a partir da
raiz do repositório**, em um novo terminal:

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

No Linux/macOS, caso o Maven Wrapper não tenha permissão de execução após
extrair o ZIP, rode `chmod +x backend/mvnw` a partir da raiz do repositório
antes de iniciá-lo.

A API sobe em `http://localhost:8080`. O Flyway aplica as migrações
automaticamente na inicialização e o Hibernate apenas valida o schema (`ddl-auto=validate`).

Também é possível empacotar e executar o JAR (mantendo as variáveis de
ambiente definidas, pois o padrão de `DB_URL` usa a porta 5432):

```bash
./mvnw clean package
java -jar target/gestao-1.0.0.jar
```

Documentação OpenAPI (com o backend iniciado):

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`

### 3. Frontend

Em **outro terminal**, a partir da raiz do repositório:

**Bash/Linux/macOS:**

```bash
cd frontend
cp .env.example .env
pnpm install
pnpm dev
```

**PowerShell (Windows):**

```powershell
cd frontend
Copy-Item .env.example .env
pnpm install
pnpm dev
```

O Vite inicia em `http://localhost:8443` (ou na porta definida em `PORT`).
A variável `VITE_API_BASE_URL` em `frontend/.env` deve apontar para a **API
Java**, incluindo `/api/v1` (padrão: `http://localhost:8080/api/v1`). Se mudar
a origem do frontend, inclua a nova origem em `APP_CORS_ALLOWED_ORIGINS` no
backend. Use o mesmo host (`localhost` ou `127.0.0.1`) na navegação e na API
para evitar problemas de cookies de sessão no desenvolvimento.

Para gerar e visualizar a compilação de produção, execute os comandos abaixo
com o servidor de desenvolvimento parado (o `preview` também usa a porta 8443
por padrão):

```bash
pnpm build     # gera frontend/dist
pnpm preview   # serve a compilação localmente
```

**Atenção:** `pnpm preview` apenas serve arquivos estáticos para validação;
não implanta nem inicia o backend.

### 4. Problemas comuns

| Mensagem                                                        | Causa                                                                          | Correção                                                                             |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `Connection to localhost:5433 refused`                          | PostgreSQL parado ou container removido                                        | `docker compose up -d` e confirme com `docker compose ps`                            |
| `Connection to localhost:5432 refused` ao subir o backend       | Padrão de `DB_URL` usa a porta 5432, mas o Compose expõe a 5433                | Defina `DB_URL=jdbc:postgresql://localhost:5433/gestao` antes de iniciar             |
| Frontend sem dados e erro de rede no console                    | API não está rodando em `http://localhost:8080`                                | Suba o backend (seção 2); o padrão do frontend já aponta para essa URL               |
| `E-mail ou senha inválidos` para as credenciais de demonstração | `APP_SEED_DEMO=false`, `APP_SEED_PASSWORD` diferente ou usuários já existentes | Confira a configuração e consulte os usuários; o seed só atua quando não há usuários |
| Erro de CORS ou cookie/sessão não persistido                    | Frontend mudou de host/porta sem atualizar CORS ou hosts incompatíveis         | Ajuste `APP_CORS_ALLOWED_ORIGINS`, reinicie a API e mantenha os hosts consistentes   |
| Testes de integração não foram executados                       | Docker ausente/incompatível e sem `GESTAO_TEST_DB_URL`/`-Dtest.db.url`         | Configure um banco de testes isolado, atualize o Docker ou use `-Dtest.db.url`       |

O backend mantém o pool de conexões ativo: se o PostgreSQL cair e voltar na
mesma porta, as requisições seguintes se reconectam sem reiniciar a API. Se o
container for recriado com o volume `gestao-postgres-data`, os dados continuam
preservados.

## Credenciais de demonstração

Criadas por `DemoDataSeeder` quando `APP_SEED_DEMO=true` **e a tabela de
usuários está vazia**:

| E-mail                  | Perfil   | Lojas           |
|-------------------------|----------|-----------------|
| `admin@gestao.local`    | ADMIN    | todas           |
| `gerente@gestao.local`  | GERENTE  | Loja 1 - Matriz |
| `operador@gestao.local` | OPERADOR | Loja 1 - Matriz |

Senha padrão: `gestao123` (configurável por `APP_SEED_PASSWORD` **antes da
primeira carga**). Alterar essa variável após a criação dos usuários não altera
suas senhas persistidas.

> **Somente desenvolvimento.** Em produção, defina `APP_SEED_DEMO=false`,
> forneça credenciais fortes e exclusivas para o banco e usuários, configure
> `APP_COOKIE_SECURE=true` atrás de HTTPS e restrinja as origens CORS. Não
> publique um arquivo `.env` com segredos.

## Variáveis de ambiente

| Variável                   | Padrão                                        | Descrição                                       |
|----------------------------|-----------------------------------------------|-------------------------------------------------|
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/gestao`     | JDBC do PostgreSQL                              |
| `DB_USERNAME`              | `gestao`                                      | Usuário do banco                                |
| `DB_PASSWORD`              | `gestao`                                      | Senha do banco                                  |
| `DB_POOL_SIZE`             | `10`                                          | Conexões do HikariCP                            |
| `APP_PORT`                 | `8080`                                        | Porta da API                                    |
| `APP_SESSION_TIMEOUT`      | `8h`                                          | Expiração da sessão                             |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8443,http://127.0.0.1:8443` | Origens liberadas (separadas por vírgula)       |
| `APP_COOKIE_SECURE`        | `false`                                       | Exige HTTPS nos cookies de sessão/CSRF          |
| `APP_SEED_DEMO`            | `true`                                        | Cria usuários e catálogo de demonstração        |
| `APP_SEED_PASSWORD`        | `gestao123`                                   | Senha inicial dos usuários de demonstração      |
| `POSTGRES_DB`              | `gestao`                                      | Nome do banco inicial (Compose)                 |
| `POSTGRES_USER`            | `gestao`                                      | Usuário do PostgreSQL (Compose)                 |
| `POSTGRES_PASSWORD`        | `gestao`                                      | Senha do PostgreSQL (Compose)                   |
| `POSTGRES_PORT`            | `5433`                                        | Porta do PostgreSQL no host (Compose)           |
| `PORT`                     | `8443`                                        | Porta do Vite (`dev` e `preview`)               |
| `VITE_API_BASE_URL`        | `http://localhost:8080/api/v1`                | URL da API usada pelo frontend                  |
| `GESTAO_TEST_DB_URL`       | —                                             | Banco externo isolado para testes de integração |
| `GESTAO_TEST_DB_USERNAME`  | `gestao`                                      | Usuário desse banco de testes                   |
| `GESTAO_TEST_DB_PASSWORD`  | `gestao`                                      | Senha desse banco de testes                     |

`POSTGRES_*` são consumidas pelo Compose; `DB_*` e `APP_*` pela API; `PORT`
pelo Vite; `VITE_API_BASE_URL` pelo frontend. `GESTAO_TEST_DB_*` são lidas
pela suíte `ApiIntegrationTest` quando se escolhe um PostgreSQL externo.

## API

Base: `/api/v1`. As rotas de autenticação `GET /auth/csrf` e
`POST /auth/login` são públicas (assim como o Swagger/OpenAPI); as demais
rotas de negócio exigem sessão autenticada. Operações de escrita, **inclusive
login e logout**, exigem o token CSRF devolvido por `GET /auth/csrf`, enviado
no header `X-XSRF-TOKEN`. O cliente `frontend/src/services/api.js` já gerencia
o cookie de sessão, o token CSRF e o envio de credenciais.

As listagens paginadas recebem, conforme o endpoint, `page`, `size`, `sort` e
filtros. A maioria dos endpoints de negócio também aceita `storeId` (UUID) como
query param; quando omitido, o backend resolve uma loja acessível ao usuário.

### Autenticação

| Método | Rota           | Descrição                                  |
|--------|----------------|--------------------------------------------|
| `GET`  | `/auth/csrf`   | Devolve `headerName` e `token`             |
| `POST` | `/auth/login`  | Cria a sessão (`GESTAO_SESSION`, HttpOnly) |
| `GET`  | `/auth/me`     | Sessão atual, perfil e lojas vinculadas    |
| `POST` | `/auth/logout` | Encerra a sessão                           |

### Produtos, estoque e movimentações

| Método   | Rota                    | Descrição                                                                                         |
|----------|-------------------------|---------------------------------------------------------------------------------------------------|
| `GET`    | `/products`             | Lista paginada; filtros `search`, `category`, `status`, `sort`, `page`, `size`, `includeInactive` |
| `GET`    | `/products/categories`  | Categorias da loja                                                                                |
| `GET`    | `/products/{id}`        | Detalhe                                                                                           |
| `POST`   | `/products`             | Cria produto; `quantity` inicial gera entrada auditável                                           |
| `PUT`    | `/products/{id}`        | Atualiza cadastro e `version` (lock otimista)                                                     |
| `DELETE` | `/products/{id}`        | Arquiva produto, preservando histórico                                                            |
| `GET`    | `/stock`                | Indicadores e produtos paginados do estoque                                                       |
| `GET`    | `/stock/movements`      | Histórico completo com filtros e paginação                                                        |
| `GET`    | `/stock/movements/{id}` | Detalhe da movimentação                                                                           |
| `POST`   | `/stock/movements`      | Entrada, saída ou ajuste                                                                          |

### Clientes, vendas e relatórios

| Método           | Rota                        | Descrição                      |
|------------------|-----------------------------|--------------------------------|
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

| Método     | Rota                      | Descrição                                        |
|------------|---------------------------|--------------------------------------------------|
| `GET`      | `/dashboard`              | Indicadores e últimas movimentações              |
| `GET`      | `/dashboard/movements`    | Tendência de movimentações (`days`, padrão 7)    |
| `GET`      | `/dashboard/low-stock`    | Produtos com baixo estoque (até 20)              |
| `GET`      | `/dashboard/out-of-stock` | Produtos sem estoque (até 20)                    |
| `GET/POST` | `/users`                  | Listagem e cadastro de usuários (**ADMIN**)      |
| `PUT`      | `/users/{id}`             | Atualização de usuário (**ADMIN**)               |
| `GET`      | `/stores`                 | Lojas acessíveis ao usuário autenticado          |
| `GET`      | `/stores/{id}`            | Detalhe de loja acessível ao usuário autenticado |
| `POST`     | `/stores`                 | Cadastro de loja (**ADMIN**)                     |
| `PUT`      | `/stores/{id}`            | Atualização de loja (**ADMIN**)                  |

A maioria das rotas aceita `storeId`. O backend valida o vínculo do usuário
com a loja em consultas e mutações, conforme o caso.

## Regras de negócio e segurança

- **Multi-loja**: ADMIN acessa as lojas ativas; GERENTE e OPERADOR acessam
  apenas as lojas vinculadas. A validação ocorre no backend, não somente na UI.
  A consulta de lojas acessíveis é permitida aos três perfis, mas o cadastro
  e a edição de lojas exigem ADMIN.
- **Permissões de escrita**: ADMIN/GERENTE podem cadastrar e alterar produtos,
  clientes e vendas; ADMIN/GERENTE podem ajustar estoque; apenas ADMIN gerencia
  usuários e lojas. OPERADOR pode consultar os dados de suas lojas e registrar
  entradas/saídas de estoque, não ajustes.
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
  usuário, motivo, observação e saldos anterior/posterior. Há paginação no
  histórico, não um limite fixo de 20 registros.
- **Sessão**: cookie `GESTAO_SESSION` HttpOnly, SameSite=Lax; CSRF por cookie
  `XSRF-TOKEN` e header `X-XSRF-TOKEN`.
- **Senhas**: BCrypt. Nenhum dado sensível é exposto nas respostas.
- **Schema**: Flyway é a única fonte de DDL (`ddl-auto=validate`).

## Testes e qualidade

### Backend

```bash
cd backend

# Executa testes unitários e de arquitetura; a integração usa Testcontainers
# quando há Docker compatível disponível.
./mvnw test

# Uma classe específica (unitária ou de integração).
./mvnw test -Dtest=ProductTest

# Alternativa: PostgreSQL externo em um banco exclusivo para testes.
./mvnw test "-Dtest.db.url=jdbc:postgresql://localhost:5433/gestao_test"
```

No PowerShell, use `.\mvnw.cmd` e mantenha a propriedade entre aspas.
Para outro usuário/senha do banco de testes, defina
`GESTAO_TEST_DB_USERNAME`/`GESTAO_TEST_DB_PASSWORD` ou passe
`-Dtest.db.username=...` e `-Dtest.db.password=...`.

**Importante:** `ApiIntegrationTest` é condicionado à disponibilidade de um
banco: se não houver Docker compatível com o Testcontainers nem
`GESTAO_TEST_DB_URL` (ou `-Dtest.db.url`), esses testes **são ignorados**, o que
também pode ocorrer com um Docker instalado mas com API antiga/incompatível.
Verifique o resumo dos testes para confirmar que a integração realmente rodou.
Os testes de integração dependem da carga de demonstração (lojas da V2 e do
`DemoDataSeeder`), então mantenha `APP_SEED_DEMO=true` e utilize apenas banco
vazio, descartável ou isolado — eles gravam dados e aplicam as migrações Flyway.

A suíte inclui casos de uso de produtos, estoque, movimentações e vendas,
restrições arquiteturais (ArchUnit) e cenários HTTP de autenticação, CSRF,
isolamento entre lojas, paginação e validação.

### Frontend

```bash
cd frontend
pnpm install           # caso ainda não tenha instalado as dependências
pnpm test              # Vitest + jsdom (todos os testes)
pnpm test products     # filtra arquivos/testes pelo nome
pnpm test:watch        # reexecuta em modo watch
pnpm typecheck         # tsc --noEmit (configs/declarações; não checa os .js)
pnpm format:check      # oxfmt em src, scripts e configs do Vite/Vitest
pnpm format            # corrige a formatação com oxfmt
pnpm build             # verifica a compilação de produção
```

Não há pipeline de CI no repositório; execute essas verificações localmente
antes de concluir uma alteração.

## Migrações

| Versão                              | Conteúdo                                                                     |
|-------------------------------------|------------------------------------------------------------------------------|
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
- Os botões **Exportar CSV** geram o arquivo a partir dos itens **da página
  atual** da tabela; não há exportação completa de todas as páginas pelo backend.
- O frontend usa criação segura de DOM (`textContent`/`createElement`) para
  dados vindos da API, evitando XSS.
- Em ambientes com Docker indisponível ou com cliente/API incompatível, o
  Testcontainers pode não detectar o daemon; use `-Dtest.db.url` como alternativa.
- Antes de colocar o sistema em produção, revise as credenciais padrão,
  habilite HTTPS, controle o CORS e configure backup/monitoramento do banco.

## Equipe

| Nome                              | Matrícula |
|-----------------------------------|-----------|
| Lívia da Silva Teixeira de Barros | 2653161   |
| Davi Cavalcante França            | 2671244   |
| Tayna Santos Santana Gonzáles     | 2636576   |
| Josué dos Santos Lourenço         | 2628200   |

**Orientador:** Prof. Me. Ícaro Santos Ferreira
