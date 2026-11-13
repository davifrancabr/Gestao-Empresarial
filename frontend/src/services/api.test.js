import { beforeEach, describe, expect, it, vi } from "vitest"
import { api, ApiError, resetCsrf } from "./api.js"

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  }
}

describe("api client", () => {
  beforeEach(() => {
    resetCsrf()
    vi.unstubAllGlobals()
  })

  it("builds query strings and sends credentials", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ content: [] }))
    vi.stubGlobal("fetch", fetchMock)

    const result = await api.get("/products", {
      query: { storeId: "loja-1", page: 0, search: "", status: undefined },
    })

    expect(result).toEqual({ content: [] })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain("/api/v1/products?storeId=loja-1&page=0")
    expect(options.credentials).toBe("include")
  })

  it("fetches and reuses the CSRF token for mutations", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ headerName: "X-XSRF-TOKEN", token: "token-123" }),
      )
      .mockResolvedValueOnce(jsonResponse({ id: "p1" }))
      .mockResolvedValueOnce(jsonResponse({ id: "p2" }))
    vi.stubGlobal("fetch", fetchMock)

    await api.post("/products", { name: "Arroz" })
    await api.post("/products", { name: "Feijão" })

    expect(fetchMock).toHaveBeenCalledTimes(3)
    const [, firstMutation] = fetchMock.mock.calls[1]
    const [, secondMutation] = fetchMock.mock.calls[2]
    expect(firstMutation.headers["X-XSRF-TOKEN"]).toBe("token-123")
    expect(secondMutation.headers["X-XSRF-TOKEN"]).toBe("token-123")
    expect(firstMutation.body).toBe(JSON.stringify({ name: "Arroz" }))
  })

  it("maps API error payloads to ApiError", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ headerName: "X-XSRF-TOKEN", token: "token-123" }),
      )
      .mockResolvedValueOnce(
        jsonResponse(
          {
            error: "Dados inválidos",
            message: "Revise os campos destacados.",
            fieldErrors: { name: "Já existe um produto com esse nome." },
          },
          422,
        ),
      )
    vi.stubGlobal("fetch", fetchMock)

    const promise = api.post("/products", { name: "Arroz" })
    await expect(promise).rejects.toBeInstanceOf(ApiError)
    await expect(promise).rejects.toMatchObject({
      name: "ApiError",
      status: 422,
      message: "Revise os campos destacados.",
      fieldErrors: { name: "Já existe um produto com esse nome." },
    })
  })

  it("reports network failures with a friendly message", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("failed to fetch")),
    )
    await expect(api.get("/dashboard")).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining("Falha de comunicação"),
    })
  })
})
