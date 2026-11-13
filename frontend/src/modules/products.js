import { api } from "../services/api.js"
import { canManage, state } from "../context.js"
import { confirmAction, openModal } from "../components/modal.js"
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
  clearFieldErrors,
  focusFirstError,
  h,
  setButtonLoading,
  setFieldError,
  svgIcon,
} from "../utils/dom.js"
import {
  formatDateTime,
  formatNumber,
  productStatus,
} from "../utils/formatters.js"
import { validateProduct } from "../utils/validators.js"

const UNIT_OPTIONS = ["un", "kg", "g", "L", "ml", "cx", "pct"]

const SORT_OPTIONS = [
  { value: "name,asc", label: "Nome (A–Z)" },
  { value: "name,desc", label: "Nome (Z–A)" },
  { value: "quantity,asc", label: "Menor quantidade" },
  { value: "quantity,desc", label: "Maior quantidade" },
  { value: "updatedAt,desc", label: "Atualizados recentemente" },
  { value: "createdAt,desc", label: "Cadastrados recentemente" },
]

function emptyFilters(filters) {
  return !filters.search && !filters.category && !filters.status
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

function productForm({ product, categories, onSaved }) {
  const creating = !product
  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "product-name", text: "Nome do produto *" }),
      h("input", {
        id: "product-name",
        name: "name",
        required: true,
        maxlength: 80,
        autocomplete: "off",
        value: product?.name ?? "",
        placeholder: "Ex.: Farinha de trigo 1 kg",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "product-sku", text: "SKU / código" }),
      h("input", {
        id: "product-sku",
        name: "sku",
        maxlength: 40,
        autocomplete: "off",
        value: product?.sku ?? "",
        placeholder: "Ex.: FAR-001",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "product-category", text: "Categoria" }),
      h("input", {
        id: "product-category",
        name: "category",
        maxlength: 60,
        list: "product-categories",
        autocomplete: "off",
        value: product?.category ?? "",
        placeholder: "Ex.: Mercearia",
      }),
      h(
        "datalist",
        { id: "product-categories" },
        categories.map((category) => h("option", { value: category })),
      ),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "product-unit", text: "Unidade" }),
      h(
        "select",
        { id: "product-unit", name: "unit" },
        UNIT_OPTIONS.map((unit) =>
          h(
            "option",
            { value: unit, selected: (product?.unit ?? "un") === unit },
            unit,
          ),
        ),
      ),
    ),
    creating
      ? h(
          "div",
          { class: "form-field" },
          h("label", { for: "product-quantity", text: "Quantidade inicial *" }),
          h("input", {
            id: "product-quantity",
            name: "quantity",
            type: "number",
            min: "0",
            step: "1",
            inputmode: "numeric",
            value: "0",
          }),
        )
      : h(
          "div",
          { class: "form-field form-field-readonly" },
          h("span", { class: "field-label", text: "Saldo atual" }),
          h("p", {
            class: "readonly-value",
            text: `${formatNumber(product.quantity)} ${product.unit}`,
          }),
          h("p", {
            class: "field-hint",
            text: "O saldo só muda por movimentações auditáveis.",
          }),
        ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "product-minimum", text: "Estoque mínimo *" }),
      h("input", {
        id: "product-minimum",
        name: "minimum",
        type: "number",
        min: "0",
        step: "1",
        inputmode: "numeric",
        value: product?.minimum ?? 0,
      }),
    ),
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "product-description", text: "Descrição" }),
      h(
        "textarea",
        {
          id: "product-description",
          name: "description",
          rows: 3,
          maxlength: 500,
          placeholder: "Observações sobre o produto (opcional)",
        },
        product?.description ?? "",
      ),
    ),
  )

  const submit = h(
    "button",
    { class: "button button-primary", type: "submit" },
    creating ? "Cadastrar produto" : "Salvar alterações",
  )

  const modal = openModal({
    title: creating ? "Novo produto" : "Editar produto",
    description: creating
      ? "O saldo inicial gera uma entrada auditável no histórico."
      : "Edite os dados cadastrais. O saldo não é alterado por aqui.",
    size: "lg",
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
  })

  form.addEventListener("submit", async (event) => {
    event.preventDefault()
    clearFieldErrors(form)

    const values = Object.fromEntries(new FormData(form).entries())
    const errors = validateProduct(values, { creating })
    if (Object.keys(errors).length) {
      for (const [field, message] of Object.entries(errors))
        setFieldError(form, field, message)
      focusFirstError(form)
      return
    }

    const payload = {
      name: values.name,
      sku: values.sku || null,
      category: values.category || null,
      description: values.description || null,
      minimum: Number(values.minimum),
      unit: values.unit || "un",
    }

    setButtonLoading(submit, true)
    try {
      let saved
      if (creating) {
        saved = await api.post(
          "/products",
          { ...payload, quantity: Number(values.quantity) },
          { query: { storeId: state.storeId } },
        )
      } else {
        saved = await api.put(
          `/products/${product.id}`,
          { ...payload, version: product.version },
          { query: { storeId: state.storeId } },
        )
      }
      modal.close()
      notifySuccess(
        creating ? "Produto cadastrado" : "Produto atualizado",
        `${saved.name} foi ${
          creating ? "cadastrado" : "atualizado"
        } com sucesso.`,
      )
      onSaved?.(saved)
    } catch (error) {
      if (!applyApiFieldErrors(form, error.fieldErrors)) {
        notifyApiError(error, "Não foi possível salvar o produto.")
      }
    } finally {
      setButtonLoading(submit, false)
    }
  })
}

