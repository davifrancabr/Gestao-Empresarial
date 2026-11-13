/**
 * Validações de formulário espelhando as regras do backend. A API continua
 * sendo a fonte de verdade; estas verificações apenas antecipam o feedback.
 */

export function normalizeSpaces(value) {
  return String(value ?? "")
    .trim()
    .replace(/\s+/g, " ")
}

export function normalizeName(value) {
  return normalizeSpaces(value)
    .normalize("NFD")
    .replaceAll(/[\u0300-\u036f]/g, "")
    .toLowerCase()
}

export function onlyDigits(value) {
  return String(value ?? "").replace(/\D/g, "")
}

export function isBlank(value) {
  return normalizeSpaces(value).length === 0
}

export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(String(value ?? "").trim())
}

/**
 * Validação de CPF com dígitos verificadores. CPF é opcional no cadastro.
 */
export function isValidCpf(value) {
  const digits = onlyDigits(value)
  if (digits.length !== 11) return false
  if (/^(\d)\1{10}$/.test(digits)) return false

  const calculate = (length) => {
    let sum = 0
    for (let index = 0; index < length; index += 1) {
      sum += Number(digits[index]) * (length + 1 - index)
    }
    const remainder = (sum * 10) % 11
    return remainder === 10 ? 0 : remainder
  }

  return (
    calculate(9) === Number(digits[9]) && calculate(10) === Number(digits[10])
  )
}

export function parseInteger(value) {
  if (value === "" || value === null || value === undefined) return null
  const number = Number(value)
  return Number.isInteger(number) ? number : null
}

function maxLength(value, max, label) {
  return String(value ?? "").length > max
    ? `${label} deve ter no máximo ${max} caracteres.`
    : null
}

export function validateLogin(values) {
  const errors = {}
  if (isBlank(values.email)) errors.email = "Informe o e-mail."
  else if (!isValidEmail(values.email))
    errors.email = "Informe um e-mail válido."
  if (!values.password) errors.password = "Informe a senha."
  return errors
}

export function validateProduct(values, { creating = false } = {}) {
  const errors = {}
  if (isBlank(values.name)) errors.name = "Informe o nome do produto."
  else errors.name = maxLength(normalizeSpaces(values.name), 80, "O nome")

  errors.sku = maxLength(values.sku, 40, "O SKU")
  errors.category = maxLength(values.category, 60, "A categoria")
  errors.description = maxLength(values.description, 500, "A descrição")
  errors.unit = maxLength(values.unit, 10, "A unidade")

  const minimum = parseInteger(values.minimum)
  if (values.minimum === "") errors.minimum = "Informe o estoque mínimo."
  else if (minimum === null)
    errors.minimum = "O estoque mínimo deve ser um número inteiro."
  else if (minimum < 0)
    errors.minimum = "O estoque mínimo não pode ser negativo."

  if (creating) {
    const quantity = parseInteger(values.quantity)
    if (values.quantity === "")
      errors.quantity = "Informe a quantidade inicial."
    else if (quantity === null)
      errors.quantity = "A quantidade inicial deve ser um número inteiro."
    else if (quantity < 0)
      errors.quantity = "A quantidade inicial não pode ser negativa."
  }

  return compact(errors)
}

export function validateCustomer(values) {
  const errors = {}
  if (isBlank(values.name)) errors.name = "Informe o nome do cliente."
  else errors.name = maxLength(normalizeSpaces(values.name), 120, "O nome")

  errors.cpf =
    values.cpf && onlyDigits(values.cpf) && !isValidCpf(values.cpf)
      ? "Informe um CPF válido ou deixe o campo vazio."
      : maxLength(values.cpf, 14, "O CPF")

  errors.phone = maxLength(values.phone, 20, "O telefone")
  errors.email =
    values.email && !isValidEmail(values.email)
      ? "Informe um e-mail válido ou deixe o campo vazio."
      : maxLength(values.email, 160, "O e-mail")

  return compact(errors)
}

export function validateMovement(values) {
  const errors = {}
  if (!values.productId) errors.productId = "Selecione o produto."

  const quantity = parseInteger(values.quantity)
  if (values.quantity === "") errors.quantity = "Informe a quantidade."
  else if (quantity === null)
    errors.quantity = "A quantidade deve ser um número inteiro."
  else if (values.type === "ADJUSTMENT") {
    if (quantity < 0)
      errors.quantity = "O saldo ajustado não pode ser negativo."
  } else if (quantity <= 0) {
    errors.quantity = "A quantidade deve ser maior que zero."
  }

  if (isBlank(values.reason))
    errors.reason = "Informe o motivo da movimentação."
  else
    errors.reason = maxLength(normalizeSpaces(values.reason), 120, "O motivo")

  errors.observation = maxLength(values.observation, 500, "A observação")
  return compact(errors)
}

export function validateUser(values, { creating = false } = {}) {
  const errors = {}
  if (isBlank(values.name)) errors.name = "Informe o nome."
  else errors.name = maxLength(normalizeSpaces(values.name), 120, "O nome")

  if (isBlank(values.email)) errors.email = "Informe o e-mail."
  else if (!isValidEmail(values.email))
    errors.email = "Informe um e-mail válido."
  else errors.email = maxLength(values.email, 160, "O e-mail")

  if (creating && !values.password) {
    errors.password = "Informe a senha."
  } else if (values.password && values.password.length < 8) {
    errors.password = "A senha deve ter ao menos 8 caracteres."
  } else if (String(values.password ?? "").length > 100) {
    errors.password = "A senha deve ter no máximo 100 caracteres."
  }

  if (!values.role) errors.role = "Informe o perfil de acesso."
  return compact(errors)
}

export function validateStore(values) {
  const errors = {}
  if (isBlank(values.name)) errors.name = "Informe o nome da loja."
  else errors.name = maxLength(normalizeSpaces(values.name), 120, "O nome")
  errors.document = maxLength(values.document, 20, "O documento")
  return compact(errors)
}

function compact(object) {
  return Object.fromEntries(
    Object.entries(object).filter(([, value]) => Boolean(value)),
  )
}

export function hasErrors(errors) {
  return Object.keys(errors).length > 0
}
