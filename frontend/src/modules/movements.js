import { api } from "../services/api.js"
import { canAdjust, state, storeName } from "../context.js"
import { openModal } from "../components/modal.js"
import {
  notifyApiError,
  notifyError,
  notifySuccess,
} from "../components/notifications.js"
import { createPagination } from "../components/pagination.js"
import {
  emptyState,
  errorState,
  loadingState,
  panel,
} from "../components/states.js"
import {
  applyApiFieldErrors,
  clearFieldErrors,
  focusFirstError,
  h,
  setButtonLoading,
  setFieldError,
  svgIcon,
} from "../utils/dom.js"
import {
  formatDateTime,
  formatDateTimeLong,
  formatNumber,
  movementType,
} from "../utils/formatters.js"
import { validateMovement } from "../utils/validators.js"

const REASONS = [
  "Compra de fornecedor",
  "Venda",
  "Perda ou avaria",
  "Devolução",
  "Inventário",
  "Transferência",
  "Outro",
]

const TYPE_OPTIONS = [
  {
    value: "ENTRY",
    label: "Entrada",
    icon: "down",
    help: "Aumenta o saldo disponível.",
  },
  {
    value: "EXIT",
    label: "Saída",
    icon: "up",
    help: "Diminui o saldo disponível.",
  },
  {
    value: "ADJUSTMENT",
    label: "Ajuste",
    icon: "sliders",
    help: "Define o saldo final (apenas gestores).",
  },
]

function movementBadge(type) {
  const info = movementType(type)
  return h(
    "span",
    { class: `status movement-badge ${info.className}` },
    h("span", { class: "status-dot", "aria-hidden": "true" }),
    info.label,
  )
}

async function loadProductOptions() {
  const page = await api.get("/products", {
    query: { storeId: state.storeId, sort: "name,asc", size: 100 },
  })
  return page.content ?? []
}

