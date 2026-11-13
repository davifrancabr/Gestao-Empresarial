import { api } from "../services/api.js"
import { navigate, state } from "../context.js"
import { openMovementModal } from "./movements.js"
import { createPagination } from "../components/pagination.js"
import {
  emptyState,
  errorState,
  loadingState,
  panel,
} from "../components/states.js"
import { h, svgIcon } from "../utils/dom.js"
import {
  formatDateTime,
  formatNumber,
  productStatus,
} from "../utils/formatters.js"

const SORT_OPTIONS = [
  { value: "name,asc", label: "Nome (A–Z)" },
  { value: "quantity,asc", label: "Menor saldo" },
  { value: "quantity,desc", label: "Maior saldo" },
  { value: "minimum,desc", label: "Maior mínimo" },
  { value: "updatedAt,desc", label: "Atualizados recentemente" },
]

function statCard(label, value, icon, tone = "") {
  return h(
    "article",
    { class: `stat-card ${tone}`.trim() },
    h("div", { class: "stat-icon", "aria-hidden": "true" }, svgIcon(icon)),
    h(
      "div",
      { class: "stat-copy" },
      h("p", { text: label }),
      h("strong", { text: value }),
    ),
  )
}

function statusBadge(status) {
  const info = productStatus(status)
  return h(
    "span",
    { class: `status ${info.className}` },
    h("span", { class: "status-dot", "aria-hidden": "true" }),
    info.label,
  )
}

