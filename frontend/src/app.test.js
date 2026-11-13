import { readFileSync } from "node:fs"
import { resolve } from "node:path"
import { beforeAll, describe, expect, it, vi } from "vitest"
import { api } from "./services/api.js"

const session = {
  user: { id: "u1", name: "Admin", email: "admin@gestao.local", role: "ADMIN" },
  stores: [
    { id: "loja-1", name: "Loja Matriz" },
    { id: "loja-2", name: "Loja Filial" },
  ],
  activeStoreId: "loja-1",
}

const rendered = vi.hoisted(() => [])
const dashboardState = vi.hoisted(() => ({ gate: null }))

vi.mock("./services/api.js", () => ({
  api: { me: vi.fn(), logout: vi.fn(), login: vi.fn() },
  resetCsrf: vi.fn(),
}))

vi.mock("./modules/login.js", () => ({
  initLogin: vi.fn(() => ({
    show() {
      document.querySelector("#login-view").hidden = false
    },
    hide() {
      document.querySelector("#login-view").hidden = true
    },
  })),
}))

vi.mock("./modules/dashboard.js", () => ({
  render: vi.fn(async (container) => {
    rendered.push("dashboard")
    if (dashboardState.gate) await dashboardState.gate
    container.textContent = "view:dashboard"
  }),
}))

vi.mock("./modules/products.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:products"
  }),
}))

vi.mock("./modules/stock.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:stock"
  }),
}))

vi.mock("./modules/movements.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:movements"
  }),
}))

vi.mock("./modules/customers.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:customers"
  }),
}))

vi.mock("./modules/sales.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:sales"
  }),
}))

vi.mock("./modules/reports.js", () => ({
  render: vi.fn(async (container) => {
    container.textContent = "view:reports"
  }),
}))

vi.mock("./modules/settings.js", () => ({
  applyPreferences: vi.fn(),
  render: vi.fn(async (container) => {
    container.textContent = "view:settings"
  }),
}))

describe("app bootstrap", () => {
  beforeAll(async () => {
    const html = readFileSync(resolve(process.cwd(), "index.html"), "utf8")
    document.body.innerHTML = html.match(/<body>([\s\S]*)<\/body>/)[1]
    window.localStorage.clear()
    window.history.replaceState(null, "", "/")
    window.scrollTo = vi.fn()
    window.matchMedia = vi.fn(() => ({
      matches: false,
      media: "",
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    }))

    api.me.mockResolvedValue(session)
    await import("./app.js")
  })

  it("loads the session, store selector and initial route", async () => {
    const appShell = document.querySelector("#app-shell")
    const loginView = document.querySelector("#login-view")

    await vi.waitFor(() => expect(appShell.hidden).toBe(false))
    await vi.waitFor(() =>
      expect(document.querySelector("#view").textContent).toBe(
        "view:dashboard",
      ),
    )

    expect(loginView.hidden).toBe(true)
    expect(document.querySelector("#boot-screen").hidden).toBe(true)
    expect(document.querySelector("#user-name").textContent).toBe("Admin")
    expect(document.querySelector("#user-role").textContent).toBe(
      "Administrador",
    )
    expect(document.querySelector("#store-select").options).toHaveLength(2)
    expect(document.title).toBe("Painel · Gestão Empresarial")

    const activeNav = document.querySelector('a[data-route="dashboard"]')
    expect(activeNav.classList.contains("nav-item-active")).toBe(true)
    expect(activeNav.getAttribute("aria-current")).toBe("page")
    expect(rendered).toContain("dashboard")
  })

  it("persists store changes and re-renders the active route", async () => {
    const rendersBefore = rendered.length
    const select = document.querySelector("#store-select")
    select.value = "loja-2"
    select.dispatchEvent(new Event("change"))

    const { state } = await import("./context.js")
    await vi.waitFor(() => expect(state.storeId).toBe("loja-2"))
    expect(window.localStorage.getItem("gestao-active-store")).toBe("loja-2")
    await vi.waitFor(() =>
      expect(rendered.length).toBeGreaterThan(rendersBefore),
    )
    expect(rendered.at(-1)).toBe("dashboard")
  })

  it("does not let a stale route overwrite the current view", async () => {
    const go = (route) => {
      window.history.replaceState(null, "", `/#/${route}`)
      window.dispatchEvent(new HashChangeEvent("hashchange"))
    }

    go("produtos")
    await vi.waitFor(() =>
      expect(document.querySelector("#view").textContent).toBe("view:products"),
    )

    let releaseDashboard
    dashboardState.gate = new Promise((resolve) => {
      releaseDashboard = resolve
    })

    go("dashboard")
    go("produtos")
    await vi.waitFor(() =>
      expect(document.querySelector("#view").textContent).toBe("view:products"),
    )

    releaseDashboard()
    await new Promise((resolve) => setTimeout(resolve, 20))
    dashboardState.gate = null

    expect(document.querySelector("#view").textContent).toBe("view:products")
  })

  it("returns to the login screen when the session expires", async () => {
    window.dispatchEvent(new CustomEvent("gestao:unauthorized"))

    await vi.waitFor(() =>
      expect(document.querySelector("#app-shell").hidden).toBe(true),
    )
    expect(document.querySelector("#login-view").hidden).toBe(false)
    expect(document.querySelector("#login-error").textContent).toContain(
      "expirou",
    )
  })
})
