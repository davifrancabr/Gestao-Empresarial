import { h, svgIcon } from "../utils/dom.js"

const toastRegion = () => document.querySelector("#toast-region")
const alertRegion = () => document.querySelector("#alert-region")

function makeToast({ title, message, type, close }) {
  return h(
    "div",
    {
      class: `toast ${
        type === "error" ? "toast-error" : type === "info" ? "toast-info" : ""
      }`,
      role: type === "error" ? "alert" : "status",
    },
    h(
      "span",
      { class: "toast-mark", "aria-hidden": "true" },
      type === "error" ? "!" : "✓",
    ),
    h(
      "div",
      { class: "toast-copy" },
      h("strong", { text: title }),
      h("p", { text: message }),
    ),
    h(
      "button",
      {
        class: "icon-button",
        type: "button",
        "aria-label": "Fechar mensagem",
        onClick: close,
      },
      svgIcon("close"),
    ),
  )
}

/**
 * Notificações gerais (sucesso/ informação) desaparecem automaticamente;
 * erros permanecem até serem fechados para não passarem despercebidos (P19).
 */
export function notify(type, title, message) {
  const region = type === "error" ? alertRegion() : toastRegion()
  if (!region) return

  const isError = type === "error"
  const toast = makeToast({ title, message, type, close: () => toast.remove() })

  if (isError) {
    region.replaceChildren(toast)
  } else {
    region.append(toast)
    while (region.children.length > 3) region.firstElementChild.remove()
  }

  toast.classList.add("toast-visible")
  if (!isError) {
    let timer = window.setTimeout(() => toast.remove(), 5000)
    const pause = () => window.clearTimeout(timer)
    const resume = () => {
      timer = window.setTimeout(() => toast.remove(), 2500)
    }
    toast.addEventListener("mouseenter", pause)
    toast.addEventListener("mouseleave", resume)
    toast.addEventListener("focusin", pause)
    toast.addEventListener("focusout", resume)
  }

  if (isError) toast.querySelector("button")?.focus({ preventScroll: true })
}

export function notifySuccess(title, message) {
  notify("success", title, message)
}

export function notifyError(title, message) {
  notify("error", title, message)
}

/**
 * Exibe o erro de uma chamada de API, priorizando a mensagem do backend.
 */
export function notifyApiError(
  error,
  fallback = "Não foi possível concluir a operação.",
) {
  const message = error?.message || fallback
  notify("error", error?.error || "Não foi possível concluir", message)
}

export function clearNotifications() {
  toastRegion()?.replaceChildren()
  alertRegion()?.replaceChildren()
}
