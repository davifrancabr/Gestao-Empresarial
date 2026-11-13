import { api } from "../services/api.js"
import { canAdjust, state } from "../context.js"
import { createPagination } from "../components/pagination.js"
import {
  emptyState,
  errorState,
  loadingState,
  panel,
} from "../components/states.js"
import { h, svgIcon } from "../utils/dom.js"
import {
  formatCurrency,
  formatDateTime,
  formatDateTimeLong,
  formatNumber,
  movementType,
  productStatus,
} from "../utils/formatters.js"

function exportCsv(filename, header, rows) {
  const escape = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`
  const content = [header, ...rows]
    .map((row) => row.map(escape).join(";"))
    .join("\r\n")
  const blob = new Blob([`\uFEFF${content}`], {
    type: "text/csv;charset=utf-8",
  })
  const url = URL.createObjectURL(blob)
  const link = h("a", { href: url, download: filename })
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function reportTable({ caption, columns, items, key }) {
  return h(
    "div",
    { class: "table-wrapper" },
    h(
      "table",
      { class: "data-table" },
      h("caption", { class: "sr-only", text: caption }),
      h(
        "thead",
        {},
        h(
          "tr",
          {},
          columns.map((column) =>
            h("th", { scope: "col", class: column.className }, column.label),
          ),
        ),
      ),
      h(
        "tbody",
        {},
        items.map((item, index) =>
          h(
            "tr",
            { key: `${key}-${index}` },
            columns.map((column) =>
              h(
                "td",
                { "data-label": column.label, class: column.className },
                column.render
                  ? column.render(item, index)
                  : (item[column.key] ?? "—"),
              ),
            ),
          ),
        ),
      ),
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

function toolbarWithSearch({
  searchLabel = "Buscar…",
  search,
  extra = [],
  onChange,
}) {
  return h(
    "div",
    { class: "toolbar" },
    h("div", { class: "toolbar-field toolbar-search" }, search),
    extra,
  )
}

function bindSearch(input, onSearch) {
  let timer
  input.addEventListener("input", () => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => onSearch(input.value.trim()), 350)
  })
}

function categoryOptions(categories, label = "Todas as categorias") {
  return [
    h("option", { value: "" }, label),
    ...categories.map((category) => h("option", { value: category }, category)),
  ]
}

async function loadCategories() {
  try {
    const result = await api.get("/products/categories", {
      query: { storeId: state.storeId },
    })
    return Array.isArray(result)
      ? result
          .map((category) =>
            typeof category === "string" ? category : category.name,
          )
          .filter(Boolean)
      : []
  } catch {
    return []
  }
}

function productsTab(area) {
  const stateTab = {
    search: "",
    category: "",
    status: "",
    sort: "name,asc",
    page: 0,
    size: 20,
    includeInactive: false,
  }
  let requestId = 0
  let categories = []
  let currentItems = []

  const search = h("input", {
    type: "search",
    "aria-label": "Buscar produtos",
    placeholder: "Buscar por nome ou SKU…",
    autocomplete: "off",
  })
  const category = h("select", { "aria-label": "Categoria" })
  const status = h(
    "select",
    { "aria-label": "Situação" },
    h("option", { value: "" }, "Todas as situações"),
    h("option", { value: "NORMAL" }, "Normal"),
    h("option", { value: "LOW" }, "Estoque baixo"),
    h("option", { value: "OUT_OF_STOCK" }, "Esgotado"),
  )
  const sort = h(
    "select",
    { "aria-label": "Ordenar" },
    h("option", { value: "name,asc" }, "Nome (A–Z)"),
    h("option", { value: "quantity,asc" }, "Menor saldo"),
    h("option", { value: "createdAt,desc" }, "Mais recentes"),
  )
  const inactive = h(
    "label",
    { class: "checkbox-label toolbar-checkbox" },
    h("input", { type: "checkbox", "aria-label": "Incluir arquivados" }),
    h("span", { text: "Incluir arquivados" }),
  )
  const exportButton = h(
    "button",
    { class: "button button-secondary button-small", type: "button" },
    svgIcon("download"),
    "Exportar CSV",
  )
  const tableArea = h("div")
  const paginationArea = h("div")
  const summary = h("p", { class: "results-summary", "aria-live": "polite" })

  function render(page) {
    currentItems = page.content
    if (!page.content.length) {
      tableArea.replaceChildren(
        emptyState({
          title: "Nenhum produto",
          message: "Nenhum produto corresponde aos filtros selecionados.",
        }),
      )
      paginationArea.replaceChildren()
      summary.textContent = "Nenhum produto para exibir."
      return
    }

    tableArea.replaceChildren(
      reportTable({
        key: "report-product",
        caption: "Relatório de produtos cadastrados",
        columns: [
          {
            label: "Produto",
            render: (item) =>
              h(
                "div",
                { class: "cell-primary" },
                h("strong", { text: item.name }),
                h("span", { class: "cell-muted", text: item.sku || "Sem SKU" }),
              ),
          },
          { label: "Categoria", key: "category" },
          {
            label: "Disponível",
            render: (item) => `${formatNumber(item.quantity)} ${item.unit}`,
          },
          {
            label: "Mínimo",
            render: (item) => `${formatNumber(item.minimum)} ${item.unit}`,
          },
          { label: "Situação", render: (item) => statusBadge(item.status) },
          {
            label: "Cadastrado em",
            render: (item) => formatDateTime(item.createdAt),
          },
          {
            label: "Ativo",
            render: (item) => (item.active ? "Sim" : "Arquivado"),
          },
        ],
        items: page.content,
      }),
    )
    summary.textContent = `${formatNumber(page.totalElements)} produtos no relatório.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "produtos",
        onChange: (next) => {
          stateTab.page = next
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Gerando relatório…"))
    try {
      const page = await api.get("/reports/products", {
        query: {
          storeId: state.storeId,
          search: stateTab.search || undefined,
          category: stateTab.category || undefined,
          status: stateTab.status || undefined,
          sort: stateTab.sort,
          includeInactive: stateTab.includeInactive,
          page: stateTab.page,
          size: stateTab.size,
        },
      })
      if (current !== requestId) return
      render(page)
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  bindSearch(search, (value) => {
    stateTab.search = value
    stateTab.page = 0
    load()
  })
  category.addEventListener("change", () => {
    stateTab.category = category.value
    stateTab.page = 0
    load()
  })
  status.addEventListener("change", () => {
    stateTab.status = status.value
    stateTab.page = 0
    load()
  })
  sort.addEventListener("change", () => {
    stateTab.sort = sort.value
    stateTab.page = 0
    load()
  })
  inactive.querySelector("input").addEventListener("change", (event) => {
    stateTab.includeInactive = event.target.checked
    stateTab.page = 0
    load()
  })
  exportButton.addEventListener("click", () => {
    exportCsv(
      "relatorio-produtos.csv",
      [
        "Nome",
        "SKU",
        "Categoria",
        "Disponível",
        "Unidade",
        "Mínimo",
        "Situação",
        "Cadastrado em",
        "Ativo",
      ],
      currentItems.map((item) => [
        item.name,
        item.sku ?? "",
        item.category ?? "",
        item.quantity,
        item.unit,
        item.minimum,
        productStatus(item.status).label,
        formatDateTimeLong(item.createdAt),
        item.active ? "Sim" : "Arquivado",
      ]),
    )
  })

  area.replaceChildren(
    panel(
      "report-panel",
      toolbarWithSearch({
        search,
        extra: [
          h("div", { class: "toolbar-field" }, category),
          h("div", { class: "toolbar-field" }, status),
          h("div", { class: "toolbar-field" }, sort),
          inactive,
          exportButton,
        ],
      }),
      summary,
      tableArea,
      paginationArea,
    ),
  )

  loadCategories().then((result) => {
    categories = result
    category.replaceChildren(...categoryOptions(categories))
  })
  load()
}

function stockTab(area) {
  const stateTab = {
    search: "",
    category: "",
    status: "",
    sort: "name,asc",
    page: 0,
    size: 20,
  }
  let requestId = 0
  let currentItems = []

  const search = h("input", {
    type: "search",
    "aria-label": "Buscar no estoque",
    placeholder: "Buscar por nome ou SKU…",
    autocomplete: "off",
  })
  const category = h("select", { "aria-label": "Categoria" })
  const status = h(
    "select",
    { "aria-label": "Situação" },
    h("option", { value: "" }, "Todas as situações"),
    h("option", { value: "NORMAL" }, "Normal"),
    h("option", { value: "LOW" }, "Estoque baixo"),
    h("option", { value: "OUT_OF_STOCK" }, "Esgotado"),
  )
  const sort = h(
    "select",
    { "aria-label": "Ordenar" },
    h("option", { value: "name,asc" }, "Nome (A–Z)"),
    h("option", { value: "quantity,asc" }, "Menor saldo"),
    h("option", { value: "quantity,desc" }, "Maior saldo"),
  )
  const exportButton = h(
    "button",
    { class: "button button-secondary button-small", type: "button" },
    svgIcon("download"),
    "Exportar CSV",
  )
  const statsArea = h("div", { class: "inline-stats" })
  const tableArea = h("div")
  const paginationArea = h("div")
  const summary = h("p", { class: "results-summary", "aria-live": "polite" })

  function render(overview) {
    statsArea.replaceChildren(
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(overview.totalProducts) }),
        " produtos",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(overview.normal) }),
        " normais",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(overview.low) }),
        " com estoque baixo",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(overview.outOfStock) }),
        " esgotados",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(overview.totalUnits) }),
        " unidades",
      ),
    )
    const page = overview.products
    currentItems = page.content
    if (!page.content.length) {
      tableArea.replaceChildren(
        emptyState({
          title: "Nenhum item",
          message: "Nenhum produto corresponde aos filtros.",
        }),
      )
      paginationArea.replaceChildren()
      summary.textContent = "Nenhum item para exibir."
      return
    }
    tableArea.replaceChildren(
      reportTable({
        key: "report-stock",
        caption: "Relatório de estoque atual",
        columns: [
          {
            label: "Produto",
            render: (item) =>
              h(
                "div",
                { class: "cell-primary" },
                h("strong", { text: item.name }),
                h("span", {
                  class: "cell-muted",
                  text: item.category || "Sem categoria",
                }),
              ),
          },
          {
            label: "Disponível",
            render: (item) => `${formatNumber(item.quantity)} ${item.unit}`,
          },
          {
            label: "Mínimo",
            render: (item) => `${formatNumber(item.minimum)} ${item.unit}`,
          },
          { label: "Situação", render: (item) => statusBadge(item.status) },
          {
            label: "Atualizado em",
            render: (item) => formatDateTime(item.updatedAt),
          },
        ],
        items: page.content,
      }),
    )
    summary.textContent = `${formatNumber(page.totalElements)} produtos no estoque.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "produtos",
        onChange: (next) => {
          stateTab.page = next
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Gerando relatório…"))
    try {
      const overview = await api.get("/reports/stock", {
        query: {
          storeId: state.storeId,
          search: stateTab.search || undefined,
          category: stateTab.category || undefined,
          status: stateTab.status || undefined,
          sort: stateTab.sort,
          page: stateTab.page,
          size: stateTab.size,
        },
      })
      if (current !== requestId) return
      render(overview)
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  bindSearch(search, (value) => {
    stateTab.search = value
    stateTab.page = 0
    load()
  })
  category.addEventListener("change", () => {
    stateTab.category = category.value
    stateTab.page = 0
    load()
  })
  status.addEventListener("change", () => {
    stateTab.status = status.value
    stateTab.page = 0
    load()
  })
  sort.addEventListener("change", () => {
    stateTab.sort = sort.value
    stateTab.page = 0
    load()
  })
  exportButton.addEventListener("click", () => {
    exportCsv(
      "relatorio-estoque.csv",
      [
        "Produto",
        "SKU",
        "Categoria",
        "Disponível",
        "Unidade",
        "Mínimo",
        "Situação",
        "Atualizado em",
      ],
      currentItems.map((item) => [
        item.name,
        item.sku ?? "",
        item.category ?? "",
        item.quantity,
        item.unit,
        item.minimum,
        productStatus(item.status).label,
        formatDateTimeLong(item.updatedAt),
      ]),
    )
  })

  area.replaceChildren(
    panel(
      "report-panel",
      toolbarWithSearch({
        search,
        extra: [
          h("div", { class: "toolbar-field" }, category),
          h("div", { class: "toolbar-field" }, status),
          h("div", { class: "toolbar-field" }, sort),
          exportButton,
        ],
      }),
      statsArea,
      summary,
      tableArea,
      paginationArea,
    ),
  )

  loadCategories().then((result) =>
    category.replaceChildren(...categoryOptions(result)),
  )
  load()
}

function movementsTab(area) {
  const stateTab = {
    search: "",
    type: "",
    from: "",
    to: "",
    sort: "createdAt,desc",
    page: 0,
    size: 20,
  }
  let requestId = 0
  let currentItems = []

  const search = h("input", {
    type: "search",
    "aria-label": "Buscar movimentações",
    placeholder: "Buscar por produto ou SKU…",
    autocomplete: "off",
  })
  const type = h(
    "select",
    { "aria-label": "Tipo" },
    h("option", { value: "" }, "Todos os tipos"),
    h("option", { value: "ENTRY" }, "Entradas"),
    h("option", { value: "EXIT" }, "Saídas"),
    canAdjust() ? h("option", { value: "ADJUSTMENT" }, "Ajustes") : null,
  )
  const from = h("input", { type: "date", "aria-label": "Data inicial" })
  const to = h("input", { type: "date", "aria-label": "Data final" })
  const sort = h(
    "select",
    { "aria-label": "Ordenar" },
    h("option", { value: "createdAt,desc" }, "Mais recentes"),
    h("option", { value: "createdAt,asc" }, "Mais antigas"),
  )
  const exportButton = h(
    "button",
    { class: "button button-secondary button-small", type: "button" },
    svgIcon("download"),
    "Exportar CSV",
  )
  const tableArea = h("div")
  const paginationArea = h("div")
  const summary = h("p", { class: "results-summary", "aria-live": "polite" })

  function render(page) {
    currentItems = page.content
    if (!page.content.length) {
      tableArea.replaceChildren(
        emptyState({
          title: "Nenhuma movimentação",
          message: "Nenhum registro corresponde aos filtros.",
        }),
      )
      paginationArea.replaceChildren()
      summary.textContent = "Nenhuma movimentação para exibir."
      return
    }
    tableArea.replaceChildren(
      reportTable({
        key: "report-movement",
        caption: "Relatório de movimentações por período",
        columns: [
          {
            label: "Data e hora",
            render: (item) => formatDateTime(item.createdAt),
          },
          {
            label: "Produto",
            render: (item) =>
              h(
                "div",
                { class: "cell-primary" },
                h("strong", { text: item.productName }),
                h("span", {
                  class: "cell-muted",
                  text: item.productSku || "Sem SKU",
                }),
              ),
          },
          { label: "Tipo", render: (item) => movementType(item.type).label },
          {
            label: "Quantidade",
            render: (item) => formatNumber(item.quantity),
          },
          {
            label: "Saldo anterior",
            render: (item) => formatNumber(item.previousQuantity),
          },
          {
            label: "Saldo posterior",
            render: (item) => formatNumber(item.resultingQuantity),
          },
          { label: "Motivo", key: "reason" },
          { label: "Responsável", key: "userName" },
        ],
        items: page.content,
      }),
    )
    summary.textContent = `${formatNumber(page.totalElements)} movimentações no período.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "movimentações",
        onChange: (next) => {
          stateTab.page = next
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Gerando relatório…"))
    try {
      const page = await api.get("/reports/movements", {
        query: {
          storeId: state.storeId,
          search: stateTab.search || undefined,
          type: stateTab.type || undefined,
          from: stateTab.from || undefined,
          to: stateTab.to || undefined,
          sort: stateTab.sort,
          page: stateTab.page,
          size: stateTab.size,
        },
      })
      if (current !== requestId) return
      render(page)
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  bindSearch(search, (value) => {
    stateTab.search = value
    stateTab.page = 0
    load()
  })
  type.addEventListener("change", () => {
    stateTab.type = type.value
    stateTab.page = 0
    load()
  })
  from.addEventListener("change", () => {
    stateTab.from = from.value
    stateTab.page = 0
    load()
  })
  to.addEventListener("change", () => {
    stateTab.to = to.value
    stateTab.page = 0
    load()
  })
  sort.addEventListener("change", () => {
    stateTab.sort = sort.value
    stateTab.page = 0
    load()
  })
  exportButton.addEventListener("click", () => {
    exportCsv(
      "relatorio-movimentacoes.csv",
      [
        "Data e hora",
        "Produto",
        "SKU",
        "Tipo",
        "Quantidade",
        "Saldo anterior",
        "Saldo posterior",
        "Motivo",
        "Responsável",
      ],
      currentItems.map((item) => [
        formatDateTimeLong(item.createdAt),
        item.productName,
        item.productSku ?? "",
        movementType(item.type).label,
        item.quantity,
        item.previousQuantity,
        item.resultingQuantity,
        item.reason,
        item.userName,
      ]),
    )
  })

  area.replaceChildren(
    panel(
      "report-panel",
      toolbarWithSearch({
        search,
        extra: [
          h("div", { class: "toolbar-field" }, type),
          h(
            "div",
            { class: "toolbar-field toolbar-date" },
            h("span", { class: "toolbar-label", text: "De" }),
            from,
          ),
          h(
            "div",
            { class: "toolbar-field toolbar-date" },
            h("span", { class: "toolbar-label", text: "Até" }),
            to,
          ),
          h("div", { class: "toolbar-field" }, sort),
          exportButton,
        ],
      }),
      summary,
      tableArea,
      paginationArea,
    ),
  )
  load()
}

function salesTab(area) {
  const stateTab = {
    search: "",
    from: "",
    to: "",
    sort: "createdAt,desc",
    page: 0,
    size: 20,
  }
  let requestId = 0
  let currentItems = []

  const search = h("input", {
    type: "search",
    "aria-label": "Buscar vendas",
    placeholder: "Buscar por cliente…",
    autocomplete: "off",
  })
  const from = h("input", { type: "date", "aria-label": "Data inicial" })
  const to = h("input", { type: "date", "aria-label": "Data final" })
  const sort = h(
    "select",
    { "aria-label": "Ordenar" },
    h("option", { value: "createdAt,desc" }, "Mais recentes"),
    h("option", { value: "total,desc" }, "Maior valor"),
  )
  const exportButton = h(
    "button",
    { class: "button button-secondary button-small", type: "button" },
    svgIcon("download"),
    "Exportar CSV",
  )
  const statsArea = h("div", { class: "inline-stats" })
  const tableArea = h("div")
  const paginationArea = h("div")
  const summary = h("p", { class: "results-summary", "aria-live": "polite" })

  function render(report) {
    statsArea.replaceChildren(
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(report.salesCount) }),
        " vendas",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatCurrency(report.revenue) }),
        " de receita",
      ),
    )
    const page = report.sales
    currentItems = page.content
    if (!page.content.length) {
      tableArea.replaceChildren(
        emptyState({
          icon: "sales",
          title: "Nenhuma venda",
          message: "Nenhuma venda corresponde aos filtros.",
        }),
      )
      paginationArea.replaceChildren()
      summary.textContent = "Nenhuma venda para exibir."
      return
    }
    tableArea.replaceChildren(
      reportTable({
        key: "report-sale",
        caption: "Relatório de vendas",
        columns: [
          {
            label: "Data e hora",
            render: (item) => formatDateTime(item.createdAt),
          },
          { label: "Cliente", key: "customerName" },
          { label: "Itens", render: (item) => formatNumber(item.itemCount) },
          { label: "Total", render: (item) => formatCurrency(item.total) },
          { label: "Vendedor", key: "userName" },
          {
            label: "Situação",
            render: (item) =>
              item.status === "CONCLUIDA" ? "Concluída" : item.status,
          },
        ],
        items: page.content,
      }),
    )
    summary.textContent = `${formatNumber(page.totalElements)} vendas no período.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "vendas",
        onChange: (next) => {
          stateTab.page = next
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Gerando relatório…"))
    try {
      const report = await api.get("/reports/sales", {
        query: {
          storeId: state.storeId,
          search: stateTab.search || undefined,
          from: stateTab.from || undefined,
          to: stateTab.to || undefined,
          sort: stateTab.sort,
          page: stateTab.page,
          size: stateTab.size,
        },
      })
      if (current !== requestId) return
      render(report)
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  bindSearch(search, (value) => {
    stateTab.search = value
    stateTab.page = 0
    load()
  })
  from.addEventListener("change", () => {
    stateTab.from = from.value
    stateTab.page = 0
    load()
  })
  to.addEventListener("change", () => {
    stateTab.to = to.value
    stateTab.page = 0
    load()
  })
  sort.addEventListener("change", () => {
    stateTab.sort = sort.value
    stateTab.page = 0
    load()
  })
  exportButton.addEventListener("click", () => {
    exportCsv(
      "relatorio-vendas.csv",
      ["Data e hora", "Cliente", "Itens", "Total", "Vendedor", "Situação"],
      currentItems.map((item) => [
        formatDateTimeLong(item.createdAt),
        item.customerName,
        item.itemCount,
        item.total,
        item.userName,
        item.status,
      ]),
    )
  })

  area.replaceChildren(
    panel(
      "report-panel",
      toolbarWithSearch({
        search,
        extra: [
          h(
            "div",
            { class: "toolbar-field toolbar-date" },
            h("span", { class: "toolbar-label", text: "De" }),
            from,
          ),
          h(
            "div",
            { class: "toolbar-field toolbar-date" },
            h("span", { class: "toolbar-label", text: "Até" }),
            to,
          ),
          h("div", { class: "toolbar-field" }, sort),
          exportButton,
        ],
      }),
      statsArea,
      summary,
      tableArea,
      paginationArea,
    ),
  )
  load()
}

function rankingTab(area, mode) {
  const isTop = mode === "top"
  const stateTab = { days: 30, limit: 10 }
  let requestId = 0
  let currentItems = []

  const days = h(
    "select",
    { "aria-label": "Período" },
    [7, 30, 90].map((value) =>
      h(
        "option",
        { value: String(value), selected: value === 30 },
        `Últimos ${value} dias`,
      ),
    ),
  )
  const limit = h(
    "select",
    { "aria-label": "Quantidade de produtos" },
    [5, 10, 20].map((value) =>
      h(
        "option",
        { value: String(value), selected: value === 10 },
        `${value} produtos`,
      ),
    ),
  )
  const tableArea = h("div")
  const summary = h("p", { class: "results-summary", "aria-live": "polite" })

  function render(items) {
    currentItems = items
    if (!items.length) {
      tableArea.replaceChildren(
        emptyState({
          title: "Sem dados no período",
          message:
            "Não há movimentações suficientes para calcular este relatório.",
        }),
      )
      summary.textContent = "Nenhum produto para exibir."
      return
    }
    const columns = isTop
      ? [
          { label: "Posição", render: (_item, index) => String(index + 1) },
          { label: "Produto", key: "name" },
          {
            label: "Quantidade movimentada",
            render: (item) => formatNumber(item.totalQuantity),
          },
          {
            label: "Movimentações",
            render: (item) => formatNumber(item.movementCount),
          },
        ]
      : [
          { label: "Produto", key: "name" },
          {
            label: "Disponível",
            render: (item) => `${formatNumber(item.quantity)} ${item.unit}`,
          },
          {
            label: "Mínimo",
            render: (item) => `${formatNumber(item.minimum)} ${item.unit}`,
          },
          { label: "Categoria", key: "category" },
          { label: "Situação", render: (item) => statusBadge(item.status) },
        ]

    tableArea.replaceChildren(
      reportTable({
        key: `ranking-${mode}`,
        caption: isTop
          ? "Produtos mais movimentados"
          : "Produtos com baixo giro",
        columns,
        items,
      }),
    )
    summary.textContent = `${formatNumber(items.length)} produtos no relatório.`
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Gerando relatório…"))
    try {
      const items = await api.get(
        isTop ? "/reports/top-products" : "/reports/low-turnover",
        {
          query: {
            storeId: state.storeId,
            days: stateTab.days,
            limit: stateTab.limit,
          },
        },
      )
      if (current !== requestId) return
      render(items ?? [])
    } catch (error) {
      if (current !== requestId) return
      tableArea.replaceChildren(errorState(error.message, { onRetry: load }))
    }
  }

  days.addEventListener("change", () => {
    stateTab.days = Number(days.value)
    load()
  })
  limit.addEventListener("change", () => {
    stateTab.limit = Number(limit.value)
    load()
  })

  area.replaceChildren(
    panel(
      "report-panel",
      h(
        "div",
        { class: "toolbar" },
        h("div", { class: "toolbar-field" }, days),
        h("div", { class: "toolbar-field" }, limit),
      ),
      h("p", {
        class: "section-description",
        text: isTop
          ? "Ranking por quantidade total movimentada (entradas, saídas e ajustes)."
          : "Produtos com menor movimentação no período, úteis para decisões de compra.",
      }),
      summary,
      tableArea,
    ),
  )
  load()
}

const TABS = [
  { id: "produtos", label: "Produtos", render: productsTab },
  { id: "estoque", label: "Estoque", render: stockTab },
  { id: "movimentacoes", label: "Movimentações", render: movementsTab },
  { id: "vendas", label: "Vendas", render: salesTab },
  {
    id: "mais-movimentados",
    label: "Mais movimentados",
    render: (area) => rankingTab(area, "top"),
  },
  {
    id: "baixo-giro",
    label: "Baixo giro",
    render: (area) => rankingTab(area, "low"),
  },
]

let activeTab = "produtos"

export async function render(container) {
  const panelArea = h("div")
  const tabList = h("div", {
    class: "tab-list",
    role: "tablist",
    "aria-label": "Relatórios disponíveis",
  })
  const buttons = new Map()

  function selectTab(tabId, { focus = false } = {}) {
    activeTab = tabId
    for (const [id, button] of buttons) {
      const selected = id === tabId
      button.setAttribute("aria-selected", String(selected))
      button.classList.toggle("active", selected)
      button.tabIndex = selected ? 0 : -1
    }
    const tab = TABS.find((item) => item.id === tabId) ?? TABS[0]
    panelArea.setAttribute("id", `report-panel-${tab.id}`)
    panelArea.setAttribute("role", "tabpanel")
    panelArea.setAttribute("aria-labelledby", `report-tab-${tab.id}`)
    for (const [id, button] of buttons)
      button.setAttribute("aria-controls", `report-panel-${id}`)
    tab.render(panelArea)
    if (focus) buttons.get(tabId)?.focus()
  }

  for (const tab of TABS) {
    const button = h(
      "button",
      {
        class: "tab-button",
        type: "button",
        role: "tab",
        id: `report-tab-${tab.id}`,
        "aria-selected": String(tab.id === activeTab),
        "aria-controls": `report-panel-${tab.id}`,
        tabIndex: tab.id === activeTab ? 0 : -1,
        onClick: () => selectTab(tab.id),
        onKeydown: (event) => {
          const index = TABS.findIndex((item) => item.id === tab.id)
          if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
            event.preventDefault()
            const offset = event.key === "ArrowRight" ? 1 : -1
            const next = TABS[(index + offset + TABS.length) % TABS.length]
            selectTab(next.id, { focus: true })
          } else if (event.key === "Home") {
            event.preventDefault()
            selectTab(TABS[0].id, { focus: true })
          } else if (event.key === "End") {
            event.preventDefault()
            selectTab(TABS[TABS.length - 1].id, { focus: true })
          }
        },
      },
      tab.label,
    )
    buttons.set(tab.id, button)
    tabList.append(button)
  }

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      h(
        "div",
        { class: "section-header" },
        h(
          "div",
          {},
          h("p", { class: "section-kicker", text: "Análise" }),
          h("h2", { text: "Relatórios" }),
          h("p", {
            class: "section-description",
            text: "Consulte produtos, estoque, movimentações e vendas com filtros por loja, categoria e período.",
          }),
        ),
      ),
      h("div", { class: "tab-scroll" }, tabList),
      panelArea,
    ),
  )
  selectTab(activeTab)
}
