import { api, resetCsrf } from "./services/api.js"
import {
  clearSession,
  displayRole,
  persistStore,
  readStoredStore,
  state,
} from "./context.js"
import { clearNotifications } from "./components/notifications.js"
import { errorState } from "./components/states.js"
import { initLogin } from "./modules/login.js"
import * as dashboard from "./modules/dashboard.js"
import * as products from "./modules/products.js"
import * as stock from "./modules/stock.js"
import * as movements from "./modules/movements.js"
import * as customers from "./modules/customers.js"
import * as sales from "./modules/sales.js"
import * as reports from "./modules/reports.js"
import * as settings from "./modules/settings.js"
import { applyPreferences } from "./modules/settings.js"
import { h, svgIcon } from "./utils/dom.js"
import { initials } from "./utils/formatters.js"

/**
 * Rotas do sistema. Cada módulo é responsável por renderizar seu próprio
 * conteúdo dentro do contêiner principal.
 */
const ROUTES = {
  dashboard: {
    title: "Painel",
    eyebrow: "Visão geral",
    render: dashboard.render,
  },
  produtos: {
    title: "Produtos",
    eyebrow: "Cadastro",
    render: products.render,
  },
  estoque: {
    title: "Estoque",
    eyebrow: "Controle",
    render: stock.render,
  },
  movimentacoes: {
    title: "Movimentações",
    eyebrow: "Auditoria",
    render: movements.render,
  },
  clientes: {
    title: "Clientes",
    eyebrow: "Relacionamento",
    render: customers.render,
  },
  vendas: {
    title: "Vendas",
    eyebrow: "Comercial",
    render: sales.render,
  },
  relatorios: {
    title: "Relatórios",
    eyebrow: "Análise",
    render: reports.render,
  },
  configuracoes: {
    title: "Configurações",
    eyebrow: "Sistema",
    render: settings.render,
  },
}

const DEFAULT_ROUTE = "dashboard"
const APP_NAME = "Gestão Empresarial"

const elements = {
  bootScreen: document.querySelector("#boot-screen"),
  loginView: document.querySelector("#login-view"),
  loginError: document.querySelector("#login-error"),
  appShell: document.querySelector("#app-shell"),
  sidebar: document.querySelector("#sidebar"),
  menuBackdrop: document.querySelector("#menu-backdrop"),
  openMenu: document.querySelector("#open-menu"),
  closeMenu: document.querySelector("#close-menu"),
  pageEyebrow: document.querySelector("#page-eyebrow"),
  pageTitle: document.querySelector("#page-title"),
  mainContent: document.querySelector("#main-content"),
  view: document.querySelector("#view"),
  storeSelect: document.querySelector("#store-select"),
  userName: document.querySelector("#user-name"),
  userRole: document.querySelector("#user-role"),
  userInitials: document.querySelector("#user-initials"),
  logoutButton: document.querySelector("#logout-button"),
}

let renderToken = 0
let menuOpen = false
let login = null

const mobileMenu = window.matchMedia("(max-width: 920px)")

