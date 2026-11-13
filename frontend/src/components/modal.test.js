import { beforeEach, describe, expect, it } from "vitest"
import { h } from "../utils/dom.js"
import { openModal } from "./modal.js"

describe("openModal", () => {
  beforeEach(() => {
    document.body.innerHTML = ""
    document.documentElement.className = ""
  })

  it("associates footer submit buttons with the modal form", () => {
    const form = h("form", {}, h("input", { name: "name" }))
    const submit = h("button", { type: "submit" }, "Salvar")

    openModal({ title: "Teste", content: form, footer: [submit] })

    expect(form.id).toBeTruthy()
    expect(submit.getAttribute("form")).toBe(form.id)
    expect(document.getElementById(form.id)).toBe(form)
  })

  it("keeps an explicit form association", () => {
    const form = h("form", { id: "custom-form" }, h("input", { name: "name" }))
    const submit = h(
      "button",
      { type: "submit", form: "custom-form" },
      "Salvar",
    )

    openModal({ title: "Teste", content: form, footer: [submit] })

    expect(submit.getAttribute("form")).toBe("custom-form")
  })
})
