import { api } from "../services/api.js"
import { displayRole, isAdmin, state, storeName } from "../context.js"
import { openModal } from "../components/modal.js"
import { notifyApiError, notifySuccess } from "../components/notifications.js"
import { errorState, loadingState, panel } from "../components/states.js"
import {
  applyApiFieldErrors,
  h,
  setButtonLoading,
  svgIcon,
} from "../utils/dom.js"
import { formatDateTime } from "../utils/formatters.js"
import { validateStore, validateUser } from "../utils/validators.js"

const ROLE_OPTIONS = [
  { value: "ADMIN", label: "Administrador" },
  { value: "GERENTE", label: "Gerente" },
  { value: "OPERADOR", label: "Operador" },
]

function accountPanel() {
  const user = state.session.user
  return panel(
    "settings-panel",
    h(
      "div",
      { class: "panel-heading" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Conta" }),
        h("h2", { text: "Seus dados" }),
      ),
    ),
    h(
      "dl",
      { class: "detail-list" },
      h("div", {}, h("dt", { text: "Nome" }), h("dd", { text: user.name })),
      h("div", {}, h("dt", { text: "E-mail" }), h("dd", { text: user.email })),
      h(
        "div",
        {},
        h("dt", { text: "Perfil de acesso" }),
        h("dd", { text: displayRole(user.role) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Loja ativa" }),
        h("dd", { text: storeName() }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Lojas vinculadas" }),
        h("dd", {
          text:
            state.session.stores.map((store) => store.name).join(", ") ||
            "Nenhuma loja",
        }),
      ),
    ),
  )
}

function preferencesPanel() {
  const prefs = readPreferences()

  const reduceMotion = h("input", {
    type: "checkbox",
    checked: prefs.reduceMotion,
  })
  const contrast = h("input", { type: "checkbox", checked: prefs.highContrast })

  reduceMotion.addEventListener("change", () => {
    savePreferences({
      ...readPreferences(),
      reduceMotion: reduceMotion.checked,
    })
    applyPreferences()
  })
  contrast.addEventListener("change", () => {
    savePreferences({ ...readPreferences(), highContrast: contrast.checked })
    applyPreferences()
  })

  return panel(
    "settings-panel",
    h(
      "div",
      { class: "panel-heading" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Interface" }),
        h("h2", { text: "Preferências" }),
      ),
    ),
    h("p", {
      class: "section-description",
      text: "Preferências visuais ficam salvas apenas neste navegador e não contêm dados de negócio.",
    }),
    h(
      "div",
      { class: "preference-list" },
      h(
        "label",
        { class: "checkbox-label" },
        reduceMotion,
        h(
          "span",
          {},
          h("strong", { text: "Reduzir animações" }),
          h("small", { text: "Diminui transições e efeitos de movimento." }),
        ),
      ),
      h(
        "label",
        { class: "checkbox-label" },
        contrast,
        h(
          "span",
          {},
          h("strong", { text: "Aumentar contraste" }),
          h("small", {
            text: "Reforça bordas e cores para melhor legibilidade.",
          }),
        ),
      ),
    ),
  )
}

function securityPanel() {
  return panel(
    "settings-panel",
    h(
      "div",
      { class: "panel-heading" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Sessão" }),
        h("h2", { text: "Segurança" }),
      ),
    ),
    h("p", {
      class: "section-description",
      text: "A sessão é mantida por cookie HttpOnly e expira automaticamente. Para sair em outro dispositivo, encerre a sessão abaixo.",
    }),
    h(
      "div",
      { class: "panel-footer-actions" },
      h(
        "button",
        {
          class: "button button-danger",
          type: "button",
          onClick: () => document.querySelector("#logout-button")?.click(),
        },
        svgIcon("logout"),
        "Encerrar sessão",
      ),
    ),
  )
}

function userForm({ user, stores, onSaved }) {
  const creating = !user
  const selectedStores = new Set(user?.stores?.map((store) => store.id) ?? [])
  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "user-name", text: "Nome *" }),
      h("input", {
        id: "user-name",
        name: "name",
        maxlength: 120,
        required: true,
        value: user?.name ?? "",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "user-email", text: "E-mail *" }),
      h("input", {
        id: "user-email",
        name: "email",
        type: "email",
        maxlength: 160,
        required: true,
        autocomplete: "off",
        value: user?.email ?? "",
      }),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", { for: "user-role", text: "Perfil *" }),
      h(
        "select",
        { id: "user-role", name: "role" },
        ROLE_OPTIONS.map((option) =>
          h(
            "option",
            {
              value: option.value,
              selected: (user?.role ?? "OPERADOR") === option.value,
            },
            option.label,
          ),
        ),
      ),
    ),
    h(
      "div",
      { class: "form-field" },
      h("label", {
        for: "user-password",
        text: creating ? "Senha *" : "Nova senha",
      }),
      h("input", {
        id: "user-password",
        name: "password",
        type: "password",
        minlength: 8,
        maxlength: 100,
        autocomplete: "new-password",
        placeholder: creating
          ? "Mínimo de 8 caracteres"
          : "Deixe vazio para manter",
      }),
    ),
    creating
      ? null
      : h(
          "label",
          { class: "checkbox-label form-field-wide" },
          h("input", {
            type: "checkbox",
            name: "active",
            checked: user.active,
          }),
          h("span", { text: "Usuário ativo" }),
        ),
    h(
      "fieldset",
      { class: "form-field form-field-wide store-checklist" },
      h("legend", { text: "Lojas vinculadas" }),
      stores.map((store) =>
        h(
          "label",
          { class: "checkbox-label" },
          h("input", {
            type: "checkbox",
            name: "storeIds",
            value: store.id,
            checked: selectedStores.has(store.id),
          }),
          h("span", { text: store.name }),
        ),
      ),
    ),
  )

  const submit = h(
    "button",
    { class: "button button-primary", type: "submit" },
    creating ? "Criar usuário" : "Salvar usuário",
  )
  const modal = openModal({
    title: creating ? "Novo usuário" : "Editar usuário",
    description:
      "Administradores podem gerenciar usuários, perfis e vínculos com lojas.",
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
    const formData = new FormData(form)
    const values = Object.fromEntries(formData.entries())
    values.storeIds = formData.getAll("storeIds")
    values.active = creating ? true : formData.get("active") === "on"

    const errors = validateUser(values, { creating })
    if (Object.keys(errors).length) {
      applyFieldErrorsFromObject(form, errors)
      return
    }

    setButtonLoading(submit, true)
    try {
      const payload = {
        name: values.name,
        email: values.email,
        role: values.role,
        storeIds: values.storeIds,
        active: values.active,
      }
      if (values.password) payload.password = values.password
      const saved = creating
        ? await api.post("/users", { ...payload, password: values.password })
        : await api.put(`/users/${user.id}`, payload)
      modal.close()
      notifySuccess(
        creating ? "Usuário criado" : "Usuário atualizado",
        `${saved.name} foi salvo com sucesso.`,
      )
      onSaved?.(saved)
    } catch (error) {
      if (!applyApiFieldErrors(form, error.fieldErrors)) {
        notifyApiError(error, "Não foi possível salvar o usuário.")
      }
    } finally {
      setButtonLoading(submit, false)
    }
  })
}

