import { describe, expect, it } from "vitest"
import {
  isValidCpf,
  isValidEmail,
  normalizeName,
  normalizeSpaces,
  validateCustomer,
  validateLogin,
  validateMovement,
  validateProduct,
  validateStore,
  validateUser,
} from "./validators.js"

describe("validators", () => {
  it("normalizes names for comparison", () => {
    expect(normalizeSpaces("  Arroz   integral  ")).toBe("Arroz integral")
    expect(normalizeName("  João   da   Silva ")).toBe("joao da silva")
    expect(normalizeName("AÇÃO")).toBe("acao")
  })

  it("validates e-mail and CPF", () => {
    expect(isValidEmail("pessoa@empresa.com.br")).toBe(true)
    expect(isValidEmail("pessoa@empresa")).toBe(false)
    expect(isValidCpf("529.982.247-25")).toBe(true)
    expect(isValidCpf("111.111.111-11")).toBe(false)
    expect(isValidCpf("123")).toBe(false)
  })

  it("validates product form", () => {
    expect(
      validateProduct({ name: "", minimum: "", quantity: "" }, {
        creating: true,
      }),
    ).toEqual({
      name: "Informe o nome do produto.",
      minimum: "Informe o estoque mínimo.",
      quantity: "Informe a quantidade inicial.",
    })

    expect(
      validateProduct(
        { name: "Arroz 5 kg", minimum: "10", quantity: "42", unit: "un" },
        { creating: true },
      ),
    ).toEqual({})

    expect(validateProduct({ name: "Arroz", minimum: "-1" })).toMatchObject({
      minimum: "O estoque mínimo não pode ser negativo.",
    })
  })

  it("validates customer form", () => {
    expect(validateCustomer({ name: "" })).toMatchObject({
      name: "Informe o nome do cliente.",
    })
    expect(
      validateCustomer({ name: "Maria", cpf: "111.111.111-11" }),
    ).toMatchObject({
      cpf: "Informe um CPF válido ou deixe o campo vazio.",
    })
    expect(
      validateCustomer({ name: "Maria", email: "invalido" }),
    ).toMatchObject({
      email: "Informe um e-mail válido ou deixe o campo vazio.",
    })
    expect(validateCustomer({ name: "Maria", cpf: "", email: "" })).toEqual({})
  })

  it("validates stock movements including adjustments", () => {
    expect(
      validateMovement({
        productId: "",
        quantity: "",
        reason: "",
        type: "ENTRY",
      }),
    ).toMatchObject({
      productId: "Selecione o produto.",
      quantity: "Informe a quantidade.",
      reason: "Informe o motivo da movimentação.",
    })
    expect(
      validateMovement({
        productId: "p1",
        quantity: "0",
        reason: "Venda",
        type: "EXIT",
      }),
    ).toMatchObject({
      quantity: "A quantidade deve ser maior que zero.",
    })
    expect(
      validateMovement({
        productId: "p1",
        quantity: "0",
        reason: "Inventário",
        type: "ADJUSTMENT",
      }),
    ).toEqual({})
  })

  it("validates login, users and stores", () => {
    expect(validateLogin({ email: "", password: "" })).toEqual({
      email: "Informe o e-mail.",
      password: "Informe a senha.",
    })
    expect(
      validateUser(
        { name: "Ana", email: "ana@empresa.com", role: "OPERADOR" },
        { creating: true },
      ),
    ).toMatchObject({
      password: "Informe a senha.",
    })
    expect(
      validateUser({
        name: "Ana",
        email: "ana@empresa.com",
        role: "ADMIN",
        password: "12345678",
      }),
    ).toEqual({})
    expect(validateStore({ name: "" })).toMatchObject({
      name: "Informe o nome da loja.",
    })
  })
})