function productDetails(product, { onEdit, onArchive }) {
  const rows = [
    ["Nome", product.name],
    ["SKU / código", product.sku || "—"],
    ["Categoria", product.category || "—"],
    ["Unidade", product.unit],
    ["Saldo disponível", `${formatNumber(product.quantity)} ${product.unit}`],
    ["Estoque mínimo", `${formatNumber(product.minimum)} ${product.unit}`],
    ["Descrição", product.description || "—"],
    ["Cadastrado em", formatDateTime(product.createdAt)],
    ["Atualizado em", formatDateTime(product.updatedAt)],
    ["Identificador", product.id],
  ]

  const content = h(
    "div",
    { class: "detail-stack" },
    h(
      "div",
      { class: "detail-status" },
      statusBadge(product.status),
      product.active
        ? h("span", { class: "badge", text: "Ativo" })
        : h("span", { class: "badge badge-muted", text: "Arquivado" }),
    ),
    h(
      "dl",
      { class: "detail-list" },
      rows.map(([label, value]) =>
        h("div", {}, h("dt", { text: label }), h("dd", { text: value })),
      ),
    ),
  )

  const footer = [
    h(
      "button",
      {
        class: "button button-secondary",
        type: "button",
        onClick: (event) => event.target.closest("dialog").close(),
      },
      "Fechar",
    ),
  ]
  if (canManage() && product.active) {
    footer.push(
      h(
        "button",
        {
          class: "button button-secondary",
          type: "button",
          onClick: (event) => {
            event.target.closest("dialog").close()
            onEdit()
          },
        },
        svgIcon("edit"),
        "Editar",
      ),
      h(
        "button",
        {
          class: "button button-danger",
          type: "button",
          onClick: (event) => {
            event.target.closest("dialog").close()
            onArchive()
          },
        },
        svgIcon("trash"),
        "Arquivar",
      ),
    )
  }

  openModal({ title: "Detalhes do produto", content, footer, size: "md" })
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

  const view = h("div", { class: "view-stack" })
  const toolbar = h("div", { class: "toolbar" })
  const tableArea = h("div", { class: "table-area" })
  const paginationArea = h("div")
  const resultsSummary = h("p", {
    class: "results-summary",
    "aria-live": "polite",
  })

  const heading = h(
    "div",
    { class: "section-header" },
    h(
      "div",
      {},
      h("p", { class: "section-kicker", text: "Cadastro" }),
      h("h2", { text: "Produtos" }),
      h("p", {
        class: "section-description",
        text: "Consulte, cadastre, edite e arquive os produtos da loja ativa.",
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
              onClick: () =>
                productForm({
                  categories: categoryNames(),
                  onSaved: () => {
                    refreshCategories()
                    load()
                  },
                }),
            },
            svgIcon("plus"),
            "Novo produto",
          ),
        )
      : null,
  )

  function categoryNames() {
    return categories
      .map((category) =>
        typeof category === "string" ? category : category.name,
      )
      .filter(Boolean)
  }

  const search = h("input", {
    type: "search",
    id: "product-search",
    placeholder: "Buscar por nome ou SKU…",
    "aria-label": "Buscar produtos por nome ou SKU",
    autocomplete: "off",
  })

  const categorySelect = h(
    "select",
    { id: "product-category-filter", "aria-label": "Filtrar por categoria" },
    h("option", { value: "" }, "Todas as categorias"),
  )
  const statusSelect = h(
    "select",
    { id: "product-status-filter", "aria-label": "Filtrar por situação" },
    h("option", { value: "" }, "Todas as situações"),
    h("option", { value: "NORMAL" }, "Normal"),
    h("option", { value: "LOW" }, "Estoque baixo"),
    h("option", { value: "OUT_OF_STOCK" }, "Esgotado"),
  )
  const sortSelect = h(
    "select",
    { id: "product-sort", "aria-label": "Ordenar produtos" },
    SORT_OPTIONS.map((option) =>
      h("option", { value: option.value }, option.label),
    ),
  )
  const sizeSelect = h(
    "select",
    { id: "product-size", "aria-label": "Itens por página" },
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
      onClick: () => resetFilters(),
    },
    "Limpar filtros",
  )

  function resetFilters() {
    filters.search = ""
    filters.category = ""
    filters.status = ""
    filters.sort = "name,asc"
    filters.page = 0
    search.value = ""
    categorySelect.value = ""
    statusSelect.value = ""
    sortSelect.value = "name,asc"
    load()
  }

  toolbar.append(
    h("div", { class: "toolbar-field toolbar-search" }, search),
    h("div", { class: "toolbar-field" }, categorySelect),
    h("div", { class: "toolbar-field" }, statusSelect),
    h("div", { class: "toolbar-field" }, sortSelect),
    h("div", { class: "toolbar-field" }, sizeSelect),
    clearButton,
  )

  view.append(
    heading,
    panel("products-panel", toolbar, resultsSummary, tableArea, paginationArea),
  )

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

  async function refreshCategories() {
    try {
      const result = await api.get("/products/categories", {
        query: { storeId: state.storeId },
      })
      categories = Array.isArray(result) ? result : []
      const current = filters.category
      categorySelect.replaceChildren(
        h("option", { value: "" }, "Todas as categorias"),
        ...categoryNames().map((category) =>
          h("option", { value: category }, category),
        ),
      )
      if (current) categorySelect.value = current
    } catch {
      // O filtro de categorias é complementar; a listagem continua funcional.
    }
  }

  function archiveProduct(product, afterArchive) {
    confirmAction({
      title: "Arquivar produto",
      message: `Arquivar “${product.name}”? O histórico de movimentações é preservado e o produto deixa de aparecer nas listas ativas.`,
      confirmLabel: "Arquivar produto",
      danger: true,
    }).then(async (confirmed) => {
      if (!confirmed) return
      try {
        await api.delete(`/products/${product.id}`, {
          query: { storeId: state.storeId },
        })
        notifySuccess(
          "Produto arquivado",
          `${product.name} foi arquivado. O histórico foi preservado.`,
        )
        afterArchive?.()
        load()
      } catch (error) {
        notifyApiError(error, "Não foi possível arquivar o produto.")
      }
    })
  }

  function row(product) {
    const status = productStatus(product.status)
    const actions = h("div", { class: "row-actions" })
    actions.append(
      h(
        "button",
        {
          class: "icon-button action-button",
          type: "button",
          "aria-label": `Ver detalhes de ${product.name}`,
          title: "Ver detalhes",
          onClick: () =>
            productDetails(product, {
              onEdit: () =>
                productForm({
                  product,
                  categories: categoryNames(),
                  onSaved: () => {
                    refreshCategories()
                    load()
                  },
                }),
              onArchive: () => archiveProduct(product),
            }),
        },
        svgIcon("eye"),
      ),
    )
    if (canManage() && product.active) {
      actions.append(
        h(
          "button",
          {
            class: "icon-button action-button",
            type: "button",
            "aria-label": `Editar ${product.name}`,
            title: "Editar",
            onClick: () =>
              productForm({
                product,
                categories: categoryNames(),
                onSaved: () => {
                  refreshCategories()
                  load()
                },
              }),
          },
          svgIcon("edit"),
        ),
        h(
          "button",
          {
            class: "icon-button action-button action-danger",
            type: "button",
            "aria-label": `Arquivar ${product.name}`,
            title: "Arquivar",
            onClick: () => archiveProduct(product),
          },
          svgIcon("trash"),
        ),
      )
    }

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
          product.sku
            ? h("span", { class: "cell-muted", text: product.sku })
            : null,
        ),
      ),
      h("td", { "data-label": "Categoria" }, product.category || "—"),
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
        { "data-label": "Atualizado em", class: "cell-date" },
        formatDateTime(product.updatedAt),
      ),
      h("td", { "data-label": "Ações", class: "cell-actions" }, actions),
    )
  }

  function renderTable(page) {
    if (!page.content.length) {
      tableArea.replaceChildren(
        emptyFilters(filters)
          ? emptyState({
              title: "Nenhum produto cadastrado",
              message:
                "Cadastre o primeiro item para começar a controlar o estoque desta loja.",
              actionLabel: canManage() ? "Cadastrar produto" : undefined,
              onAction: canManage()
                ? () =>
                    productForm({
                      categories: categoryNames(),
                      onSaved: () => {
                        refreshCategories()
                        load()
                      },
                    })
                : undefined,
            })
          : emptyState({
              title: "Nenhum produto encontrado",
              message:
                "Ajuste a busca ou os filtros para encontrar o que procura.",
              actionLabel: "Limpar filtros",
              onAction: resetFilters,
            }),
      )
      paginationArea.replaceChildren()
      resultsSummary.textContent = "Nenhum produto para exibir."
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
            text: "Produtos cadastrados na loja ativa",
          }),
          h(
            "thead",
            {},
            h(
              "tr",
              {},
              h("th", { scope: "col" }, "Produto"),
              h("th", { scope: "col" }, "Categoria"),
              h("th", { scope: "col" }, "Disponível"),
              h("th", { scope: "col" }, "Mínimo"),
              h("th", { scope: "col" }, "Situação"),
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
      page.totalElements === 1 ? "produto encontrado" : "produtos encontrados"
    }.`
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

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Carregando produtos…"))
    resultsSummary.textContent = ""
    clearButton.hidden = emptyFilters(filters)

    try {
      const page = await api.get("/products", {
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

  container.replaceChildren(view)
  await refreshCategories()
  await load()
}
