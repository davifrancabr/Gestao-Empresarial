const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
})

const dateFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
})

const timeFormatter = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
})

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

const numberFormatter = new Intl.NumberFormat("pt-BR")

export function formatCurrency(value) {
  const number = Number(value ?? 0)
  return currencyFormatter.format(Number.isFinite(number) ? number : 0)
}

export function formatNumber(value) {
  return numberFormatter.format(Number(value ?? 0))
}

export function formatDate(value) {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? "—" : dateFormatter.format(date)
}

export function formatTime(value) {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? "—" : timeFormatter.format(date)
}

function startOfDay(date) {
  const copy = new Date(date)
  copy.setHours(0, 0, 0, 0)
  return copy
}

/**
 * Exibe data e hora com um rótulo explícito para hoje/ontem, eliminando a
 * ambiguidade entre dias diferentes (P13).
 */
export function formatDateTime(value) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"

  const today = startOfDay(new Date())
  const target = startOfDay(date)
  const diffDays = Math.round((today.getTime() - target.getTime()) / 86_400_000)

  if (diffDays === 0) return `Hoje, ${timeFormatter.format(date)}`
  if (diffDays === 1) return `Ontem, ${timeFormatter.format(date)}`
  return dateTimeFormatter.format(date)
}

export function formatDateTimeLong(value) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return dateTimeFormatter.format(date)
}

export function formatIsoDate(value) {
  if (!value) return ""
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

export function initials(name) {
  const parts = String(name ?? "")
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (parts.length === 0) return "?"
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
}

const PRODUCT_STATUS = {
  NORMAL: { label: "Normal", className: "status-normal" },
  LOW: { label: "Estoque baixo", className: "status-estoque-baixo" },
  OUT_OF_STOCK: { label: "Esgotado", className: "status-esgotado" },
}

export function productStatus(status) {
  return (
    PRODUCT_STATUS[status] ?? {
      label: status ?? "—",
      className: "status-neutral",
    }
  )
}

const MOVEMENT_TYPE = {
  ENTRY: {
    label: "Entrada",
    className: "movement-entry",
    sign: "+",
    icon: "down",
  },
  EXIT: { label: "Saída", className: "movement-exit", sign: "−", icon: "up" },
  ADJUSTMENT: {
    label: "Ajuste",
    className: "movement-adjustment",
    sign: "±",
    icon: "sliders",
  },
}

export function movementType(type) {
  return (
    MOVEMENT_TYPE[type] ?? {
      label: type ?? "—",
      className: "movement-neutral",
      sign: "",
      icon: "package",
    }
  )
}

export function formatSku(sku) {
  return sku ? sku.toUpperCase() : ""
}

export function truncate(value, max = 48) {
  const text = String(value ?? "")
  return text.length > max ? `${text.slice(0, max - 1)}…` : text
}

export function formatCpf(cpf) {
  const digits = String(cpf ?? "").replace(/\D/g, "")
  if (digits.length !== 11) return cpf ?? "—"
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4")
}

export function formatPhone(phone) {
  const digits = String(phone ?? "").replace(/\D/g, "")
  if (digits.length === 11)
    return digits.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3")
  if (digits.length === 10)
    return digits.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3")
  return phone ?? "—"
}
