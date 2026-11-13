/**
 * Pequenos utilitários de DOM usados por todos os módulos.
 * A criação de elementos é preferida em relação a innerHTML para evitar XSS.
 */

const BOOLEAN_ATTRIBUTES = new Set([
  "disabled",
  "required",
  "readonly",
  "checked",
  "selected",
])

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;")
}

function appendChildren(element, children) {
  for (const child of children) {
    if (child === null || child === undefined || child === false) continue
    if (Array.isArray(child)) {
      appendChildren(element, child)
      continue
    }
    if (child instanceof Node) {
      element.append(child)
      continue
    }
    element.append(document.createTextNode(String(child)))
  }
}

/**
 * Cria um elemento. Suporta className, dataset, text, html (apenas conteúdo
 * confiável), eventos (onClick, onInput) e atributos ARIA.
 */
export function h(tag, props = {}, ...children) {
  const element = document.createElement(tag)
  for (const [key, value] of Object.entries(props ?? {})) {
    if (value === null || value === undefined || value === false) continue
    if (key === "class" || key === "className") {
      element.className = value
    } else if (key === "dataset") {
      Object.assign(element.dataset, value)
    } else if (key === "text") {
      element.textContent = value
    } else if (key === "html") {
      element.innerHTML = value
    } else if (key === "value") {
      element.value = value
    } else if (key === "style" && typeof value === "object") {
      Object.assign(element.style, value)
    } else if (key.startsWith("on") && typeof value === "function") {
      element.addEventListener(key.slice(2).toLowerCase(), value)
    } else if (BOOLEAN_ATTRIBUTES.has(key)) {
      element[key] = Boolean(value)
    } else {
      element.setAttribute(key, value === true ? "" : String(value))
    }
  }
  appendChildren(element, children)
  return element
}

export function svgIcon(name, className = "icon") {
  return h(
    "svg",
    { class: className, "aria-hidden": "true", focusable: "false" },
    h("use", { href: `#icon-${name}` }),
  )
}

export function clear(node) {
  if (node) node.replaceChildren()
}

export function debounce(fn, delay = 350) {
  let timer
  const debounced = (...args) => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), delay)
  }
  debounced.cancel = () => window.clearTimeout(timer)
  return debounced
}

/**
 * Adiciona uma mensagem de erro acessível abaixo do campo.
 */
export function setFieldError(form, name, message) {
  if (!form) return
  const field = form.elements.namedItem(name)
  if (!field || !(field instanceof HTMLElement)) return
  const errorId = `${field.id || name}-error`
  let error = form.querySelector(`#${CSS.escape(errorId)}`)
  if (!error) {
    error = h("p", { class: "field-error", id: errorId })
    const wrapper = field.closest(".form-field") ?? field.parentElement
    wrapper?.append(error)
  }
  error.textContent = message
  error.hidden = false
  field.setAttribute("aria-invalid", "true")
  const describedBy = new Set(
    (field.getAttribute("aria-describedby") ?? "").split(/\s+/).filter(Boolean),
  )
  describedBy.add(errorId)
  field.setAttribute("aria-describedby", [...describedBy].join(" "))
}

export function clearFieldErrors(form) {
  if (!form) return
  form.querySelectorAll(".field-error").forEach((error) => error.remove())
  form.querySelectorAll('[aria-invalid="true"]').forEach((field) => {
    field.removeAttribute("aria-invalid")
    const describedBy = (field.getAttribute("aria-describedby") ?? "")
      .split(/\s+/)
      .filter((id) => id && !id.endsWith("-error"))
    if (describedBy.length)
      field.setAttribute("aria-describedby", describedBy.join(" "))
    else field.removeAttribute("aria-describedby")
  })
}

export function focusFirstError(form) {
  form?.querySelector('[aria-invalid="true"]')?.focus()
}

/**
 * Aplica os erros de validação devolvidos pela API aos campos do formulário.
 */
export function applyApiFieldErrors(form, fieldErrors = {}) {
  let found = false
  for (const [field, message] of Object.entries(fieldErrors)) {
    setFieldError(form, field, message)
    found = true
  }
  if (found) focusFirstError(form)
  return found
}

export function setButtonLoading(button, loading, loadingLabel = "Salvando…") {
  if (!button) return
  if (loading) {
    if (!button.dataset.originalLabel) {
      button.dataset.originalLabel = button.textContent.trim()
    }
    button.disabled = true
    button.setAttribute("aria-busy", "true")
    button.textContent = loadingLabel
  } else {
    button.disabled = false
    button.removeAttribute("aria-busy")
    if (button.dataset.originalLabel) {
      button.textContent = button.dataset.originalLabel
      delete button.dataset.originalLabel
    }
  }
}

export function formatCount(count, singular, plural) {
  return `${count} ${count === 1 ? singular : plural}`
}
