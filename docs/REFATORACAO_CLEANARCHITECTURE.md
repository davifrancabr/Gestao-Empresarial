# Instrução: remodelar o backend para Clean Architecture

> Destinatário: agente de código ou desenvolvedor. Escopo: **somente `backend/`**.
> O frontend, as migrações Flyway, o contrato HTTP e o comportamento observável
> da API **não mudam**.

## 1. Objetivo

Reorganizar `backend/src/main/java/com/empresa/gestao` para a Clean Architecture,
de modo que as regras de negócio (estoque, venda, multi-loja) não dependam de
Spring, JPA nem de outros módulos por meio de repositórios concretos. Ao final:

- a regra de dependência aponta sempre para dentro
  (`presentation` → `application` → `domain`; `infrastructure` → `application`/`domain`);
- cada módulo de negócio é independente e se comunica com os demais por **portas**;
- as regras são verificadas automaticamente (ArchUnit) e quebram o build se violadas.

É uma refatoração: nenhuma rota, JSON, status HTTP, mensagem de erro, tabela ou
coluna pode mudar.

## 2. Diagnóstico do estado atual

Levantado a partir das importações do código:

| Problema | Onde | Consequência |
| --- | --- | --- |
| Entidades JPA (`Sale`, `Product`, `User`...) são o modelo de domínio | todos os módulos | regra de negócio presa a `jakarta.persistence`; domínio não testável sem JPA |
| Regra de negócio dentro de serviços anêmicos | `MovementService.apply` (cálculo de saldo), `SaleService.create` (total, duplicidade) | entidades sem comportamento; regra espalhada |
| Serviços usam repositórios de outros módulos | `SaleService` → `ProductRepository`, `StoreRepository`, `UserRepository`; `MovementService` → `ProductRepository`, `StoreRepository`, `UserRepository`; `StockService` → `ProductRepository` | acoplamento a persistência alheia |
| Serviços trocam entidades JPA entre si | `movementService.apply(Store, User, Product, ...)` | vazamento de modelo de persistência |
| **Ciclos entre pacotes** | `product` ↔ `movement`; `store` ↔ `security` (`StoreService` usa `CurrentUserService`; `CurrentUserService` usa `StoreRepository`) | impossível isolar módulos |
| Camada de aplicação importa tipos de framework web/dados | `Pageable`, `Page`, `Specification`, `PageUtils` em serviços | casos de uso dependem de Spring Data |
| `report` depende de `dashboard`; `config` depende de quase tudo | `ReportService`, `DemoDataSeeder` | agregação sem fronteira clara |
| Exceções de negócio dependem de HTTP | `ApiException` carrega `HttpStatus` | domínio conhece o protocolo |

## 3. Estrutura alvo

Mantém-se *package-by-feature* no primeiro nível (um pacote por módulo) e
divide-se cada módulo em quatro camadas. Pacote raiz continua `com.empresa.gestao`.

