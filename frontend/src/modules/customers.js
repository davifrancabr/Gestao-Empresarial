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
  formatCpf,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatNumber,
  formatPhone,
} from "../utils/formatters.js"
import { validateCustomer } from "../utils/validators.js"

const SORT_OPTIONS = [
  { value: "name,asc", label: "Nome (A–Z)" },
  { value: "name,desc", label: "Nome (Z–A)" },
  { value: "createdAt,desc", label: "Cadastrados recentemente" },
]

function customerForm({ customer, onSaved }) {
  const creating = !customer
  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "customer-name", text: "Nome *" }),
      h("input", {
        id: "customer-name",
        name: "name",
        required: true,
        maxlength: 120,
        autocomplete: "name",
        value: customer?.name ?? "",
        placeholder: "Nome completo",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "customer-cpf", text: "CPF" }),
      h("input", {
        id: "customer-cpf",
        name: "cpf",
        maxlength: 14,
        inputmode: "numeric",
        autocomplete: "off",
        value: customer?.cpf ? formatCpf(customer.cpf) : "",
        placeholder: "000.000.000-00",
      }),
      h("p", {
        class: "field-hint",
        text: "Opcional. Usado apenas para identificação.",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "customer-phone", text: "Telefone" }),
      h("input", {
        id: "customer-phone",
        name: "phone",
        maxlength: 20,
        inputmode: "tel",
        autocomplete: "tel",
        value: customer?.phone ?? "",
        placeholder: "(00) 00000-0000",
      }),
    ),
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "customer-email", text: "E-mail" }),
      h("input", {
        id: "customer-email",
        name: "email",
        type: "email",
        maxlength: 160,
        autocomplete: "email",
        value: customer?.email ?? "",
        placeholder: "cliente@exemplo.com",
      }),
    ),
    creating
      ? null
      : h(
          "div",
          { class: "form-field form-field-wide" },
          h(
            "label",
            { class: "checkbox-label" },
            h("input", {
              type: "checkbox",
              name: "active",
              checked: customer.active,
            }),
            h("span", { text: "Cliente ativo" }),
          ),
        ),
  )

  const submit = h(
    "button",
    { class: "button button-primary", type: "submit" },
    creating ? "Cadastrar cliente" : "Salvar alterações",
  )
  const modal = openModal({
    title: creating ? "Novo cliente" : "Editar cliente",
    description:
      "Cadastre apenas os dados necessários para o relacionamento comercial.",
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
    size: "md",
  })

  form.addEventListener("submit", async (event) => {
    event.preventDefault()
    clearFieldErrors(form)
    const formData = new FormData(form)
    const values = Object.fromEntries(formData.entries())
    values.active = creating ? true : formData.get("active") === "on"

    const errors = validateCustomer(values)
    if (Object.keys(errors).length) {
      for (const [field, message] of Object.entries(errors))
        setFieldError(form, field, message)
      focusFirstError(form)
      return
    }

    setButtonLoading(submit, true)
    try {
      const payload = {
        name: values.name,
        cpf: values.cpf ? values.cpf.replace(/\D/g, "") : null,
        phone: values.phone || null,
        email: values.email || null,
        active: values.active,
      }
      const saved = creating
        ? await api.post("/customers", payload, {
            query: { storeId: state.storeId },
          })
        : await api.put(`/customers/${customer.id}`, payload, {
            query: { storeId: state.storeId },
          })
      modal.close()
      notifySuccess(
        creating ? "Cliente cadastrado" : "Cliente atualizado",
        `${saved.name} foi ${
          creating ? "cadastrado" : "atualizado"
        } com sucesso.`,
      )
      onSaved?.(saved)
    } catch (error) {
      if (!applyApiFieldErrors(form, error.fieldErrors)) {
        notifyApiError(error, "Não foi possível salvar o cliente.")
      }
    } finally {
      setButtonLoading(submit, false)
    }
  })
}

