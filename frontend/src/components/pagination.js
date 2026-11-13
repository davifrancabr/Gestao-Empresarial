import { h } from "../utils/dom.js"
import { formatNumber } from "../utils/formatters.js"

function pageWindow(currentPage, totalPages) {
  const pages = new Set([
    0,
    totalPages - 1,
    currentPage,
    currentPage - 1,
    currentPage + 1,
  ])
  return [...pages]
    .filter((page) => page >= 0 && page < totalPages)
    .sort((a, b) => a - b)
}

/**
 * Paginação acessível. `onChange` recebe o índice da página (base 0).
 */
export function createPagination({
  page,
  totalPages,
  totalElements,
  onChange,
  itemLabel = "itens",
}) {
  const safeTotal = Math.max(totalPages, 1)
  const summary = h("p", {
    class: "pagination-summary",
    "aria-live": "polite",
    text: `${formatNumber(totalElements)} ${itemLabel} · página ${page + 1} de ${safeTotal}`,
  })

  const nav = h("nav", {
    class: "pagination",
    "aria-label": "Paginação dos resultados",
  })
  const list = h("div", { class: "pagination-pages" })

  const prev = h(
    "button",
    {
      class: "pagination-button",
      type: "button",
      "aria-label": "Página anterior",
      disabled: page <= 0,
      onClick: () => onChange(page - 1),
    },
    "‹",
  )

  const next = h(
    "button",
    {
      class: "pagination-button",
      type: "button",
      "aria-label": "Próxima página",
      disabled: page >= safeTotal - 1,
      onClick: () => onChange(page + 1),
    },
    "›",
  )

  let previous = -1
  for (const index of pageWindow(page, safeTotal)) {
    if (previous >= 0 && index - previous > 1) {
      list.append(
        h("span", { class: "pagination-gap", "aria-hidden": "true" }, "…"),
      )
    }
    list.append(
      h(
        "button",
        {
          class: `pagination-button ${
            index === page ? "pagination-current" : ""
          }`.trim(),
          type: "button",
          "aria-label": `Página ${index + 1}`,
          "aria-current": index === page ? "page" : null,
          onClick: () => index !== page && onChange(index),
        },
        String(index + 1),
      ),
    )
    previous = index
  }

  nav.append(prev, list, next)

  return h("div", { class: "pagination-bar" }, summary, nav)
}