```text
com.empresa.gestao
├── GestaoApplication.java
│
├── shared/                              kernel compartilhado (mínimo e estável)
│   ├── domain/
│   │   ├── DomainException.java         base sem HttpStatus
│   │   ├── BusinessRuleException.java
│   │   ├── NotFoundException.java       (ex-ResourceNotFoundException)
│   │   ├── ConflictException.java
│   │   ├── ForbiddenException.java
│   │   ├── UnauthorizedException.java
│   │   └── util/ (CpfValidator, TextNormalizer)
│   ├── application/
│   │   ├── PageQuery.java               page, size, sort (sem Spring)
│   │   └── PageResult.java              items, page, size, totalElements...
│   └── presentation/web/
│       ├── GlobalExceptionHandler.java  mapeia exceção -> HTTP / ApiError
│       ├── ApiError.java
│       └── PageResponse.java            DTO HTTP de página
│
├── <modulo>/                            auth, user, store, product, movement,
│   │                                    customer, sale, stock, dashboard, report
│   ├── domain/
│   │   ├── model/                       entidades e value objects em Java puro
│   │   ├── repository/                  portas de saída (interfaces) do agregado
│   │   └── service/                     regras que não cabem em um só agregado
│   ├── application/
│   │   ├── usecase/                     um caso de uso por classe/ação
│   │   ├── port/in/  (opcional)         interfaces dos casos de uso
│   │   ├── port/out/                    portas para outros módulos e serviços
│   │   └── dto/                         Command, Query e Result (records, sem Spring)
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── entity/                  *JpaEntity (@Entity, @Table, @Version)
│   │       ├── repository/              Spring Data (*JpaRepository) e Specifications
│   │       ├── mapper/                  JpaEntity <-> domain model
│   │       └── adapter/                 implementa domain.repository.* e port.out.*
│   └── presentation/
│       └── web/
│           ├── *Controller.java
│           ├── request/ e response/     DTOs HTTP com Bean Validation e OpenAPI
│           └── *WebMapper.java          Result <-> Response
│
├── security/                            adaptador de autenticação (infrastructure)
│   ├── AppUserDetailsService, AppUserPrincipal, RestSecurityHandlers
│   └── CurrentUserAdapter               implementa CurrentUserPort
│
└── bootstrap/                           composição e configuração
    ├── SecurityConfig.java
    ├── OpenApiConfig.java
    ├── UseCaseConfig.java (se os casos de uso não usarem @Service)
    └── seed/DemoDataSeeder.java         usa casos de uso, não repositórios
```

Os módulos `stock`, `dashboard` e `report` são **somente leitura**: não têm
`domain/model` próprio. Têm `application/usecase` + `port/out` (consultas) e
`presentation`; a implementação das consultas fica em `infrastructure/persistence`.

## 4. Regra de dependência (inviolável)

| Camada | Pode importar | Não pode importar |
| --- | --- | --- |
| `domain` | JDK e `shared.domain` | Spring, `jakarta.*` (persistence, validation, servlet), Jackson, outras camadas, outros módulos |
| `application` | `domain` do próprio módulo, `shared`, **portas** de outros módulos (`application.port`/`application.dto` públicos) | `infrastructure`, `presentation`, Spring Data (`Page`, `Pageable`, `Specification`), `HttpStatus`, servlet |
| `infrastructure` | `application`, `domain`, Spring Data, JPA, Spring Security | `presentation` |
| `presentation` | `application` (casos de uso e DTOs), `shared` | `domain.model`, `infrastructure` |
| `bootstrap` | tudo (é a raiz de composição) | — |

Exceções pragmáticas aceitas e documentadas (qualquer outra exige aprovação):

- `application` pode usar `@Transactional` e `@Service` do Spring, e nada além
  disso do framework.
- Módulos conversam apenas por: `application.usecase`/`port` e `application.dto`
  do outro módulo. **Nunca** por `domain.model`, `infrastructure` nem `presentation`
  alheios.
- O grafo de dependência entre módulos deve ser **acíclico**.

Direção entre módulos (alvo):

```text
auth ─▶ user, store
sale ─▶ movement, product, customer, store
movement ─▶ product, store, user
product, customer, stock ─▶ store (para validar a loja)
dashboard, report ─▶ product, movement, sale, stock   (leitura)
store, user ─▶ (nenhum módulo de negócio)
```

## 5. Quebra dos ciclos e acoplamentos (obrigatório antes de mover pastas)

1. **`product` ↔ `movement`.** `ProductService` hoje chama `MovementService` para
   gerar a entrada inicial. Inverta: o saldo e a regra de movimentação passam a
   viver no domínio de `product` (ou em um serviço de domínio de estoque), e
   `movement` deixa de ser consultado por `product`.
   - Crie em `product.domain.model` o comportamento que hoje está em
     `MovementService.apply`: `Product.registerEntry(int)`, `registerExit(int)`,
     `adjustTo(int)`, retornando `previous`/`resulting` (por exemplo um
     `StockChange`). As validações (quantidade > 0, saída ≤ saldo, ajuste ≠ saldo
     atual e ≥ 0) vão para lá, com as **mesmas mensagens**.
   - O caso de uso `RecordMovementUseCase` (módulo `movement`) carrega o produto,
     chama o método de domínio, persiste o produto e grava o `StockMovement`
     (com snapshot) na mesma transação. `CreateProductUseCase` usa a porta
     `RecordInitialStockPort` (implementada pelo módulo `movement`) ou publica
     um evento de domínio tratado na mesma transação. Escolha um e aplique de
     forma consistente.
