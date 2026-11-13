import { describe, expect, it, vi } from "vitest"
import { createPagination } from "./pagination.js"

describe("createPagination", () => {
  it("announces the current page and total results", () => {
    const element = createPagination({
      page: 2,
      totalPages: 5,
      totalElements: 42,
      onChange: vi.fn(),
    })
    expect(element.querySelector(".pagination-summary").textContent).toBe(
      "42 itens · página 3 de 5",
    )
    expect(
      element
        .querySelector('[aria-label="Página 3"]')
        .getAttribute("aria-current"),
    ).toBe("page")
    expect(
      element.querySelector('[aria-label="Página anterior"]').disabled,
    ).toBe(false)
    expect(
      element.querySelector('[aria-label="Próxima página"]').disabled,
    ).toBe(false)
  })

  it("disables navigation at the boundaries", () => {
    const first = createPagination({
      page: 0,
      totalPages: 2,
      totalElements: 12,
      onChange: vi.fn(),
    })
    expect(first.querySelector('[aria-label="Página anterior"]').disabled).toBe(
      true,
    )

    const last = createPagination({
      page: 1,
      totalPages: 2,
      totalElements: 12,
      onChange: vi.fn(),
    })
    expect(last.querySelector('[aria-label="Próxima página"]').disabled).toBe(
      true,
    )
  })

  it("emits zero-based page changes", () => {
    const onChange = vi.fn()
    const element = createPagination({
      page: 1,
      totalPages: 4,
      totalElements: 40,
      onChange,
    })
    element.querySelector('[aria-label="Página 4"]').click()
    expect(onChange).toHaveBeenCalledWith(3)

    element.querySelector('[aria-label="Página anterior"]').click()
    expect(onChange).toHaveBeenCalledWith(0)
  })
})
