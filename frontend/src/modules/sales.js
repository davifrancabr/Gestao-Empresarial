import { api } from "../services/api.js"
import { canManage, state } from "../context.js"
import { openModal } from "../components/modal.js"
import { notifyApiError, notifySuccess } from "../components/notifications.js"
import { createPagination } from "../components/pagination.js"
import {
  emptyState,
  errorState,
  loadingState,
  panel,
} from "../components/states.js"
import {
  applyApiFieldErrors,
  h,
  setButtonLoading,
  svgIcon,
} from "../utils/dom.js"
import {
  formatCurrency,
  formatDateTime,
  formatDateTimeLong,
  formatNumber,
} from "../utils/formatters.js"

async function loadSaleOptions() {
  const [products, customers] = await Promise.all([
    api.get("/products", {
      query: { storeId: state.storeId, sort: "name,asc", size: 100 },
    }),
    api.get("/customers", {
      query: { storeId: state.storeId, sort: "name,asc", size: 100 },
    }),
  ])
  return {
    products: products.content ?? [],
    customers: customers.content ?? [],
  }
}

function saleCreationModal({ onSaved }) {
  const cart = []
  const contentArea = h(
    "div",
    { class: "sale-builder" },
    loadingState("Carregando produtos e clientes…"),
  )
  const submit = h(
    "button",
    {
      class: "button button-primary",
      type: "submit",
      form: "sale-form",
      disabled: true,
    },
    svgIcon("check"),
    "Concluir venda",
  )

  const modal = openModal({
    title: "Registrar venda",
    description:
      "A conclusão da venda gera automaticamente as saídas de estoque em uma única transação.",
    content: contentArea,
    footer: [
      h(
        "button",
        {
          class: "button button-secondary",
          type: "button",
          onClick: () => modal.close(),
        },
        "Cancelar",
      ),
      submit,
    ],
    size: "xl",
  })

  loadSaleOptions()
    .then(({ products, customers }) => {
      if (!products.length) {
        contentArea.replaceChildren(
          emptyState({
            icon: "package",
            title: "Nenhum produto disponível",
            message: "Cadastre produtos antes de registrar uma venda.",
            actionLabel: "Fechar",
            onAction: () => modal.close(),
          }),
        )
        return
      }

      const customerSelect = h(
        "select",
        { id: "sale-customer", name: "customerId" },
        h("option", { value: "" }, "Consumidor final (sem cadastro)"),
        customers.map((customer) =>
          h("option", { value: customer.id }, customer.name),
        ),
      )

      const productSelect = h(
        "select",
        { id: "sale-product", "aria-label": "Produto" },
        h("option", { value: "" }, "Selecione o produto"),
        products.map((product) =>
          h(
            "option",
            { value: product.id, disabled: product.quantity === 0 },
            `${product.name} · ${formatNumber(product.quantity)} ${product.unit} em estoque`,
          ),
        ),
      )
      const quantityInput = h("input", {
        id: "sale-quantity",
        type: "number",
        min: "1",
        step: "1",
        inputmode: "numeric",
        "aria-label": "Quantidade",
        placeholder: "Qtd.",
      })
      const priceInput = h("input", {
        id: "sale-price",
        type: "number",
        min: "0",
        step: "0.01",
        inputmode: "decimal",
        "aria-label": "Preço unitário (opcional)",
        placeholder: "Preço un. (opcional)",
      })
      const itemError = h("p", {
        class: "field-error",
        role: "alert",
        hidden: true,
      })
      const addButton = h(
        "button",
        { class: "button button-secondary", type: "button" },
        svgIcon("plus"),
        "Adicionar item",
      )

      const cartArea = h("div", { class: "cart-area" })
      const footer = h("div", { class: "sale-footer" })
      const form = h(
        "form",
        { id: "sale-form", class: "sale-form", novalidate: true },
        h(
          "div",
          { class: "form-field form-field-wide" },
          h("label", { for: "sale-customer", text: "Cliente" }),
          customerSelect,
        ),
        h(
          "div",
          { class: "sale-item-builder" },
          h(
            "div",
            { class: "form-field" },
            h("label", { for: "sale-product", text: "Produto" }),
            productSelect,
          ),
          h(
            "div",
            { class: "form-field field-qty" },
            h("label", { for: "sale-quantity", text: "Qtd." }),
            quantityInput,
          ),
          h(
            "div",
            { class: "form-field field-price" },
            h("label", { for: "sale-price", text: "Preço unitário" }),
            priceInput,
          ),
          h(
            "div",
            { class: "form-field field-add" },
            h("span", { class: "field-label", "aria-hidden": "true" }, " "),
            addButton,
          ),
        ),
        itemError,
        cartArea,
        footer,
      )

      function availableFor(productId) {
        const product = products.find((item) => item.id === productId)
        const inCart = cart
          .filter((item) => item.productId === productId)
          .reduce((sum, item) => sum + item.quantity, 0)
        return Math.max((product?.quantity ?? 0) - inCart, 0)
      }

      function showItemError(message) {
        itemError.textContent = message
        itemError.hidden = false
      }

      function clearItemError() {
        itemError.textContent = ""
        itemError.hidden = true
      }

      function renderCart() {
        if (!cart.length) {
          cartArea.replaceChildren(
            h("p", {
              class: "muted-note",
              text: "Nenhum item adicionado. Selecione um produto e informe a quantidade.",
            }),
          )
          footer.replaceChildren()
          submit.disabled = true
          return
        }

        let total = 0
        const rows = cart.map((item, index) => {
          const subtotal =
            item.unitPrice === null ? null : item.unitPrice * item.quantity
          if (subtotal !== null) total += subtotal
          return h(
            "tr",
            {},
            h(
              "td",
              { "data-label": "Produto" },
              h("strong", { text: item.name }),
            ),
            h(
              "td",
              { "data-label": "Quantidade" },
              formatNumber(item.quantity),
            ),
            h(
              "td",
              { "data-label": "Preço unitário" },
              item.unitPrice === null ? "—" : formatCurrency(item.unitPrice),
            ),
            h(
              "td",
              { "data-label": "Subtotal" },
              subtotal === null ? "—" : formatCurrency(subtotal),
            ),
            h(
              "td",
              { "data-label": "Ações", class: "cell-actions" },
              h(
                "button",
                {
                  class: "icon-button action-button action-danger",
                  type: "button",
                  "aria-label": `Remover ${item.name} da venda`,
                  title: "Remover item",
                  onClick: () => {
                    cart.splice(index, 1)
                    renderCart()
                  },
                },
                svgIcon("trash"),
              ),
            ),
          )
        })

        cartArea.replaceChildren(
          h(
            "div",
            { class: "table-wrapper" },
            h(
              "table",
              { class: "data-table cart-table" },
              h("caption", {
                class: "sr-only",
                text: "Itens adicionados à venda",
              }),
              h(
                "thead",
                {},
                h(
                  "tr",
                  {},
                  h("th", { scope: "col" }, "Produto"),
                  h("th", { scope: "col" }, "Qtd."),
                  h("th", { scope: "col" }, "Preço un."),
                  h("th", { scope: "col" }, "Subtotal"),
                  h(
                    "th",
                    { scope: "col", class: "cell-actions" },
                    h("span", { class: "sr-only", text: "Ações" }),
                  ),
                ),
              ),
              h("tbody", {}, rows),
            ),
          ),
        )

        footer.replaceChildren(
          h(
            "p",
            { class: "sale-total" },
            h("span", { text: "Total da venda" }),
            h("strong", { text: formatCurrency(total) }),
          ),
          h("p", {
            class: "field-hint",
            text: "Informe o preço unitário para calcular o total; ele é opcional nesta versão.",
          }),
        )
        submit.disabled = false
      }

      addButton.addEventListener("click", () => {
        clearItemError()
        const productId = productSelect.value
        const product = products.find((item) => item.id === productId)
        const quantity = Number(quantityInput.value)
        const price = priceInput.value === "" ? null : Number(priceInput.value)

        if (!product)
          return showItemError("Selecione um produto para adicionar.")
        if (!Number.isInteger(quantity) || quantity <= 0)
          return showItemError(
            "A quantidade deve ser um número inteiro maior que zero.",
          )
        if (quantity > availableFor(productId))
          return showItemError(
            `Estoque insuficiente para ${product.name}. Disponível: ${formatNumber(availableFor(productId))} ${product.unit}.`,
          )
        if (price !== null && (!Number.isFinite(price) || price < 0))
          return showItemError(
            "Informe um preço unitário válido ou deixe o campo vazio.",
          )
        if (cart.some((item) => item.productId === productId))
          return showItemError(
            "Este produto já foi adicionado. Remova o item para alterar a quantidade.",
          )

        cart.push({
          productId,
          name: product.name,
          unit: product.unit,
          quantity,
          unitPrice: price,
        })
        productSelect.value = ""
        quantityInput.value = ""
        priceInput.value = ""
        renderCart()
        productSelect.focus()
      })

      form.addEventListener("submit", async (event) => {
        event.preventDefault()
        clearItemError()
        if (!cart.length)
          return showItemError("Adicione ao menos um produto à venda.")

        setButtonLoading(submit, true, "Concluindo…")
        try {
          const sale = await api.post(
            "/sales",
            {
              customerId: customerSelect.value || null,
              items: cart.map((item) => ({
                productId: item.productId,
                quantity: item.quantity,
                unitPrice: item.unitPrice,
              })),
            },
            { query: { storeId: state.storeId } },
          )
          modal.close()
          notifySuccess(
            "Venda registrada",
            `Venda de ${formatCurrency(sale.total)} concluída e estoque atualizado.`,
          )
          onSaved?.(sale)
        } catch (error) {
          if (!applyApiFieldErrors(form, error.fieldErrors)) {
            notifyApiError(error, "Não foi possível registrar a venda.")
          }
        } finally {
          setButtonLoading(submit, false)
        }
      })

      contentArea.replaceChildren(form)
      renderCart()
      window.setTimeout(() => customerSelect.focus(), 50)
    })
    .catch((error) => {
      contentArea.replaceChildren(
        errorState(error.message, {
          onRetry: () => {
            modal.close()
            saleCreationModal({ onSaved })
          },
        }),
      )
    })
}