2. **`store` ↔ `security`.** `CurrentUserService.resolveStoreId` mistura
   identidade (security) com regra de acesso a loja (store).
   - Defina a porta `CurrentUserPort` em `shared.application` (ou em `user.application.port`):
     `UUID userId()`, `Role role()`, `Set<UUID> storeIds()`, `boolean isAdmin()`.
   - Mova a regra de `resolveStoreId` para `store.application.usecase.ResolveStoreAccessUseCase`
     (usa `CurrentUserPort` e `StoreRepository`). Controllers chamam esse caso de uso.
   - `security.CurrentUserAdapter` implementa `CurrentUserPort` lendo o
     `SecurityContextHolder`. `StoreService` deixa de importar `security`.
3. **Serviços usando repositórios alheios.** `SaleService`, `MovementService` e
   `StockService` não podem injetar `ProductRepository`/`StoreRepository`/`UserRepository`
   de outro módulo. Substitua por casos de uso ou portas de leitura, por exemplo
   `ProductQueryPort.findActiveByIdAndStore(...)`, `StoreQueryPort.getById(...)`,
   `UserQueryPort.getById(...)`, implementadas no módulo dono.
4. **Entidades JPA entre módulos.** `movementService.apply(Store, User, Product, ...)`
   passa a receber **ids ou modelos de domínio do próprio módulo**. Nenhuma
   `*JpaEntity` cruza a fronteira do módulo.
5. **`report` → `dashboard` e `config` → tudo.** `report` e `dashboard`
   consomem portas de leitura (`SalesQueryPort`, `MovementQueryPort`...) e não um
   ao outro. `DemoDataSeeder` usa casos de uso de `application`.

## 6. Mapeamento dos arquivos atuais

| Atual | Destino |
| --- | --- |
| `<m>/<Entidade>.java` | modelo puro em `<m>/domain/model/<Entidade>.java` **e** `<m>/infrastructure/persistence/entity/<Entidade>JpaEntity.java` |
| `common/entity/BaseEntity`, `AuditableEntity` | `shared/infrastructure/persistence/` (apenas para as `*JpaEntity`) |
| `<m>/<Entidade>Repository.java` (Spring Data) | `<m>/infrastructure/persistence/repository/<Entidade>JpaRepository.java`; interface de domínio em `<m>/domain/repository/<Entidade>Repository.java`; adaptador em `.../adapter/` |
| `<m>/*Specifications.java` | `<m>/infrastructure/persistence/repository/` |
| `<m>/<M>Service.java` | quebrar em casos de uso em `<m>/application/usecase/` (ex.: `CreateSaleUseCase`, `ListSalesUseCase`, `GetSaleUseCase`) |
| `<m>/<M>Mapper.java` | `presentation/web/<M>WebMapper` (Result → Response) e `infrastructure/persistence/mapper` (JPA ↔ domínio) |
| `<m>/<M>Controller.java` | `<m>/presentation/web/` |
| `<m>/dto/*Request` | `application/dto/*Command` (sem anotações de framework) + `presentation/web/request/*Request` (com `@Valid`, `@NotBlank` etc.) |
| `<m>/dto/*Response` | `application/dto/*Result` + `presentation/web/response/*Response` |
| enums (`MovementType`, `SaleStatus`, `ProductStatus`, `Role`) | `<m>/domain/model/` |
| `common/exception/*` | `shared/domain/` (exceções) e `shared/presentation/web/` (`ApiError`, `GlobalExceptionHandler`) |
| `common/dto/PageResponse`, `common/util/PageUtils` | `PageResult`/`PageQuery` em `shared/application`; `PageResponse` e conversão para `Pageable` em `shared/presentation` e `infrastructure` |
| `security/*` | `security/` (adaptador) + `CurrentUserPort` na aplicação |
| `config/SecurityConfig`, `OpenApiConfig`, `DemoDataSeeder` | `bootstrap/` |