function routeIdFromHash() {
  const raw = window.location.hash.replace(/^#\/?/, "").split("?")[0]
  return Object.hasOwn(ROUTES, raw) ? raw : DEFAULT_ROUTE
}

function setRouteHash(routeId) {
  const target = `#/${routeId}`
  if (window.location.hash !== target) {
    window.history.replaceState(null, "", target)
  }
}

function syncChrome(routeId) {
  const route = ROUTES[routeId]
  elements.pageEyebrow.textContent = route.eyebrow
  elements.pageTitle.textContent = route.title
  document.title = `${route.title} · ${APP_NAME}`

  for (const link of elements.sidebar.querySelectorAll("a[data-route]")) {
    const active = link.dataset.route === routeId
    link.classList.toggle("nav-item-active", active)
    if (active) link.setAttribute("aria-current", "page")
    else link.removeAttribute("aria-current")
  }
}

function renderNoStore() {
  return h(
    "div",
    { class: "state-panel state-panel-error", role: "alert" },
    h("div", { class: "state-icon", "aria-hidden": "true" }, svgIcon("store")),
    h("h2", { text: "Nenhuma loja vinculada" }),
    h("p", {
      text: "Seu usuário não possui acesso a nenhuma loja ativa. Peça a um administrador para vincular uma loja ao seu perfil.",
    }),
  )
}

async function renderRoute({ focus = false } = {}) {
  if (!state.session) return

  const routeId = routeIdFromHash()
  const route = ROUTES[routeId]
  state.route = routeId
  setRouteHash(routeId)
  syncChrome(routeId)

  const token = ++renderToken
  elements.view.setAttribute("aria-busy", "true")

  // Cada rota renderiza em um contêiner próprio. Se outra navegação começar
  // antes desta terminar, o resultado antigo fica desanexado e não sobrescreve
  // a página atual.
  const outlet = document.createElement("div")
  elements.view.replaceChildren(outlet)

  try {
    if (!state.storeId) {
      outlet.replaceChildren(renderNoStore())
      return
    }
    await route.render(outlet)
  } catch (error) {
    if (error?.status === 401) return
    if (token !== renderToken) return
    elements.view.replaceChildren(
      errorState(error?.message ?? "Não foi possível carregar esta página.", {
        onRetry: () => renderRoute({ focus }),
      }),
    )
  } finally {
    if (token === renderToken) {
      elements.view.removeAttribute("aria-busy")
      if (focus) {
        window.scrollTo(0, 0)
        elements.pageTitle.focus({ preventScroll: true })
      }
    }
  }
}

function renderStoreOptions() {
  const stores = state.session?.stores ?? []
  elements.storeSelect.replaceChildren(
    ...stores.map((store) => h("option", { value: store.id }, store.name)),
  )
  if (state.storeId) elements.storeSelect.value = state.storeId
  elements.storeSelect.disabled = stores.length <= 1
  const label = stores.length <= 1 ? "Loja ativa" : "Selecionar loja ativa"
  elements.storeSelect.setAttribute("aria-label", label)
}

function renderUserChip() {
  const user = state.session?.user
  if (!user) return
  elements.userName.textContent = user.name
  elements.userRole.textContent = displayRole(user.role)
  elements.userInitials.textContent = initials(user.name)
}

function enterApp(session) {
  clearNotifications()

  const stores = session?.stores ?? []
  state.session = session
  const preferred = readStoredStore()
  state.storeId =
    stores.find((store) => store.id === preferred)?.id ??
    session.activeStoreId ??
    stores[0]?.id ??
    null
  persistStore(state.storeId)

  renderStoreOptions()
  renderUserChip()

  login.hide()
  elements.bootScreen.hidden = true
  elements.appShell.hidden = false

  renderRoute()
}

function showLogin(message = "") {
  renderToken += 1
  clearSession()
  closeMenu({ restoreFocus: false })
  clearNotifications()
  elements.view.replaceChildren()
  elements.appShell.hidden = true
  elements.storeSelect.replaceChildren()
  elements.bootScreen.hidden = true
  elements.loginError.textContent = message
  elements.loginError.hidden = !message
  login.show()
}

/* ------------------------------------------------------------------ */
/* Menu mobile (P09): foco, Escape, scroll e fechamento ao redimensionar */
/* ------------------------------------------------------------------ */

function menuFocusables() {
  return [
    ...elements.sidebar.querySelectorAll("a[href], button:not([disabled])"),
  ]
}

function openMenu() {
  if (menuOpen || !mobileMenu.matches) return
  menuOpen = true
  elements.sidebar.classList.add("sidebar-open")
  elements.menuBackdrop.hidden = false
  elements.openMenu.setAttribute("aria-expanded", "true")
  document.body.classList.add("menu-open")
  elements.closeMenu.focus({ preventScroll: true })
}

function closeMenu({ restoreFocus = true } = {}) {
  if (!menuOpen) {
    elements.menuBackdrop.hidden = true
    document.body.classList.remove("menu-open")
    elements.openMenu.setAttribute("aria-expanded", "false")
    return
  }
  menuOpen = false
  elements.sidebar.classList.remove("sidebar-open")
  elements.menuBackdrop.hidden = true
  elements.openMenu.setAttribute("aria-expanded", "false")
  document.body.classList.remove("menu-open")
  if (restoreFocus && mobileMenu.matches) {
    elements.openMenu.focus({ preventScroll: true })
  }
}

function handleMenuKeydown(event) {
  if (!menuOpen) return

  if (event.key === "Escape") {
    event.preventDefault()
    closeMenu()
    return
  }

  if (event.key !== "Tab") return
  const focusables = menuFocusables()
  if (!focusables.length) return
  const first = focusables[0]
  const last = focusables[focusables.length - 1]

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

/* ------------------------------------------------------------------ */
/* Sessão                                                              */
/* ------------------------------------------------------------------ */

async function logout() {
  let message = ""
  try {
    await api.logout()
  } catch (error) {
    if (error?.status !== 401) {
      // Mesmo com falha de rede, a sessão local deixa de ser usada; o
      // cookie HttpOnly expira no servidor.
      message =
        "Não foi possível encerrar a sessão no servidor. Feche o navegador se necessário."
    }
  } finally {
    resetCsrf()
    persistStore(null)
    showLogin(message)
  }
}

function handleUnauthorized() {
  if (!state.session) return
  resetCsrf()
  showLogin("Sua sessão expirou. Entre novamente para continuar.")
}

/* ------------------------------------------------------------------ */
/* Inicialização                                                       */
/* ------------------------------------------------------------------ */

function bindEvents() {
  elements.openMenu.addEventListener("click", openMenu)
  elements.closeMenu.addEventListener("click", () => closeMenu())
  elements.menuBackdrop.addEventListener("click", () => closeMenu())
  elements.sidebar.querySelector("nav").addEventListener("click", (event) => {
    const link = event.target.closest("a[data-route]")
    if (!link) return
    const sameRoute = link.dataset.route === state.route
    if (menuOpen) closeMenu({ restoreFocus: false })
    if (sameRoute) renderRoute({ focus: true })
  })

  document.addEventListener("keydown", handleMenuKeydown)

  const onViewportChange = (event) => {
    if (!event.matches) closeMenu({ restoreFocus: false })
  }
  if (typeof mobileMenu.addEventListener === "function") {
    mobileMenu.addEventListener("change", onViewportChange)
  } else {
    mobileMenu.addListener(onViewportChange)
  }

  elements.storeSelect.addEventListener("change", () => {
    state.storeId = elements.storeSelect.value || null
    persistStore(state.storeId)
    renderRoute()
  })

  elements.logoutButton.addEventListener("click", logout)

  window.addEventListener("hashchange", () => renderRoute({ focus: true }))
  window.addEventListener("gestao:unauthorized", handleUnauthorized)
}

async function bootstrap() {
  applyPreferences()
  login = initLogin({
    onAuthenticated: (session) => {
      resetCsrf()
      enterApp(session)
    },
  })
  bindEvents()

  try {
    const session = await api.me()
    enterApp(session)
  } catch (error) {
    const message =
      error?.status === 401 || error?.status === 403
        ? ""
        : "Não foi possível verificar sua sessão. Verifique se o servidor está disponível e tente entrar novamente."
    showLogin(message)
  }
}

bootstrap()