export async function openMovementModal({ product = null, onSaved } = {}) {
  let products = []
  try {
    products = product
      ? [
          product,
          ...(await loadProductOptions()).filter(
            (item) => item.id !== product.id,
          ),
        ]
      : await loadProductOptions()
  } catch (error) {
    notifyApiError(error, "Não foi possível carregar os produtos.")
    return
  }

  if (products.length === 0) {
    notifyError(
      "Nenhum produto disponível",
      "Cadastre um produto antes de registrar movimentações.",
    )
    return
  }

  let type = "ENTRY"

  const productSelect = h(
    "select",
    { id: "movement-product", name: "productId", required: true },
    h("option", { value: "" }, "Selecione o produto"),
    products.map((item) =>
      h(
        "option",
        { value: item.id, selected: product?.id === item.id },
        `${item.name}${item.sku ? ` · ${item.sku}` : ""}`,
      ),
    ),
  )

  const balanceInfo = h("p", { class: "field-hint", "aria-live": "polite" })

  const typeButtons = TYPE_OPTIONS.filter(
    (option) => option.value !== "ADJUSTMENT" || canAdjust(),
  ).map((option) =>
    h(
      "button",
      {
        type: "button",
        class: `type-button ${option.value === type ? "active" : ""}`,
        "aria-pressed": String(option.value === type),
        "data-type": option.value,
        onClick: () => setType(option.value),
      },
      svgIcon(option.icon),
      h(
        "span",
        {},
        h("strong", { text: option.label }),
        h("small", { text: option.help }),
      ),
    ),
  )

  const quantityLabel = h("label", {
    for: "movement-quantity",
    text: "Quantidade *",
  })
  const quantityHint = h("p", { class: "field-hint" })

  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "movement-product", text: "Produto *" }),
      productSelect,
      balanceInfo,
    ),
    h(
      "fieldset",
      { class: "type-toggle form-field-wide" },
      h("legend", { text: "Tipo de movimentação *" }),
      h("div", { class: "type-toggle-grid" }, typeButtons),
    ),
    h(
      "div",
      { class: "form-field" },
      quantityLabel,
      h("input", {
        id: "movement-quantity",
        name: "quantity",
        type: "number",
        min: "0",
        step: "1",
        inputmode: "numeric",
        placeholder: "0",
      }),
      quantityHint,
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "movement-reason", text: "Motivo *" }),
      h("input", {
        id: "movement-reason",
        name: "reason",
        maxlength: 120,
        list: "movement-reasons",
        autocomplete: "off",
        placeholder: "Ex.: Compra de fornecedor",
      }),
      h(
        "datalist",
        { id: "movement-reasons" },
        REASONS.map((reason) => h("option", { value: reason })),
      ),
    ),
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "movement-observation", text: "Observações" }),
      h("textarea", {
        id: "movement-observation",
        name: "observation",
        rows: 3,
        maxlength: 500,
        placeholder: "Detalhes adicionais (opcional)",
      }),
    ),
  )

  function selectedProduct() {
    return products.find((item) => item.id === productSelect.value) ?? null
  }

  function refreshBalance() {
    const item = selectedProduct()
    if (!item) {
      balanceInfo.textContent = "Selecione um produto para ver o saldo atual."
      return
    }
    balanceInfo.textContent = `Saldo atual: ${formatNumber(item.quantity)} ${item.unit} · mínimo ${formatNumber(item.minimum)} ${item.unit}.`
  }

  function setType(nextType) {
    type = nextType
    for (const button of typeButtons) {
      const active = button.dataset.type === type
      button.classList.toggle("active", active)
      button.setAttribute("aria-pressed", String(active))
    }
    if (type === "ADJUSTMENT") {
      quantityLabel.textContent = "Novo saldo *"
      quantityHint.textContent =
        "O saldo final desejado; a diferença será registrada no histórico."
      form.elements.quantity.min = "0"
    } else {
      quantityLabel.textContent = "Quantidade *"
      quantityHint.textContent = "Informe um número inteiro maior que zero."
      form.elements.quantity.min = "1"
    }
    const item = selectedProduct()
    if (item && type === "EXIT") {
      quantityHint.textContent = `Disponível para saída: ${formatNumber(item.quantity)} ${item.unit}.`
    }
  }

  productSelect.addEventListener("change", () => {
    refreshBalance()
    setType(type)
  })
  refreshBalance()
  setType(type)

  const submit = h(
    "button",
    { class: "button button-primary", type: "submit" },
    "Registrar movimentação",
  )
  const modal = openModal({
    title: "Registrar movimentação",
    description:
      "Toda movimentação é registrada com usuário, loja, motivo e saldos anterior/posterior.",
    content: form,
    footer: [
      h(
        "button",
        {
          class: "button button-secondary",
          type: "button",
          onClick: (event) => event.target.closest("dialog").close(),
        },
        "Cancelar",
      ),
      submit,
    ],
    size: "lg",
  })

  form.addEventListener("submit", async (event) => {
    event.preventDefault()
    clearFieldErrors(form)
    const values = Object.fromEntries(new FormData(form).entries())
    const errors = validateMovement({ ...values, type })
    if (Object.keys(errors).length) {
      for (const [field, message] of Object.entries(errors))
        setFieldError(form, field, message)
      focusFirstError(form)
      return
    }

    setButtonLoading(submit, true, "Registrando…")
    try {
      const movement = await api.post(
        "/stock/movements",
        {
          productId: values.productId,
          type,
          quantity: Number(values.quantity),
          reason: values.reason,
          observation: values.observation || null,
        },
        { query: { storeId: state.storeId } },
      )
      modal.close()
      const info = movementType(type)
      notifySuccess(
        `${info.label} registrada`,
        `${movement.productName}: ${formatNumber(movement.previousQuantity)} → ${formatNumber(movement.resultingQuantity)} ${products.find((item) => item.id === movement.productId)?.unit ?? "un"}.`,
      )
      onSaved?.(movement)
    } catch (error) {
      if (!applyApiFieldErrors(form, error.fieldErrors)) {
        notifyApiError(error, "Não foi possível registrar a movimentação.")
      }
    } finally {
      setButtonLoading(submit, false)
    }
  })
}