async function saleDetails(saleId) {
  const contentArea = h("div", {}, loadingState("Carregando venda…"))
  const modal = openModal({
    title: "Detalhes da venda",
    content: contentArea,
    footer: h(
      "button",
      {
        class: "button button-secondary",
        type: "button",
        onClick: () => modal.close(),
      },
      "Fechar",
    ),
    size: "lg",
  })

  try {
    const sale = await api.get(`/sales/${saleId}`, {
      query: { storeId: state.storeId },
    })
    contentArea.replaceChildren(
      h(
        "div",
        { class: "detail-stack" },
        h(
          "div",
          { class: "detail-status" },
          h("span", { class: "status status-normal", text: "Concluída" }),
          h("span", { class: "badge", text: formatCurrency(sale.total) }),
        ),
        h(
          "dl",
          { class: "detail-list" },
          h(
            "div",
            {},
            h("dt", { text: "Cliente" }),
            h("dd", { text: sale.customerName }),
          ),
          h(
            "div",
            {},
            h("dt", { text: "Vendedor" }),
            h("dd", { text: sale.userName }),
          ),
          h(
            "div",
            {},
            h("dt", { text: "Data e hora" }),
            h("dd", { text: formatDateTimeLong(sale.createdAt) }),
          ),
          h(
            "div",
            {},
            h("dt", { text: "Identificador" }),
            h("dd", { text: sale.id }),
          ),
        ),
        h(
          "div",
          { class: "table-wrapper" },
          h(
            "table",
            { class: "data-table" },
            h("caption", { class: "sr-only", text: "Itens da venda" }),
            h(
              "thead",
              {},
              h(
                "tr",
                {},
                h("th", { scope: "col" }, "Produto"),
                h("th", { scope: "col" }, "Qtd."),
                h("th", { scope: "col" }, "Preço un."),
                h("th", { scope: "col" }, "Subtotal"),
              ),
            ),
            h(
              "tbody",
              {},
              sale.items.map((item) =>
                h(
                  "tr",
                  {},
                  h("td", { "data-label": "Produto" }, item.productName),
                  h(
                    "td",
                    { "data-label": "Qtd." },
                    formatNumber(item.quantity),
                  ),
                  h(
                    "td",
                    { "data-label": "Preço un." },
                    item.unitPrice === null
                      ? "—"
                      : formatCurrency(item.unitPrice),
                  ),
                  h(
                    "td",
                    { "data-label": "Subtotal" },
                    item.subtotal === null
                      ? "—"
                      : formatCurrency(item.subtotal),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  } catch (error) {
    contentArea.replaceChildren(
      errorState(error.message, {
        onRetry: () => {
          modal.close()
          saleDetails(saleId)
        },
      }),
    )
  }
}

export async function render(container) {
  const filters = {
    search: "",
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
  const statsArea = h("div", { class: "inline-stats" })

  const search = h("input", {
    type: "search",
    placeholder: "Buscar por cliente…",
    "aria-label": "Buscar vendas por cliente",
    autocomplete: "off",
  })
  const fromInput = h("input", { type: "date", "aria-label": "Data inicial" })
  const toInput = h("input", { type: "date", "aria-label": "Data final" })
  const sortSelect = h(
    "select",
    { "aria-label": "Ordenar vendas" },
    h("option", { value: "createdAt,desc" }, "Mais recentes primeiro"),
    h("option", { value: "createdAt,asc" }, "Mais antigas primeiro"),
    h("option", { value: "total,desc" }, "Maior valor"),
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
      h("p", { class: "section-kicker", text: "Comercial" }),
      h("h2", { text: "Vendas" }),
      h("p", {
        class: "section-description",
        text: "Registre vendas e acompanhe o faturamento com baixa automática no estoque.",
      }),
    ),
    canManage()
      ? h(
          "div",
          { class: "section-actions" },
          h(
            "button",
            {
              class: "button button-primary",
              type: "button",
              onClick: () => saleCreationModal({ onSaved: load }),
            },
            svgIcon("plus"),
            "Nova venda",
          ),
        )
      : null,
  )

  function resetFilters() {
    Object.assign(filters, {
      search: "",
      from: "",
      to: "",
      sort: "createdAt,desc",
      page: 0,
    })
    search.value = ""
    fromInput.value = ""
    toInput.value = ""
    sortSelect.value = "createdAt,desc"
    load()
  }

  function hasFilters() {
    return Boolean(filters.search || filters.from || filters.to)
  }

  function renderStats(page) {
    const total = page.content.reduce(
      (sum, sale) => sum + Number(sale.total ?? 0),
      0,
    )
    statsArea.replaceChildren(
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatNumber(page.totalElements) }),
        " vendas no filtro",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: formatCurrency(total) }),
        " somadas nesta página",
      ),
      h(
        "span",
        { class: "inline-stat" },
        h("strong", { text: "Baixa automática" }),
        " no estoque ao concluir",
      ),
    )
  }

  function renderTable(page) {
    if (!page.content.length) {
      tableArea.replaceChildren(
        hasFilters()
          ? emptyState({
              title: "Nenhuma venda encontrada",
              message: "Ajuste o período ou a busca por cliente.",
              actionLabel: "Limpar filtros",
              onAction: resetFilters,
            })
          : emptyState({
              icon: "sales",
              title: "Nenhuma venda registrada",
              message:
                "As vendas concluídas aparecerão aqui com seus itens e totais.",
              actionLabel: canManage() ? "Registrar venda" : undefined,
              onAction: canManage()
                ? () => saleCreationModal({ onSaved: load })
                : undefined,
            }),
      )
      paginationArea.replaceChildren()
      resultsSummary.textContent = "Nenhuma venda para exibir."
      renderStats({ content: [], totalElements: 0 })
      return
    }

    tableArea.replaceChildren(
      h(
        "div",
        { class: "table-wrapper" },
        h(
          "table",
          { class: "data-table" },
          h("caption", { class: "sr-only", text: "Vendas registradas" }),
          h(
            "thead",
            {},
            h(
              "tr",
              {},
              h("th", { scope: "col" }, "Data e hora"),
              h("th", { scope: "col" }, "Cliente"),
              h("th", { scope: "col" }, "Itens"),
              h("th", { scope: "col" }, "Total"),
              h("th", { scope: "col" }, "Vendedor"),
              h("th", { scope: "col" }, "Situação"),
              h(
                "th",
                { scope: "col", class: "cell-actions" },
                h("span", { class: "sr-only", text: "Ações" }),
              ),
            ),
          ),
          h(
            "tbody",
            {},
            page.content.map((sale) =>
              h(
                "tr",
                {},
                h(
                  "td",
                  { "data-label": "Data e hora" },
                  formatDateTime(sale.createdAt),
                ),
                h("td", { "data-label": "Cliente" }, sale.customerName),
                h(
                  "td",
                  { "data-label": "Itens" },
                  formatNumber(sale.itemCount),
                ),
                h(
                  "td",
                  { "data-label": "Total" },
                  h("strong", { text: formatCurrency(sale.total) }),
                ),
                h("td", { "data-label": "Vendedor" }, sale.userName),
                h(
                  "td",
                  { "data-label": "Situação" },
                  h("span", {
                    class: "status status-normal",
                    text:
                      sale.status === "CONCLUIDA" ? "Concluída" : sale.status,
                  }),
                ),
                h(
                  "td",
                  { "data-label": "Ações", class: "cell-actions" },
                  h(
                    "button",
                    {
                      class: "icon-button action-button",
                      type: "button",
                      "aria-label": `Ver detalhes da venda de ${sale.customerName}`,
                      title: "Ver detalhes",
                      onClick: () => saleDetails(sale.id),
                    },
                    svgIcon("eye"),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    resultsSummary.textContent = `${formatNumber(page.totalElements)} ${
      page.totalElements === 1 ? "venda registrada" : "vendas registradas"
    }.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "vendas",
        onChange: (nextPage) => {
          filters.page = nextPage
          load()
        },
      }),
    )
    renderStats(page)
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Carregando vendas…"))
    resultsSummary.textContent = ""
    clearButton.hidden = !hasFilters()

    try {
      const page = await api.get("/sales", {
        query: {
          storeId: state.storeId,
          search: filters.search || undefined,
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

  let searchTimer
  search.addEventListener("input", () => {
    window.clearTimeout(searchTimer)
    searchTimer = window.setTimeout(() => {
      filters.search = search.value.trim()
      filters.page = 0
      load()
    }, 350)
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

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      heading,
      statsArea,
      panel("sales-panel", toolbar, resultsSummary, tableArea, paginationArea),
    ),
  )
  await load()
}
