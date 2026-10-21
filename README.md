# Sistema de Gestão e Controle de Estoque para Supermercados

Sistema web para gerenciar o estoque de mercados e redes de supermercados: centraliza informações de produtos, registra entradas e saídas, emite alertas de reposição e simula a demanda futura com base no histórico de vendas.

---

## Sumário

- [Contexto e problema](#contexto-e-problema)
- [Objetivos](#objetivos)
- [Público-alvo](#público-alvo)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Modelo de dados inicial](#modelo-de-dados-inicial)
- [Como executar](#como-executar)
- [Roadmap](#roadmap)
- [Equipe](#equipe)

---

## Contexto e problema

O controle de estoque impacta diretamente o fluxo de caixa, a satisfação do cliente e a competitividade do negócio. A falta de produtos prejudica o atendimento; o excesso gera estoque parado e desperdício financeiro.

Micro, pequenas e médias empresas costumam controlar o estoque de forma manual (planilhas ou anotações físicas), o que leva a:

- informações espalhadas em vários lugares;
- dificuldade para gerenciar e consultar os dados;
- falta de organização e de rastreabilidade das movimentações;
- decisões de compra tardias ou desnecessárias.

## Objetivos

Desenvolver um sistema de gestão e controle de estoque que centralize as informações de movimentação de mercadorias, permita acompanhar os produtos, gere alertas de reposição, utilize o histórico de vendas para simular a demanda e seja acessível, prático e escalável para diferentes portes de negócio, sem exigir conhecimento técnico avançado.

**Objetivos específicos:**

- [ ] Organizar e facilitar o acompanhamento dos produtos em estoque;
- [ ] Auxiliar na identificação da necessidade de reposição, evitando a falta de produtos;
- [ ] Reduzir desperdícios causados por excesso e estoque parado;
- [ ] Usar dados de demanda em simulações que apoiem o planejamento de reposição.

## Público-alvo

- Supermercados e mercados;
- Pequenos e médios estabelecimentos;
- Responsáveis pela administração da empresa.

## Funcionalidades

### Dashboard

- Indicadores gerais de estoque
- Alertas de reposição próxima
- Gráficos de tendência

### Produtos e estoque

- Cadastro completo de produtos
- Registro de entradas e saídas
- Identificação de itens com baixo giro

### Clientes e vendas

- Cadastro de dados básicos dos clientes
- Histórico de compras
- Apoio à fidelização

### Simulação de demanda

- Projeção de tendências de consumo a partir do histórico de vendas e movimentações
- Apoio ao planejamento de compras em períodos sazonais e promocionais

## Tecnologias

| Camada         | Tecnologia                                           |
| -------------- | ---------------------------------------------------- |
| Banco de dados | PostgreSQL                                           |
| Frontend       | HTML5, CSS3, Bootstrap, JavaScript                   |
| Backend        | Java |
| Ferramentas    | Git, VS Code                                         |


## Arquitetura

Arquitetura em camadas, com frontend e backend desacoplados e comunicação via API REST (JSON).

```
┌──────────────────────┐      HTTP / JSON      ┌─────────────────────────┐      SQL      ┌──────────────┐
│  Frontend            │ ────────────────────▶ │  Backend (API REST)     │ ────────────▶ │  PostgreSQL  │
│  HTML + CSS +        │ ◀──────────────────── │   Java                  │ ◀──────────── │              │
│  Bootstrap + JS      │                       │  Controllers / Services │               │              │
└──────────────────────┘                       │  Repositórios           │               └──────────────┘
                                               └─────────────────────────┘
```

Camadas do backend: **rotas/controllers** → **serviços** (regras de negócio: alertas, giro, simulação de demanda) → **repositórios** (acesso ao banco).

## Estrutura de pastas

```
.
├── backend/              # API
├── frontend/
│   ├── index.html
│   ├── pages/            # dashboard, produtos, estoque, clientes, vendas
│   ├── css/
│   ├── js/
│   └── assets/
├── database/
│   ├── schema.sql        # criação das tabelas
│   └── seed.sql          # dados de exemplo
├── docs/                 # relatório (ABNT), slides, diagramas
└── README.md
```

## Modelo de dados inicial

Proposta inicial, a ser refinada conforme o desenvolvimento. Já considera uma **rede** com mais de uma loja.

| Tabela                   | Finalidade                                                               |
| ------------------------ | ------------------------------------------------------------------------ |
| `lojas`                  | Unidades da rede                                                         |
| `usuarios`               | Acesso ao sistema e perfis (administrador, gerente, operador)            |
| `categorias`             | Classificação dos produtos                                               |
| `fornecedores`           | Origem das mercadorias                                                   |
| `produtos`               | Cadastro de produtos (código de barras, preço, estoque mínimo, validade) |
| `estoque`                | Quantidade atual por produto e por loja                                  |
| `movimentacoes_estoque`  | Entradas, saídas, ajustes e perdas                                       |
| `clientes`               | Dados básicos e apoio à fidelização                                      |
| `vendas` / `itens_venda` | Histórico de compras e base para a simulação de demanda                  |

Exemplo (trecho):

```sql
CREATE TABLE produtos (
    id              SERIAL PRIMARY KEY,
    codigo_barras   VARCHAR(50) UNIQUE,
    nome            VARCHAR(150) NOT NULL,
    categoria_id    INT REFERENCES categorias(id),
    fornecedor_id   INT REFERENCES fornecedores(id),
    preco_venda     NUMERIC(10,2) NOT NULL,
    estoque_minimo  INT NOT NULL DEFAULT 0,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE movimentacoes_estoque (
    id          SERIAL PRIMARY KEY,
    produto_id  INT NOT NULL REFERENCES produtos(id),
    loja_id     INT NOT NULL REFERENCES lojas(id),
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA','SAIDA','AJUSTE','PERDA')),
    quantidade  INT NOT NULL,
    data_hora   TIMESTAMP NOT NULL DEFAULT NOW(),
    usuario_id  INT REFERENCES usuarios(id)
);
```

## Como executar

### Pré-requisitos

- PostgreSQL 15+
- JDK 21+ com Maven

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd <nome-do-projeto>
```

### 2. Banco de dados

```bash
psql -U postgres -c "CREATE DATABASE estoque;"
psql -U postgres -d estoque -f database/schema.sql
psql -U postgres -d estoque -f database/seed.sql   # opcional
```

### 3. Backend

**Opção B: Java (Spring Boot)**

```bash
cd backend
./mvnw spring-boot:run
```

Configure a conexão em `application.properties` (URL, usuário e senha do PostgreSQL).

### 4. Frontend

Abra `frontend/index.html` no navegador ou sirva a pasta com um servidor estático:

```bash
cd frontend
python -m http.server 5500
```

Acesse: <http://localhost:5500>

### Variáveis de ambiente (exemplo)

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=estoque
DB_USER=postgres
DB_PASSWORD=sua_senha
API_PORT=8001
```

## Roadmap

- [ ] Definir a tecnologia do backend (Python ou Java)
- [ ] Modelagem do banco de dados e diagramas (ER, casos de uso)
- [ ] Autenticação e perfis de usuário
- [ ] CRUD de produtos, categorias e fornecedores
- [ ] Registro de entradas e saídas de estoque
- [ ] Alertas de reposição (estoque mínimo)
- [ ] Identificação de itens com baixo giro
- [ ] Cadastro de clientes e histórico de compras
- [ ] Dashboard com indicadores e gráficos de tendência
- [ ] Simulação de demanda com base no histórico
- [ ] Suporte a múltiplas lojas
- [ ] Testes e documentação da API

## Equipe

| Nome                              | Matrícula |
| --------------------------------- | --------- |
| Lívia da Silva Teixeira de Barros | 2653161   |
| Davi Cavalcante França            | 2671244   |
| Tayna Santos Santana Gonzáles     | 2636576   |
| Josué dos Santos Lourenço         | 2628200   |

**Orientador:** Prof. Me. Ícaro Santos Ferreira

**Instituição:** Centro Universitário Afya Maceió — Curso de Engenharia de Software

---

<sub>Projeto acadêmico em desenvolvimento. Licença a definir.</sub>