export async function render(container) {
  const filters = {
    search: "",
    category: "",
    status: "",
    sort: "name,asc",
    page: 0,
    size: 10,
  }
  let categories = []
  let requestId = 0

  const statsArea = h("section", {
    class: "stats-grid stats-grid-extended",
    "aria-label": "Resumo do estoque",
  })
  const alertArea = h("div")
  const tableArea = h("div", { class: "table-area" })
  const paginationArea = h("div")
  const resultsSummary = h("p", {
    class: "results-summary",
    "aria-live": "polite",
  })

  const search = h("input", {
    type: "search",
    placeholder: "Buscar por nome ou SKU…",
    "aria-label": "Buscar no estoque",
    autocomplete: "off",
  })
  const categorySelect = h(
    "select",
    { "aria-label": "Filtrar por categoria" },
    h("option", { value: "" }, "Todas as categorias"),
  )
  const statusSelect = h(
    "select",
    { "aria-label": "Filtrar por situação" },
    h("option", { value: "" }, "Todas as situações"),
    h("option", { value: "NORMAL" }, "Normal"),
    h("option", { value: "LOW" }, "Estoque baixo"),
    h("option", { value: "OUT_OF_STOCK" }, "Esgotado"),
  )
  const sortSelect = h(
    "select",
    { "aria-label": "Ordenar estoque" },
    SORT_OPTIONS.map((option) =>
      h("option", { value: option.value }, option.label),
    ),
  )
  const sizeSelect = h(
    "select",
    { "aria-label": "Itens por página" },
    [10, 20, 50].map((size) =>
      h("option", { value: String(size) }, `${size} por página`),
    ),
  )
  const clearButton = h(
    "button",
    {
      class: "button button-secondary button-small",
      type: "button",
      hidden: true,
    },
    "Limpar filtros",
  )

  const toolbar = h(
    "div",
    { class: "toolbar" },
    h("div", { class: "toolbar-field toolbar-search" }, search),
    h("div", { class: "toolbar-field" }, categorySelect),
    h("div", { class: "toolbar-field" }, statusSelect),
    h("div", { class: "toolbar-field" }, sortSelect),
    h("div", { class: "toolbar-field" }, sizeSelect),
    clearButton,
  )

  const heading = h(
    "div",
    { class: "section-header" },
    h(
      "div",
      {},
      h("p", { class: "section-kicker", text: "Controle" }),
      h("h2", { text: "Estoque" }),
      h("p", {
        class: "section-description",
        text: "Acompanhe saldos, alertas de reposição e a classificação automática de cada produto.",
      }),
    ),
    h(
      "div",
      { class: "section-actions" },
      h(
        "button",
        {
          class: "button button-primary",
          type: "button",
          onClick: () => openMovementModal({ onSaved: load }),
        },
        svgIcon("plus"),
        "Movimentar estoque",
      ),
    ),
  )

  function resetFilters() {
    Object.assign(filters, {
      search: "",
      category: "",
      status: "",
      sort: "name,asc",
      page: 0,
    })
    search.value = ""
    categorySelect.value = ""
    statusSelect.value = ""
    sortSelect.value = "name,asc"
    load()
  }

  function hasFilters() {
    return Boolean(filters.search || filters.category || filters.status)
  }

  function renderStats(overview) {
    statsArea.replaceChildren(
      statCard(
        "Produtos ativos",
        formatNumber(overview.totalProducts),
        "package",
      ),
      statCard(
        "Estoque normal",
        formatNumber(overview.normal),
        "check",
        "stat-normal",
      ),
      statCard("Estoque baixo", formatNumber(overview.low), "down", "stat-low"),
      statCard(
        "Esgotados",
        formatNumber(overview.outOfStock),
        "close",
        "stat-empty",
      ),
      statCard(
        "Necessitam reposição",
        formatNumber(overview.needsRestock),
        "alert",
        "stat-low",
      ),
      statCard(
        "Unidades em estoque",
        formatNumber(overview.totalUnits),
        "layers",
        "stat-normal",
      ),
    )
    alertArea.replaceChildren(
      overview.needsRestock > 0
        ? h(
            "div",
            { class: "inline-alert inline-alert-warning", role: "status" },
            svgIcon("alert"),
            h(
              "p",
              {},
              h("strong", { text: "Reposição recomendada. " }),
              `${formatNumber(overview.needsRestock)} ${
                overview.needsRestock === 1
                  ? "produto precisa"
                  : "produtos precisam"
              } de reposição (estoque baixo ou esgotado).`,
            ),
            h(
              "button",
              {
                class: "button button-secondary button-small",
                type: "button",
                onClick: () => {
                  filters.status = "LOW"
                  statusSelect.value = "LOW"
                  filters.page = 0
                  load()
                },
              },
              "Ver estoque baixo",
            ),
          )
        : h(
            "div",
            { class: "inline-alert inline-alert-success", role: "status" },
            svgIcon("check"),
            h("p", {
              text: "Nenhum produto precisa de reposição neste momento.",
            }),
          ),
    )
  }

  function row(product) {
    const status = productStatus(product.status)
    const action = h(
      "div",
      { class: "row-actions" },
      h(
        "button",
        {
          class: "button button-secondary button-small",
          type: "button",
          onClick: () => openMovementModal({ product, onSaved: load }),
        },
        svgIcon("movements"),
        "Movimentar",
      ),
    )
    const suggestion =
      product.quantity === 0
        ? `Repor ao menos ${formatNumber(Math.max(product.minimum, 1))} ${product.unit}`
        : product.quantity <= product.minimum
          ? `Repor ${formatNumber(product.minimum - product.quantity + 1)} ${product.unit}`
          : "—"

    return h(
      "tr",
      {},
      h(
        "td",
        { "data-label": "Produto" },
        h(
          "div",
          { class: "cell-primary" },
          h("strong", { text: product.name }),
          h("span", {
            class: "cell-muted",
            text:
              [product.sku, product.category].filter(Boolean).join(" · ") ||
              "Sem categoria",
          }),
        ),
      ),
      h(
        "td",
        { "data-label": "Disponível" },
        h("strong", {
          text: `${formatNumber(product.quantity)} ${product.unit}`,
        }),
      ),
      h(
        "td",
        { "data-label": "Mínimo" },
        `${formatNumber(product.minimum)} ${product.unit}`,
      ),
      h("td", { "data-label": "Situação" }, statusBadge(product.status)),
      h(
        "td",
        {
          "data-label": "Reposição sugerida",
          class:
            status.className === "status-normal"
              ? "cell-muted"
              : "cell-attention",
        },
        suggestion,
      ),
      h(
        "td",
        { "data-label": "Atualizado em" },
        formatDateTime(product.updatedAt),
      ),
      h("td", { "data-label": "Ações", class: "cell-actions" }, action),
    )
  }

  function renderTable(page) {
    if (!page.content.length) {
      tableArea.replaceChildren(
        hasFilters()
          ? emptyState({
              title: "Nenhum item encontrado",
              message: "Nenhum produto corresponde aos filtros selecionados.",
              actionLabel: "Limpar filtros",
              onAction: resetFilters,
            })
          : emptyState({
              title: "Estoque vazio",
              message:
                "Cadastre produtos para acompanhar o estoque desta loja.",
              actionLabel: "Ir para produtos",
              onAction: () => navigate("produtos"),
            }),
      )
      paginationArea.replaceChildren()
      resultsSummary.textContent = "Nenhum item para exibir."
      return
    }

    tableArea.replaceChildren(
      h(
        "div",
        { class: "table-wrapper" },
        h(
          "table",
          { class: "data-table" },
          h("caption", {
            class: "sr-only",
            text: "Posição de estoque dos produtos",
          }),
          h(
            "thead",
            {},
            h(
              "tr",
              {},
              h("th", { scope: "col" }, "Produto"),
              h("th", { scope: "col" }, "Disponível"),
              h("th", { scope: "col" }, "Mínimo"),
              h("th", { scope: "col" }, "Situação"),
              h("th", { scope: "col" }, "Reposição sugerida"),
              h("th", { scope: "col" }, "Atualizado em"),
              h(
                "th",
                { scope: "col", class: "cell-actions" },
                h("span", { class: "sr-only", text: "Ações" }),
              ),
            ),
          ),
          h("tbody", {}, page.content.map(row)),
        ),
      ),
    )
    resultsSummary.textContent = `${formatNumber(page.totalElements)} ${
      page.totalElements === 1 ? "produto" : "produtos"
    } no estoque.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "produtos",
        onChange: (nextPage) => {
          filters.page = nextPage
          load()
        },
      }),
    )
  }

  async function loadCategories() {
    try {
      const result = await api.get("/products/categories", {
        query: { storeId: state.storeId },
      })
      categories = Array.isArray(result)
        ? result
            .map((category) =>
              typeof category === "string" ? category : category.name,
            )
            .filter(Boolean)
        : []
      const current = filters.category
      categorySelect.replaceChildren(
        h("option", { value: "" }, "Todas as categorias"),
        ...categories.map((category) =>
          h("option", { value: category }, category),
        ),
      )
      if (current) categorySelect.value = current
    } catch {
      // Categorias são um filtro complementar.
    }
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Carregando estoque…"))
    resultsSummary.textContent = ""
    clearButton.hidden = !hasFilters()

    try {
      const overview = await api.get("/stock", {
        query: {
          storeId: state.storeId,
          search: filters.search || undefined,
          category: filters.category || undefined,
          status: filters.status || undefined,
          sort: filters.sort,
          page: filters.page,
          size: filters.size,
        },
      })
      if (current !== requestId) return
      renderStats(overview)
      const page = overview.products
      if (page.totalPages > 0 && filters.page >= page.totalPages) {
        filters.page = page.totalPages - 1
        load()
        return
      }
      renderTable(page)
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  let searchTimer
  search.addEventListener("input", () => {
    window.clearTimeout(searchTimer)
    searchTimer = window.setTimeout(() => {
      filters.search = search.value.trim()
      filters.page = 0
      load()
    }, 350)
  })
  categorySelect.addEventListener("change", () => {
    filters.category = categorySelect.value
    filters.page = 0
    load()
  })
  statusSelect.addEventListener("change", () => {
    filters.status = statusSelect.value
    filters.page = 0
    load()
  })
  sortSelect.addEventListener("change", () => {
    filters.sort = sortSelect.value
    filters.page = 0
    load()
  })
  sizeSelect.addEventListener("change", () => {
    filters.size = Number(sizeSelect.value)
    filters.page = 0
    load()
  })
  clearButton.addEventListener("click", resetFilters)

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      heading,
      statsArea,
      alertArea,
      panel("stock-panel", toolbar, resultsSummary, tableArea, paginationArea),
    ),
  )
  await loadCategories()
  await load()
}