function movementDetails(movement) {
  const info = movementType(movement.type)
  const content = h(
    "div",
    { class: "detail-stack" },
    h(
      "div",
      { class: "detail-status" },
      movementBadge(movement.type),
      h("span", { class: "badge", text: storeName() }),
    ),
    h(
      "dl",
      { class: "detail-list" },
      h(
        "div",
        {},
        h("dt", { text: "Produto" }),
        h("dd", { text: movement.productName }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "SKU" }),
        h("dd", { text: movement.productSku || "—" }),
      ),
      h("div", {}, h("dt", { text: "Tipo" }), h("dd", { text: info.label })),
      h(
        "div",
        {},
        h("dt", { text: "Quantidade" }),
        h("dd", { text: formatNumber(movement.quantity) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Saldo anterior" }),
        h("dd", { text: formatNumber(movement.previousQuantity) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Saldo posterior" }),
        h("dd", { text: formatNumber(movement.resultingQuantity) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Motivo" }),
        h("dd", { text: movement.reason }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Observações" }),
        h("dd", { text: movement.observation || "—" }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Responsável" }),
        h("dd", { text: movement.userName }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Data e hora" }),
        h("dd", { text: formatDateTimeLong(movement.createdAt) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Identificador" }),
        h("dd", { text: movement.id }),
      ),
    ),
  )
  openModal({ title: "Detalhes da movimentação", content, size: "md" })
}

export async function render(container) {
  const filters = {
    search: "",
    type: "",
    from: "",
    to: "",
    sort: "createdAt,desc",
    page: 0,
    size: 10,
  }
  let requestId = 0

  const tableArea = h("div", { class: "table-area" })
  const paginationArea = h("div")
  const resultsSummary = h("p", {
    class: "results-summary",
    "aria-live": "polite",
  })

  const search = h("input", {
    type: "search",
    placeholder: "Buscar por produto ou SKU…",
    "aria-label": "Buscar movimentações por produto",
    autocomplete: "off",
  })
  const typeSelect = h(
    "select",
    { "aria-label": "Filtrar por tipo" },
    h("option", { value: "" }, "Todos os tipos"),
    TYPE_OPTIONS.filter(
      (option) => option.value !== "ADJUSTMENT" || canAdjust(),
    ).map((option) => h("option", { value: option.value }, option.label)),
  )
  const fromInput = h("input", { type: "date", "aria-label": "Data inicial" })
  const toInput = h("input", { type: "date", "aria-label": "Data final" })
  const sortSelect = h(
    "select",
    { "aria-label": "Ordenar movimentações" },
    h("option", { value: "createdAt,desc" }, "Mais recentes primeiro"),
    h("option", { value: "createdAt,asc" }, "Mais antigas primeiro"),
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
    { class: "toolbar toolbar-wrap" },
    h("div", { class: "toolbar-field toolbar-search" }, search),
    h("div", { class: "toolbar-field" }, typeSelect),
    h(
      "div",
      { class: "toolbar-field toolbar-date" },
      h("span", { class: "toolbar-label", text: "De" }),
      fromInput,
    ),
    h(
      "div",
      { class: "toolbar-field toolbar-date" },
      h("span", { class: "toolbar-label", text: "Até" }),
      toInput,
    ),
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
      h("p", { class: "section-kicker", text: "Auditoria" }),
      h("h2", { text: "Histórico de movimentações" }),
      h("p", {
        class: "section-description",
        text: "Histórico integral, sem limite artificial de registros. Correções devem ser feitas por ajuste.",
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
        "Nova movimentação",
      ),
    ),
  )

  function resetFilters() {
    Object.assign(filters, {
      search: "",
      type: "",
      from: "",
      to: "",
      sort: "createdAt,desc",
      page: 0,
    })
    search.value = ""
    typeSelect.value = ""
    fromInput.value = ""
    toInput.value = ""
    sortSelect.value = "createdAt,desc"
    load()
  }

  function hasFilters() {
    return Boolean(filters.search || filters.type || filters.from || filters.to)
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
  typeSelect.addEventListener("change", () => {
    filters.type = typeSelect.value
    filters.page = 0
    load()
  })
  fromInput.addEventListener("change", () => {
    filters.from = fromInput.value
    filters.page = 0
    load()
  })
  toInput.addEventListener("change", () => {
    filters.to = toInput.value
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

  function row(movement) {
    const info = movementType(movement.type)
    return h(
      "tr",
      {},
      h(
        "td",
        { "data-label": "Data e hora" },
        h("span", { class: "cell-date" }, formatDateTime(movement.createdAt)),
      ),
      h(
        "td",
        { "data-label": "Produto" },
        h(
          "div",
          { class: "cell-primary" },
          h("strong", { text: movement.productName }),
          movement.productSku
            ? h("span", { class: "cell-muted", text: movement.productSku })
            : null,
        ),
      ),
      h("td", { "data-label": "Tipo" }, movementBadge(movement.type)),
      h(
        "td",
        { "data-label": "Quantidade" },
        h(
          "strong",
          { class: `quantity ${info.className}` },
          `${info.sign}${formatNumber(movement.quantity)}`,
        ),
      ),
      h(
        "td",
        { "data-label": "Saldo" },
        `${formatNumber(movement.previousQuantity)} → ${formatNumber(movement.resultingQuantity)}`,
      ),
      h("td", { "data-label": "Motivo", class: "cell-wrap" }, movement.reason),
      h("td", { "data-label": "Responsável" }, movement.userName),
      h(
        "td",
        { "data-label": "Ações", class: "cell-actions" },
        h(
          "button",
          {
            class: "icon-button action-button",
            type: "button",
            "aria-label": `Ver detalhes da movimentação de ${movement.productName}`,
            title: "Ver detalhes",
            onClick: () => movementDetails(movement),
          },
          svgIcon("eye"),
        ),
      ),
    )
  }

  function renderTable(page) {
    if (!page.content.length) {
      tableArea.replaceChildren(
        hasFilters()
          ? emptyState({
              title: "Nenhuma movimentação encontrada",
              message: "Ajuste os filtros de período, tipo ou produto.",
              actionLabel: "Limpar filtros",
              onAction: resetFilters,
            })
          : emptyState({
              title: "Histórico vazio",
              message:
                "As entradas, saídas e ajustes aparecerão aqui com auditoria completa.",
              actionLabel: "Registrar movimentação",
              onAction: () => openMovementModal({ onSaved: load }),
            }),
      )
      paginationArea.replaceChildren()
      resultsSummary.textContent = "Nenhuma movimentação para exibir."
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
            text: "Histórico de movimentações de estoque",
          }),
          h(
            "thead",
            {},
            h(
              "tr",
              {},
              h("th", { scope: "col" }, "Data e hora"),
              h("th", { scope: "col" }, "Produto"),
              h("th", { scope: "col" }, "Tipo"),
              h("th", { scope: "col" }, "Quantidade"),
              h("th", { scope: "col" }, "Saldo"),
              h("th", { scope: "col" }, "Motivo"),
              h("th", { scope: "col" }, "Responsável"),
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
      page.totalElements === 1
        ? "movimentação registrada"
        : "movimentações registradas"
    }.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "movimentações",
        onChange: (nextPage) => {
          filters.page = nextPage
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Carregando histórico…"))
    resultsSummary.textContent = ""
    clearButton.hidden = !hasFilters()

    try {
      const page = await api.get("/stock/movements", {
        query: {
          storeId: state.storeId,
          search: filters.search || undefined,
          type: filters.type || undefined,
          from: filters.from || undefined,
          to: filters.to || undefined,
          sort: filters.sort,
          page: filters.page,
          size: filters.size,
        },
      })
      if (current !== requestId) return
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

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      heading,
      panel(
        "movements-panel",
        toolbar,
        resultsSummary,
        tableArea,
        paginationArea,
      ),
    ),
  )
  await load()
}