Observações:

- Pode-se simplificar onde o benefício é nulo: se `Command`/`Request` forem
  idênticos, mantenha os dois por fronteira mesmo assim; a anotação
  `jakarta.validation` só existe em `presentation`, e a validação de **regra**
  (não de formato) acontece no domínio/caso de uso.
- `ApiException` deixa de carregar `HttpStatus`. O mapeamento
  exceção → status fica em `GlobalExceptionHandler`
  (`NotFoundException`→404, `BusinessRuleException`→422/400, `ConflictException`→409,
  `ForbiddenException`→403, `UnauthorizedException`→401), **preservando exatamente
  os status que a API devolve hoje**. Verifique-os no código antes de mapear.

## 7. Restrições para não quebrar nada

- **Banco**: não alterar migrações nem nomes de tabela/coluna; `ddl-auto` segue
  `validate`. `@Table`/`@Column` migram idênticos para as `*JpaEntity`. `@Version`
  de `Product` permanece na entidade JPA e o lock otimista continua mapeado para
  409 (`ObjectOptimisticLockingFailureException`).
- **Transações**: `CreateSaleUseCase` e `RecordMovementUseCase` continuam atômicos
  (venda + saídas de estoque; saldo + movimentação). Teste de rollback inclusive.
- **Carregamento**: `open-in-view=false`. Os `@EntityGraph` atuais (`SaleRepository`)
  precisam ser preservados nos adaptadores para evitar `LazyInitializationException`
  e N+1.
- **Contrato**: URLs, JSON (`non_null`), validações, mensagens em português e
  ordenação (whitelist em `PageUtils`) idênticos.
- **Segurança**: `@PreAuthorize` permanece nos controllers; CSRF, CORS e cookies
  não mudam; `resolveStoreId` continua sendo chamado em toda rota com `storeId`.
- Escaneamento: `GestaoApplication` fica no pacote raiz, então `@Entity` e
  repositórios Spring Data em subpacotes continuam sendo detectados. Não use
  `@EntityScan`/`@EnableJpaRepositories` a não ser que o build exija.
- Não introduza Lombok, MapStruct nem novas bibliotecas (exceto ArchUnit em
  `test`).

## 8. Plano de execução (um commit/PR por etapa, build verde em cada uma)

**Etapa 0: rede de segurança.** Rode `./mvnw test` e registre o resultado.
Garanta que `ApiIntegrationTest` **executa** (Docker ou `-Dtest.db.url`); sem ele a
refatoração fica sem rede. Se faltar cobertura, acrescente testes de
caracterização para: venda com rollback, ajuste de estoque, isolamento entre
lojas, 401/403/409, ordenação e paginação.

**Etapa 1: kernel compartilhado.** Crie `shared/` e mova exceções, `PageResult`/
`PageQuery`, utilitários e `GlobalExceptionHandler`. `ApiException` perde o
`HttpStatus`. Ajuste imports. Testes verdes.

**Etapa 2: ArchUnit em modo informativo.** Adicione `com.tngtech.archunit:archunit-junit5`
(escopo `test`, versão estável recente) e escreva as regras da seção 9 com
`@ArchTest`; marque as ainda violadas com `FreezingArchRule` ou desative-as por
módulo, e vá ligando conforme cada etapa termina.

**Etapa 3: módulos-folha.** Migre `store` e `user` (sem dependências de
negócio) e crie `CurrentUserPort` + `security.CurrentUserAdapter`, quebrando o
ciclo `store` ↔ `security`. Migre `auth`.

**Etapa 4: catálogo e estoque.** Migre `product` e `movement` juntos, pois é
onde o ciclo está: mova as regras de saldo para o domínio de produto (seção 5.1),
crie `RecordMovementUseCase` e as portas necessárias. Migre `stock`.

**Etapa 5: comercial.** Migre `customer` e `sale` (`CreateSaleUseCase` usa
`RecordMovementUseCase`/porta de saída de estoque, sem repositórios alheios).

