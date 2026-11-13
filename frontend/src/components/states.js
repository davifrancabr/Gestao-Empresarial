import { h, svgIcon } from "../utils/dom.js"

export function loadingState(message = "Carregando dados…") {
  return h(
    "div",
    { class: "state-panel", role: "status", "aria-live": "polite" },
    h("span", { class: "spinner", "aria-hidden": "true" }),
    h("p", { text: message }),
  )
}

export function errorState(
  message,
  { onRetry, title = "Não foi possível carregar os dados" } = {},
) {
  return h(
    "div",
    { class: "state-panel state-panel-error", role: "alert" },
    h("div", { class: "state-icon", "aria-hidden": "true" }, svgIcon("alert")),
    h("h2", { text: title }),
    h("p", { text: message }),
    onRetry
      ? h(
          "button",
          {
            class: "button button-secondary",
            type: "button",
            onClick: onRetry,
          },
          svgIcon("refresh"),
          "Tentar novamente",
        )
      : null,
  )
}

export function emptyState({
  icon = "package",
  title,
  message,
  actionLabel,
  onAction,
} = {}) {
  return h(
    "div",
    { class: "empty-state empty-block" },
    h("div", { class: "empty-icon", "aria-hidden": "true" }, svgIcon(icon)),
    h("h3", { text: title }),
    message ? h("p", { text: message }) : null,
    actionLabel && onAction
      ? h(
          "button",
          { class: "button button-primary", type: "button", onClick: onAction },
          svgIcon("plus"),
          actionLabel,
        )
      : null,
  )
}

export function sectionHeader({ kicker, title, description, actions }) {
  return h(
    "div",
    { class: "section-header" },
    h(
      "div",
      {},
      kicker ? h("p", { class: "section-kicker", text: kicker }) : null,
      h("h2", { text: title }),
      description
        ? h("p", { class: "section-description", text: description })
        : null,
    ),
    actions
      ? h(
          "div",
          { class: "section-actions" },
          ...(Array.isArray(actions) ? actions : [actions]),
        )
      : null,
  )
}

export function panel(className, ...children) {
  return h("section", { class: `panel ${className ?? ""}`.trim() }, ...children)
}

export function dataBadge(label, className = "") {
  return h("span", { class: `badge ${className}`.trim(), text: label })
}