function customerDetails(customer, { onEdit, onArchive, onPurchases }) {
  const content = h(
    "div",
    { class: "detail-stack" },
    h(
      "div",
      { class: "detail-status" },
      customer.active
        ? h("span", { class: "status status-normal", text: "Ativo" })
        : h("span", { class: "badge badge-muted", text: "Arquivado" }),
    ),
    h(
      "dl",
      { class: "detail-list" },
      h("div", {}, h("dt", { text: "Nome" }), h("dd", { text: customer.name })),
      h(
        "div",
        {},
        h("dt", { text: "CPF" }),
        h("dd", { text: customer.cpf ? formatCpf(customer.cpf) : "—" }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Telefone" }),
        h("dd", { text: customer.phone ? formatPhone(customer.phone) : "—" }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "E-mail" }),
        h("dd", { text: customer.email || "—" }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Cadastrado em" }),
        h("dd", { text: formatDateTime(customer.createdAt) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Atualizado em" }),
        h("dd", { text: formatDateTime(customer.updatedAt) }),
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
  footer.push(
    h(
      "button",
      {
        class: "button button-secondary",
        type: "button",
        onClick: (event) => {
          event.target.closest("dialog").close()
          onPurchases()
        },
      },
      svgIcon("history"),
      "Compras",
    ),
  )
  if (canManage() && customer.active) {
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

  openModal({ title: "Detalhes do cliente", content, footer, size: "md" })
}

function purchasesModal(customer) {
  const contentArea = h(
    "div",
    { class: "modal-table-area" },
    loadingState("Carregando compras…"),
  )
  const paginationArea = h("div")
  let pageIndex = 0

  const modal = openModal({
    title: `Compras de ${customer.name}`,
    description: "Histórico de vendas vinculadas a este cliente.",
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

  async function load() {
    contentArea.replaceChildren(loadingState("Carregando compras…"))
    try {
      const page = await api.get(`/customers/${customer.id}/purchases`, {
        query: { storeId: state.storeId, page: pageIndex, size: 10 },
      })
      if (!page.content.length) {
        contentArea.replaceChildren(
          emptyState({
            icon: "sales",
            title: "Nenhuma compra registrada",
            message: "As vendas vinculadas a este cliente aparecerão aqui.",
          }),
        )
        paginationArea.replaceChildren()
        return
      }

      contentArea.replaceChildren(
        h(
          "div",
          { class: "table-wrapper" },
          h(
            "table",
            { class: "data-table" },
            h("caption", {
              class: "sr-only",
              text: `Compras de ${customer.name}`,
            }),
            h(
              "thead",
              {},
              h(
                "tr",
                {},
                h("th", { scope: "col" }, "Data"),
                h("th", { scope: "col" }, "Itens"),
                h("th", { scope: "col" }, "Total"),
                h("th", { scope: "col" }, "Situação"),
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
                    { "data-label": "Data" },
                    formatDateTime(sale.createdAt),
                  ),
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
                  h(
                    "td",
                    { "data-label": "Situação" },
                    h("span", {
                      class: "status status-normal",
                      text:
                        sale.status === "CONCLUIDA" ? "Concluída" : sale.status,
                    }),
                  ),
                ),
              ),
            ),
          ),
        ),
      )
      paginationArea.replaceChildren(
        createPagination({
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          itemLabel: "compras",
          onChange: (nextPage) => {
            pageIndex = nextPage
            load()
          },
        }),
      )
    } catch (error) {
      contentArea.replaceChildren(errorState(error.message, { onRetry: load }))
      paginationArea.replaceChildren()
    }
  }

  contentArea.after(paginationArea)
  load()
}

export async function render(container) {
  const filters = {
    search: "",
    includeInactive: false,
    sort: "name,asc",
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
    placeholder: "Buscar por nome, e-mail ou CPF…",
    "aria-label": "Buscar clientes",
    autocomplete: "off",
  })
  const sortSelect = h(
    "select",
    { "aria-label": "Ordenar clientes" },
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
  const inactiveToggle = h(
    "label",
    { class: "checkbox-label toolbar-checkbox" },
    h("input", {
      type: "checkbox",
      "aria-label": "Incluir clientes arquivados",
    }),
    h("span", { text: "Incluir arquivados" }),
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
    h("div", { class: "toolbar-field" }, sortSelect),
    h("div", { class: "toolbar-field" }, sizeSelect),
    inactiveToggle,
    clearButton,
  )

  const heading = h(
    "div",
    { class: "section-header" },
    h(
      "div",
      {},
      h("p", { class: "section-kicker", text: "Relacionamento" }),
      h("h2", { text: "Clientes" }),
      h("p", {
        class: "section-description",
        text: "Consulte, cadastre e acompanhe o histórico de compras dos clientes.",
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
              onClick: () => customerForm({ onSaved: load }),
            },
            svgIcon("plus"),
            "Novo cliente",
          ),
        )
      : null,
  )

  function resetFilters() {
    Object.assign(filters, {
      search: "",
      includeInactive: false,
      sort: "name,asc",
      page: 0,
    })
    search.value = ""
    inactiveToggle.querySelector("input").checked = false
    sortSelect.value = "name,asc"
    load()
  }

  function archiveCustomer(customer) {
    confirmAction({
      title: "Arquivar cliente",
      message: `Arquivar “${customer.name}”? O histórico de compras é mantido, mas o cliente deixa de aparecer nas listas ativas.`,
      confirmLabel: "Arquivar cliente",
      danger: true,
    }).then(async (confirmed) => {
      if (!confirmed) return
      try {
        await api.delete(`/customers/${customer.id}`, {
          query: { storeId: state.storeId },
        })
        notifySuccess("Cliente arquivado", `${customer.name} foi arquivado.`)
        load()
      } catch (error) {
        notifyApiError(error, "Não foi possível arquivar o cliente.")
      }
    })
  }

  function row(customer) {
    const actions = h("div", { class: "row-actions" })
    const openDetails = () =>
      customerDetails(customer, {
        onEdit: () => customerForm({ customer, onSaved: load }),
        onArchive: () => archiveCustomer(customer),
        onPurchases: () => purchasesModal(customer),
      })

    actions.append(
      h(
        "button",
        {
          class: "icon-button action-button",
          type: "button",
          "aria-label": `Ver detalhes de ${customer.name}`,
          title: "Ver detalhes",
          onClick: openDetails,
        },
        svgIcon("eye"),
      ),
    )
    if (canManage() && customer.active) {
      actions.append(
        h(
          "button",
          {
            class: "icon-button action-button",
            type: "button",
            "aria-label": `Editar ${customer.name}`,
            title: "Editar",
            onClick: () => customerForm({ customer, onSaved: load }),
          },
          svgIcon("edit"),
        ),
        h(
          "button",
          {
            class: "icon-button action-button action-danger",
            type: "button",
            "aria-label": `Arquivar ${customer.name}`,
            title: "Arquivar",
            onClick: () => archiveCustomer(customer),
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
        { "data-label": "Nome" },
        h(
          "div",
          { class: "cell-primary" },
          h("strong", { text: customer.name }),
          h("span", {
            class: "cell-muted",
            text: customer.email || "Sem e-mail",
          }),
        ),
      ),
      h(
        "td",
        { "data-label": "CPF" },
        customer.cpf ? formatCpf(customer.cpf) : "—",
      ),
      h(
        "td",
        { "data-label": "Telefone" },
        customer.phone ? formatPhone(customer.phone) : "—",
      ),
      h(
        "td",
        { "data-label": "Cadastrado em" },
        formatDate(customer.createdAt),
      ),
      h(
        "td",
        { "data-label": "Situação" },
        customer.active
          ? h("span", { class: "status status-normal", text: "Ativo" })
          : h("span", { class: "badge badge-muted", text: "Arquivado" }),
      ),
      h("td", { "data-label": "Ações", class: "cell-actions" }, actions),
    )
  }

  function renderTable(page) {
    if (!page.content.length) {
      tableArea.replaceChildren(
        filters.search || filters.includeInactive
          ? emptyState({
              title: "Nenhum cliente encontrado",
              message: "Ajuste a busca ou inclua clientes arquivados.",
              actionLabel: "Limpar filtros",
              onAction: resetFilters,
            })
          : emptyState({
              title: "Nenhum cliente cadastrado",
              message:
                "Cadastre clientes para vincular vendas e consultar o histórico de compras.",
              actionLabel: canManage() ? "Cadastrar cliente" : undefined,
              onAction: canManage()
                ? () => customerForm({ onSaved: load })
                : undefined,
            }),
      )
      paginationArea.replaceChildren()
      resultsSummary.textContent = "Nenhum cliente para exibir."
      return
    }

    tableArea.replaceChildren(
      h(
        "div",
        { class: "table-wrapper" },
        h(
          "table",
          { class: "data-table" },
          h("caption", { class: "sr-only", text: "Clientes cadastrados" }),
          h(
            "thead",
            {},
            h(
              "tr",
              {},
              h("th", { scope: "col" }, "Nome"),
              h("th", { scope: "col" }, "CPF"),
              h("th", { scope: "col" }, "Telefone"),
              h("th", { scope: "col" }, "Cadastrado em"),
              h("th", { scope: "col" }, "Situação"),
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
      page.totalElements === 1 ? "cliente encontrado" : "clientes encontrados"
    }.`
    paginationArea.replaceChildren(
      createPagination({
        page: page.page,
        totalPages: page.totalPages,
        totalElements: page.totalElements,
        itemLabel: "clientes",
        onChange: (nextPage) => {
          filters.page = nextPage
          load()
        },
      }),
    )
  }

  async function load() {
    const current = ++requestId
    tableArea.replaceChildren(loadingState("Carregando clientes…"))
    resultsSummary.textContent = ""
    clearButton.hidden = !(filters.search || filters.includeInactive)

    try {
      const page = await api.get("/customers", {
        query: {
          storeId: state.storeId,
          search: filters.search || undefined,
          includeInactive: filters.includeInactive,
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
  inactiveToggle.querySelector("input").addEventListener("change", (event) => {
    filters.includeInactive = event.target.checked
    filters.page = 0
    load()
  })
  clearButton.addEventListener("click", resetFilters)

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      heading,
      panel(
        "customers-panel",
        toolbar,
        resultsSummary,
        tableArea,
        paginationArea,
      ),
    ),
  )
  await load()
}
