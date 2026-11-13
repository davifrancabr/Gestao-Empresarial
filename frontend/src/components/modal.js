import { h, svgIcon } from "../utils/dom.js"

let openDialogs = 0

/**
 * Modal acessível baseado em <dialog>, com título associado, foco gerenciado
 * e fechamento por Escape/backdrop.
 */
export function openModal({
  title,
  description,
  content,
  footer,
  size = "md",
  className = "",
  onClose,
  initialFocus,
} = {}) {
  const titleId = `modal-title-${Date.now()}-${Math.random().toString(16).slice(2, 7)}`
  const body = h("div", { class: "modal-body" })
  if (content) body.append(...(Array.isArray(content) ? content : [content]))

  const closeButton = h(
    "button",
    {
      class: "icon-button modal-close",
      type: "button",
      "aria-label": "Fechar janela",
      onClick: () => dialog.close(),
    },
    svgIcon("close"),
  )

  const footerNode = footer
    ? h(
        "footer",
        { class: "modal-footer" },
        ...(Array.isArray(footer) ? footer : [footer]),
      )
    : null

  // Botões de envio no rodapé ficam fora do <form> (que vive no corpo do
  // modal). O atributo "form" mantém o submit nativo funcionando.
  const form = body.querySelector("form")
  if (form && footerNode) {
    if (!form.id) {
      form.id = `modal-form-${Date.now()}-${Math.random().toString(16).slice(2, 7)}`
    }
    for (const button of footerNode.querySelectorAll('button[type="submit"]')) {
      if (!button.getAttribute("form")) button.setAttribute("form", form.id)
    }
  }

  const dialog = h(
    "dialog",
    {
      class: `modal modal-${size} ${className}`.trim(),
      "aria-labelledby": titleId,
    },
    h(
      "div",
      { class: "modal-card" },
      h(
        "header",
        { class: "modal-header" },
        h(
          "div",
          {},
          h("h2", { id: titleId, text: title }),
          description ? h("p", { text: description }) : null,
        ),
        closeButton,
      ),
      body,
      footerNode,
    ),
  )

  let closed = false
  const restoreFocus = document.activeElement

  dialog.addEventListener("close", () => {
    if (closed) return
    closed = true
    openDialogs = Math.max(openDialogs - 1, 0)
    if (openDialogs === 0)
      document.documentElement.classList.remove("modal-open")
    dialog.remove()
    if (
      restoreFocus instanceof HTMLElement &&
      document.contains(restoreFocus)
    ) {
      restoreFocus.focus({ preventScroll: true })
    }
    onClose?.()
  })

  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close()
  })

  document.body.append(dialog)
  openDialogs += 1
  document.documentElement.classList.add("modal-open")

  if (typeof dialog.showModal === "function") dialog.showModal()
  else dialog.setAttribute("open", "")

  const focusTarget =
    initialFocus ??
    body.querySelector("input, select, textarea, button") ??
    closeButton
  window.setTimeout(() => focusTarget?.focus({ preventScroll: true }), 0)

  return {
    element: dialog,
    body,
    close: () => dialog.close(),
  }
}

/**
 * Diálogo de confirmação para operações irreversíveis.
 */
export function confirmAction({
  title = "Confirmar operação",
  message,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  danger = false,
  requireText,
} = {}) {
  return new Promise((resolve) => {
    let settled = false
    let input = null
    let confirmButton = null

    const finish = (value) => {
      if (settled) return
      settled = true
      modal.close()
      resolve(value)
    }

    const content = h(
      "div",
      { class: "confirm-content" },
      h("p", { text: message }),
    )
    if (requireText) {
      input = h("input", {
        type: "text",
        class: "confirm-input",
        "aria-label": `Digite ${requireText} para confirmar`,
        placeholder: requireText,
        autocomplete: "off",
      })
      content.append(
        h(
          "label",
          { class: "form-field confirm-field" },
          h("span", { text: `Digite “${requireText}” para confirmar:` }),
          input,
        ),
      )
    }

    confirmButton = h(
      "button",
      {
        class: `button ${danger ? "button-danger" : "button-primary"}`,
        type: "button",
        disabled: Boolean(requireText),
        onClick: () => finish(true),
      },
      confirmLabel,
    )

    const modal = openModal({
      title,
      size: "sm",
      content,
      footer: [
        h(
          "button",
          {
            class: "button button-secondary",
            type: "button",
            onClick: () => finish(false),
          },
          cancelLabel,
        ),
        confirmButton,
      ],
      onClose: () => {
        if (!settled) {
          settled = true
          resolve(false)
        }
      },
    })

    if (input) {
      input.addEventListener("input", () => {
        confirmButton.disabled = input.value.trim() !== requireText
      })
      window.setTimeout(() => input.focus(), 30)
    } else {
      window.setTimeout(() => confirmButton.focus(), 30)
    }
  })
}