**Etapa 6: leitura.** Migre `dashboard` e `report` para portas de consulta.

**Etapa 7: composição.** Mova `SecurityConfig`, `OpenApiConfig`, `DemoDataSeeder`
para `bootstrap/`. Se os casos de uso não usarem `@Service`, registre-os em
`UseCaseConfig`. O seeder usa casos de uso.

**Etapa 8: testes e limpeza.** Reorganize os testes (seção 10), ligue todas as
regras do ArchUnit, remova pacotes vazios e classes antigas, atualize o
`README.md` (seção "Estrutura") e o `AGENTS.md`.

Em cada etapa: mova com `git mv` para preservar histórico, compile, rode todos os
testes e só então avance. Não misture mover arquivos com mudar comportamento no
mesmo commit.

## 9. Verificação automática (ArchUnit)

Crie `src/test/java/com/empresa/gestao/architecture/ArchitectureTest.java` com
regras equivalentes a:

- `..domain..` não depende de `org.springframework..`, `jakarta..`,
  `..application..`, `..infrastructure..`, `..presentation..`;
- `..application..` não depende de `..infrastructure..`, `..presentation..`,
  `org.springframework.data..`, `org.springframework.http..`,
  `org.springframework.web..`, `jakarta.servlet..`, `jakarta.persistence..`;
- `..presentation..` não depende de `..infrastructure..` nem de `..domain.model..`;
- `..infrastructure..` não depende de `..presentation..`;
- módulos não acessam `domain`/`infrastructure`/`presentation` de outros módulos;
- ausência de ciclos entre módulos (`slices().matching("com.empresa.gestao.(*)..").should().beFreeOfCycles()`);
- classes `@Entity` residem somente em `..infrastructure.persistence.entity..`;
- classes `@RestController` residem somente em `..presentation..`.

## 10. Testes

- `domain`: testes unitários **sem Spring e sem Mockito** (ex.: `ProductTest`
  cobrindo entrada, saída, ajuste, saldo negativo e mensagens).
- `application`: Mockito sobre as portas (migrar `SaleServiceTest`,
  `MovementServiceTest`, `ProductServiceTest` para
  `CreateSaleUseCaseTest`, `RecordMovementUseCaseTest`, `CreateProductUseCaseTest`),
  no mesmo pacote do caso de uso testado.
- `infrastructure`: manter cobertura de repositórios/adaptadores pelo teste de
  integração (consultas, `@EntityGraph`, `Specification`).
- `presentation` + ponta a ponta: `ApiIntegrationTest` permanece na raiz e **não
  pode ser alterado para passar**; qualquer divergência de status, JSON ou
  mensagem é regressão a corrigir no código.
- `shared`: `TextNormalizerTest` acompanha a classe.

## 11. Definição de pronto

- [ ] `./mvnw clean verify` verde, com `ApiIntegrationTest` efetivamente executado.
- [ ] `ArchitectureTest` com todas as regras da seção 9 ativas e sem congelamento.
- [ ] Nenhuma migração Flyway alterada; `ddl-auto=validate` passa na subida.
- [ ] Nenhum import de `jakarta.persistence`/`org.springframework` em `..domain..`.
- [ ] Nenhum import de repositório ou entidade de outro módulo.
- [ ] Sem ciclos entre pacotes (ArchUnit).
- [ ] Contrato da API idêntico: o frontend funciona sem mudanças (testar login, produto,
      movimentação, venda e relatório pela UI ou Swagger).
- [ ] `README.md` e `AGENTS.md` refletem a nova estrutura.

## 12. O que o agente deve evitar

- Reescrever regras de negócio "melhorando-as"; só mover e proteger com teste.
- Criar camadas vazias ou interfaces com uma única implementação sem ganho
  (exceto as portas que quebram dependências entre módulos ou camadas).
- Colocar anotações JPA ou Bean Validation no domínio.
- Fazer o domínio devolver DTOs ou `Page`.
- Alterar o frontend, `docker-compose.yml`, `.env.example` ou `docs/`.
- Concluir a etapa com testes desativados, `@Disabled` ou skips silenciosos.