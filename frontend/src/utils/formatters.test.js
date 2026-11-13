import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  formatCpf,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatDateTimeLong,
  formatNumber,
  formatPhone,
  initials,
  movementType,
  productStatus,
} from "./formatters.js"

describe("formatters", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date("2026-05-10T15:30:00"))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("formats numbers and currency in pt-BR", () => {
    expect(formatNumber(1234)).toBe("1.234")
    expect(formatCurrency(1234.5)).toContain("1.234,50")
    expect(formatCurrency(null)).toContain("0,00")
  })

  it("formats dates without ambiguity", () => {
    expect(formatDate("2026-05-10T12:00:00")).toBe("10/05/2026")
    expect(formatDateTimeLong("2026-05-10T12:00:00")).toBe("10/05/2026, 12:00")
    expect(formatDateTime("2026-05-10T09:05:00")).toBe("Hoje, 09:05")
    expect(formatDateTime("2026-05-09T18:40:00")).toBe("Ontem, 18:40")
    expect(formatDateTime("2026-05-01T08:00:00")).toBe("01/05/2026, 08:00")
  })

  it("handles invalid dates", () => {
    expect(formatDate("não é data")).toBe("—")
    expect(formatDateTime(null)).toBe("—")
  })

  it("maps product status to mutually exclusive labels", () => {
    expect(productStatus("NORMAL")).toEqual({
      label: "Normal",
      className: "status-normal",
    })
    expect(productStatus("LOW").label).toBe("Estoque baixo")
    expect(productStatus("OUT_OF_STOCK").label).toBe("Esgotado")
    expect(productStatus("UNKNOWN").label).toBe("UNKNOWN")
  })

  it("maps movement types with sign and icon", () => {
    expect(movementType("ENTRY")).toMatchObject({ label: "Entrada", sign: "+" })
    expect(movementType("EXIT")).toMatchObject({ label: "Saída", sign: "−" })
    expect(movementType("ADJUSTMENT").icon).toBe("sliders")
  })

  it("formats CPF, phone and initials", () => {
    expect(formatCpf("52998224725")).toBe("529.982.247-25")
    expect(formatPhone("11987654321")).toBe("(11) 98765-4321")
    expect(formatPhone("1134567890")).toBe("(11) 3456-7890")
    expect(initials("Maria da Silva")).toBe("MS")
    expect(initials("Ana")).toBe("AN")
    expect(initials("")).toBe("?")
  })
})
