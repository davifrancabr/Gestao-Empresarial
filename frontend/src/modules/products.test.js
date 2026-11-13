import { beforeEach, describe, expect, it, vi } from "vitest"
import { state } from "../context.js"
import { api } from "../services/api.js"
import { render } from "./products.js"

vi.mock("../services/api.js", () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

function product(overrides = {}) {
  return {
    id: "produto-1",
    storeId: "loja-1",
    name: "Arroz 5 kg",
    sku: "ARR-005",
    category: "Mercearia",
    description: null,
    quantity: 42,
    minimum: 20,
    unit: "un",
    status: "NORMAL",
    active: true,
    version: 0,
    createdAt: "2026-05-01T10:00:00Z",
    updatedAt: "2026-05-09T10:00:00Z",
    ...overrides,
  }
}

function page(content, overrides = {}) {
  return {
    content,
    page: 0,
    size: 10,
    totalPages: content.length ? 1 : 0,
    totalElements: content.length,
    ...overrides,
  }
}

describe("products module", () => {
  let container

  beforeEach(() => {
    document.body.innerHTML =
      '<div id="toast-region"></div><div id="alert-region"></div><div id="container"></div>'
    container = document.querySelector("#container")
    state.session = {
      user: {
        id: "u1",
        name: "Admin",
        email: "admin@gestao.local",
        role: "ADMIN",
      },
      stores: [{ id: "loja-1", name: "Loja Matriz" }],
      activeStoreId: "loja-1",
    }
    state.storeId = "loja-1"
    state.route = "produtos"
    vi.clearAllMocks()
  })

  it("renders the product table with status, balance and pagination", async () => {
    api.get.mockImplementation((path) => {
      if (path === "/products/categories") return Promise.resolve(["Mercearia"])
      if (path === "/products") return Promise.resolve(page([product()]))
      return Promise.reject(new Error(`Rota inesperada: ${path}`))
    })

    await render(container)

    const rows = container.querySelectorAll("tbody tr")
    expect(rows).toHaveLength(1)
    expect(rows[0].textContent).toContain("Arroz 5 kg")
    expect(rows[0].textContent).toContain("42 un")
    expect(rows[0].textContent).toContain("Normal")
    expect(container.querySelector(".pagination-summary").textContent).toBe(
      "1 produtos · página 1 de 1",
    )

    expect(api.get).toHaveBeenCalledWith("/products", {
      query: expect.objectContaining({
        storeId: "loja-1",
        sort: "name,asc",
        page: 0,
        size: 10,
      }),
    })
  })

  it("renders the empty state when the store has no products", async () => {
    api.get.mockImplementation((path) => {
      if (path === "/products/categories") return Promise.resolve([])
      if (path === "/products")
        return Promise.resolve(page([], { totalPages: 0 }))
      return Promise.reject(new Error(`Rota inesperada: ${path}`))
    })

    await render(container)

    expect(container.textContent).toContain("Nenhum produto cadastrado")
    expect(container.querySelectorAll("tbody tr")).toHaveLength(0)
  })

  it("hides management actions for operators", async () => {
    state.session.user.role = "OPERADOR"
    api.get.mockImplementation((path) => {
      if (path === "/products/categories") return Promise.resolve([])
      if (path === "/products") return Promise.resolve(page([product()]))
      return Promise.reject(new Error(`Rota inesperada: ${path}`))
    })

    await render(container)

    expect(container.textContent).not.toContain("Novo produto")
    expect(
      container.querySelector('[aria-label="Editar Arroz 5 kg"]'),
    ).toBeNull()
    expect(
      container.querySelector('[aria-label="Ver detalhes de Arroz 5 kg"]'),
    ).not.toBeNull()
  })

  it("sends the current version when saving an edit", async () => {
    api.get.mockImplementation((path) => {
      if (path === "/products/categories") return Promise.resolve([])
      if (path === "/products")
        return Promise.resolve(page([product({ version: 3 })]))
      return Promise.reject(new Error(`Rota inesperada: ${path}`))
    })
    api.put.mockResolvedValue(
      product({ name: "Arroz 5 kg Integral", version: 4 }),
    )

    await render(container)
    container.querySelector('[aria-label="Editar Arroz 5 kg"]').click()

    const form = document.querySelector("dialog form")
    form.elements.namedItem("name").value = "Arroz 5 kg Integral"
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }))
    await vi.waitFor(() => expect(api.put).toHaveBeenCalled())

    expect(api.put).toHaveBeenCalledWith(
      "/products/produto-1",
      expect.objectContaining({ name: "Arroz 5 kg Integral", version: 3 }),
      { query: { storeId: "loja-1" } },
    )
  })
})