function applyFieldErrorsFromObject(form, errors) {
  // A API devolve os mesmos nomes de campos; reaproveita o mecanismo acessível.
  const fakeError = { fieldErrors: errors }
  applyApiFieldErrors(form, fakeError.fieldErrors)
}

function usersPanel(container) {
  container.replaceChildren(loadingState("Carregando usuários…"))
  Promise.all([api.get("/users"), api.get("/stores")])
    .then(([users, stores]) => {
      const rows = users.map((user) =>
        h(
          "tr",
          {},
          h(
            "td",
            { "data-label": "Nome" },
            h(
              "div",
              { class: "cell-primary" },
              h("strong", { text: user.name }),
              h("span", { class: "cell-muted", text: user.email }),
            ),
          ),
          h("td", { "data-label": "Perfil" }, displayRole(user.role)),
          h(
            "td",
            { "data-label": "Lojas", class: "cell-wrap" },
            user.stores.map((store) => store.name).join(", ") || "—",
          ),
          h(
            "td",
            { "data-label": "Situação" },
            user.active
              ? h("span", { class: "status status-normal", text: "Ativo" })
              : h("span", { class: "badge badge-muted", text: "Inativo" }),
          ),
          h(
            "td",
            { "data-label": "Ações", class: "cell-actions" },
            h(
              "button",
              {
                class: "icon-button action-button",
                type: "button",
                "aria-label": `Editar ${user.name}`,
                title: "Editar",
                onClick: () =>
                  userForm({
                    user,
                    stores,
                    onSaved: () => usersPanel(container),
                  }),
              },
              svgIcon("edit"),
            ),
          ),
        ),
      )

      container.replaceChildren(
        panel(
          "settings-panel",
          h(
            "div",
            { class: "panel-heading" },
            h(
              "div",
              {},
              h("p", { class: "section-kicker", text: "Administração" }),
              h("h2", { text: "Usuários" }),
            ),
            h(
              "button",
              {
                class: "button button-primary button-small",
                type: "button",
                onClick: () =>
                  userForm({ stores, onSaved: () => usersPanel(container) }),
              },
              svgIcon("plus"),
              "Novo usuário",
            ),
          ),
          h(
            "div",
            { class: "table-wrapper" },
            h(
              "table",
              { class: "data-table" },
              h("caption", { class: "sr-only", text: "Usuários do sistema" }),
              h(
                "thead",
                {},
                h(
                  "tr",
                  {},
                  h("th", { scope: "col" }, "Usuário"),
                  h("th", { scope: "col" }, "Perfil"),
                  h("th", { scope: "col" }, "Lojas"),
                  h("th", { scope: "col" }, "Situação"),
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
        ),
      )
    })
    .catch((error) =>
      container.replaceChildren(
        errorState(error.message, { onRetry: () => usersPanel(container) }),
      ),
    )
}

function storeForm({ store, onSaved }) {
  const creating = !store
  const form = h(
    "form",
    { class: "form-grid", novalidate: true },
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "store-name", text: "Nome da loja *" }),
      h("input", {
        id: "store-name",
        name: "name",
        maxlength: 120,
        required: true,
        value: store?.name ?? "",
      }),
    ),
    h(
      "div",
      { class: "form-field form-field-wide" },
      h("label", { for: "store-document", text: "Documento (CNPJ)" }),
      h("input", {
        id: "store-document",
        name: "document",
        maxlength: 20,
        value: store?.document ?? "",
      }),
    ),
    creating
      ? null
      : h(
          "label",
          { class: "checkbox-label form-field-wide" },
          h("input", {
            type: "checkbox",
            name: "active",
            checked: store.active,
          }),
          h("span", { text: "Loja ativa" }),
        ),
  )

  const submit = h(
    "button",
    { class: "button button-primary", type: "submit" },
    creating ? "Criar loja" : "Salvar loja",
  )
  const modal = openModal({
    title: creating ? "Nova loja" : "Editar loja",
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
    const formData = new FormData(form)
    const values = Object.fromEntries(formData.entries())
    values.active = creating ? true : formData.get("active") === "on"

    const errors = validateStore(values)
    if (Object.keys(errors).length) {
      applyFieldErrorsFromObject(form, errors)
      return
    }

    setButtonLoading(submit, true)
    try {
      const payload = {
        name: values.name,
        document: values.document || null,
        active: values.active,
      }
      const saved = creating
        ? await api.post("/stores", payload)
        : await api.put(`/stores/${store.id}`, payload)
      modal.close()
      notifySuccess(
        creating ? "Loja criada" : "Loja atualizada",
        `${saved.name} foi salva com sucesso.`,
      )
      onSaved?.(saved)
    } catch (error) {
      if (!applyApiFieldErrors(form, error.fieldErrors)) {
        notifyApiError(error, "Não foi possível salvar a loja.")
      }
    } finally {
      setButtonLoading(submit, false)
    }
  })
}

function storesPanel(container) {
  container.replaceChildren(loadingState("Carregando lojas…"))
  api
    .get("/stores")
    .then((stores) => {
      const rows = stores.map((store) =>
        h(
          "tr",
          {},
          h("td", { "data-label": "Loja" }, h("strong", { text: store.name })),
          h("td", { "data-label": "Documento" }, store.document || "—"),
          h(
            "td",
            { "data-label": "Situação" },
            store.active
              ? h("span", { class: "status status-normal", text: "Ativa" })
              : h("span", { class: "badge badge-muted", text: "Inativa" }),
          ),
          h(
            "td",
            { "data-label": "Cadastrada em" },
            formatDateTime(store.createdAt),
          ),
          h(
            "td",
            { "data-label": "Ações", class: "cell-actions" },
            h(
              "button",
              {
                class: "icon-button action-button",
                type: "button",
                "aria-label": `Editar ${store.name}`,
                title: "Editar",
                onClick: () =>
                  storeForm({ store, onSaved: () => storesPanel(container) }),
              },
              svgIcon("edit"),
            ),
          ),
        ),
      )

      container.replaceChildren(
        panel(
          "settings-panel",
          h(
            "div",
            { class: "panel-heading" },
            h(
              "div",
              {},
              h("p", { class: "section-kicker", text: "Administração" }),
              h("h2", { text: "Lojas" }),
            ),
            h(
              "button",
              {
                class: "button button-primary button-small",
                type: "button",
                onClick: () =>
                  storeForm({ onSaved: () => storesPanel(container) }),
              },
              svgIcon("plus"),
              "Nova loja",
            ),
          ),
          h(
            "div",
            { class: "table-wrapper" },
            h(
              "table",
              { class: "data-table" },
              h("caption", { class: "sr-only", text: "Lojas do sistema" }),
              h(
                "thead",
                {},
                h(
                  "tr",
                  {},
                  h("th", { scope: "col" }, "Loja"),
                  h("th", { scope: "col" }, "Documento"),
                  h("th", { scope: "col" }, "Situação"),
                  h("th", { scope: "col" }, "Cadastrada em"),
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
        ),
      )
    })
    .catch((error) =>
      container.replaceChildren(
        errorState(error.message, { onRetry: () => storesPanel(container) }),
      ),
    )
}

const ADMIN_TABS = [
  { id: "usuarios", label: "Usuários", render: usersPanel },
  { id: "lojas", label: "Lojas", render: storesPanel },
]

export async function render(container) {
  const content = h(
    "div",
    { class: "settings-stack" },
    accountPanel(),
    preferencesPanel(),
    securityPanel(),
  )

  if (!isAdmin()) {
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
            h("p", { class: "section-kicker", text: "Sistema" }),
            h("h2", { text: "Configurações" }),
            h("p", {
              class: "section-description",
              text: "Informações da conta, preferências de interface e segurança da sessão.",
            }),
          ),
        ),
        content,
      ),
    )
    return
  }

  const adminArea = h("div")
  const tabList = h("div", {
    class: "tab-list",
    role: "tablist",
    "aria-label": "Administração",
  })
  const buttons = new Map()
  let selected = "usuarios"

  function selectTab(id) {
    selected = id
    for (const [tabId, button] of buttons) {
      button.setAttribute("aria-selected", String(tabId === id))
      button.classList.toggle("active", tabId === id)
    }
    adminArea.setAttribute("role", "tabpanel")
    adminArea.setAttribute("aria-labelledby", `settings-tab-${id}`)
    const tab = ADMIN_TABS.find((item) => item.id === id)
    tab.render(adminArea)
  }

  for (const tab of ADMIN_TABS) {
    const button = h(
      "button",
      {
        class: "tab-button",
        type: "button",
        role: "tab",
        id: `settings-tab-${tab.id}`,
        "aria-selected": String(tab.id === selected),
        onClick: () => selectTab(tab.id),
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
          h("p", { class: "section-kicker", text: "Sistema" }),
          h("h2", { text: "Configurações" }),
          h("p", {
            class: "section-description",
            text: "Gerencie sua conta, preferências, usuários e lojas.",
          }),
        ),
      ),
      content,
      h("div", { class: "tab-scroll" }, tabList),
      adminArea,
    ),
  )
  selectTab(selected)
}

function readPreferences() {
  try {
    return {
      reduceMotion: false,
      highContrast: false,
      ...JSON.parse(
        window.localStorage.getItem("gestao-ui-preferences") || "{}",
      ),
    }
  } catch {
    return { reduceMotion: false, highContrast: false }
  }
}

function savePreferences(preferences) {
  try {
    window.localStorage.setItem(
      "gestao-ui-preferences",
      JSON.stringify(preferences),
    )
  } catch {
    // Preferências são opcionais.
  }
}

export function applyPreferences() {
  const { reduceMotion, highContrast } = readPreferences()
  document.documentElement.classList.toggle(
    "pref-reduce-motion",
    Boolean(reduceMotion),
  )
  document.documentElement.classList.toggle(
    "pref-high-contrast",
    Boolean(highContrast),
  )
}
