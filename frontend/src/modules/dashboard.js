import { api } from "../services/api.js"
import { loadingState, errorState, panel } from "../components/states.js"
import { h, svgIcon } from "../utils/dom.js"
import {
  formatCurrency,
  formatDateTime,
  formatNumber,
  movementType,
  productStatus,
} from "../utils/formatters.js"
import { navigate, state } from "../context.js"

function statCard({ label, value, icon, tone = "", hint }) {
  return h(
    "article",
    { class: `stat-card ${tone}`.trim() },
    h("div", { class: "stat-icon", "aria-hidden": "true" }, svgIcon(icon)),
    h(
      "div",
      { class: "stat-copy" },
      h("p", { text: label }),
      h("strong", { text: value }),
      hint ? h("span", { class: "stat-hint", text: hint }) : null,
    ),
  )
}

function trendChart(trend) {
  const max = Math.max(
    1,
    ...trend.flatMap((point) => [point.entries, point.exits]),
  )
  const totalEntries = trend.reduce((sum, point) => sum + point.entries, 0)
  const totalExits = trend.reduce((sum, point) => sum + point.exits, 0)

  const bars = trend.map((point) =>
    h(
      "div",
      { class: "chart-group" },
      h(
        "div",
        { class: "chart-bars" },
        h("span", {
          class: "chart-bar chart-entrada",
          style: {
            height: `${Math.max((point.entries / max) * 100, point.entries > 0 ? 4 : 0)}%`,
          },
          title: `${point.entries} entradas`,
          "aria-label": `${point.entries} entradas`,
        }),
        h("span", {
          class: "chart-bar chart-saida",
          style: {
            height: `${Math.max((point.exits / max) * 100, point.exits > 0 ? 4 : 0)}%`,
          },
          title: `${point.exits} saídas`,
          "aria-label": `${point.exits} saídas`,
        }),
      ),
      h(
        "span",
        { class: "chart-label", "aria-hidden": "true" },
        point.date.slice(8, 10) + "/" + point.date.slice(5, 7),
      ),
    ),
  )

  return h(
    "figure",
    { class: "chart-figure" },
    h(
      "div",
      { class: "chart-legend", "aria-hidden": "true" },
      h("span", { class: "legend-item legend-entry" }, "Entradas"),
      h("span", { class: "legend-item legend-exit" }, "Saídas"),
    ),
    h(
      "div",
      {
        class: "chart",
        role: "img",
        "aria-label": `Movimentações dos últimos 7 dias: ${totalEntries} entradas e ${totalExits} saídas.`,
      },
      bars,
    ),
    h(
      "figcaption",
      { class: "chart-caption" },
      `Últimos 7 dias: ${formatNumber(totalEntries)} entradas e ${formatNumber(totalExits)} saídas.`,
    ),
    h(
      "details",
      { class: "chart-data" },
      h("summary", { text: "Ver dados do gráfico em tabela" }),
      h(
        "table",
        { class: "simple-table" },
        h("caption", { class: "sr-only", text: "Movimentações por dia" }),
        h(
          "thead",
          {},
          h(
            "tr",
            {},
            h("th", { scope: "col" }, "Dia"),
            h("th", { scope: "col" }, "Entradas"),
            h("th", { scope: "col" }, "Saídas"),
          ),
        ),
        h(
          "tbody",
          {},
          trend.map((point) =>
            h(
              "tr",
              {},
              h("td", { text: point.date.split("-").reverse().join("/") }),
              h("td", { text: formatNumber(point.entries) }),
              h("td", { text: formatNumber(point.exits) }),
            ),
          ),
        ),
      ),
    ),
  )
}

function recentMovement(movement) {
  const type = movementType(movement.type)
  return h(
    "li",
    { class: "movement-item" },
    h(
      "span",
      { class: `movement-icon ${type.className}`, "aria-hidden": "true" },
      svgIcon(type.icon),
    ),
    h(
      "div",
      { class: "movement-copy" },
      h("strong", { text: movement.productName }),
      h("span", {
        text: `${type.label} · ${movement.userName} · ${formatDateTime(movement.createdAt)}`,
      }),
    ),
    h(
      "b",
      { class: `quantity ${type.className}` },
      `${type.sign}${formatNumber(movement.quantity)}`,
    ),
  )
}

function rankingList(items, { emptyMessage, quantityLabel = "un." }) {
  if (!items?.length) return h("p", { class: "muted-note", text: emptyMessage })
  return h(
    "ol",
    { class: "ranking-list" },
    items.map((item, index) =>
      h(
        "li",
        {},
        h("span", {
          class: "ranking-position",
          text: String(index + 1).padStart(2, "0"),
        }),
        h("span", { class: "ranking-name", text: item.name }),
        h("b", {
          text: `${formatNumber(item.totalQuantity ?? item.quantity)} ${quantityLabel}`,
        }),
      ),
    ),
  )
}

export async function render(container) {
  container.replaceChildren(loadingState("Carregando visão geral…"))
  let data
  try {
    data = await api.get("/dashboard", { query: { storeId: state.storeId } })
  } catch (error) {
    container.replaceChildren(
      errorState(error.message, { onRetry: () => render(container) }),
    )
    return
  }

  const stats = h(
    "section",
    {
      class: "stats-grid stats-grid-extended",
      "aria-label": "Indicadores do estoque",
    },
    statCard({
      label: "Produtos cadastrados",
      value: formatNumber(data.productsTotal),
      icon: "package",
      hint: `${formatNumber(data.totalUnits)} unidades em estoque`,
    }),
    statCard({
      label: "Estoque normal",
      value: formatNumber(data.normal),
      icon: "check",
      tone: "stat-normal",
    }),
    statCard({
      label: "Estoque baixo",
      value: formatNumber(data.low),
      icon: "down",
      tone: "stat-low",
    }),
    statCard({
      label: "Esgotados",
      value: formatNumber(data.outOfStock),
      icon: "close",
      tone: "stat-empty",
    }),
    statCard({
      label: "Movimentações",
      value: formatNumber(data.movementsTotal),
      icon: "movements",
    }),
    statCard({
      label: "Entradas",
      value: formatNumber(data.entries),
      icon: "up",
      tone: "stat-normal",
    }),
    statCard({
      label: "Saídas",
      value: formatNumber(data.exits),
      icon: "down",
      tone: "stat-low",
    }),
  )

  const chartPanel = panel(
    "chart-panel",
    h(
      "div",
      { class: "panel-heading" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Tendência" }),
        h("h2", { text: "Movimentações da semana" }),
      ),
    ),
    trendChart(data.trend ?? []),
  )

  const restockPanel = panel(
    "restock-panel",
    h(
      "div",
      { class: "panel-heading compact" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Reposição" }),
        h("h2", { text: "Precisa de atenção" }),
      ),
      h("span", { class: "count-pill", text: formatNumber(data.needsRestock) }),
    ),
    (data.lowStockProducts ?? []).length
      ? h(
          "ul",
          { class: "alert-list" },
          data.lowStockProducts.map((product) => {
            const status = productStatus(product.status)
            return h(
              "li",
              {},
              h(
                "div",
                {},
                h("strong", { text: product.name }),
                h("span", {
                  text: `${product.category} · mínimo ${product.minimum} ${product.unit}`,
                }),
              ),
              h("span", {
                class: `status ${status.className}`,
                text: status.label,
              }),
            )
          }),
        )
      : h("p", {
          class: "muted-note",
          text: "Nenhum produto com estoque baixo. Bom trabalho!",
        }),
    h(
      "div",
      { class: "panel-footer-actions" },
      h(
        "button",
        {
          type: "button",
          class: "button button-secondary button-small",
          onClick: () => navigate("estoque"),
        },
        "Ver estoque",
      ),
      data.outOfStock > 0
        ? h(
            "button",
            {
              type: "button",
              class: "button button-secondary button-small",
              onClick: () => navigate("estoque"),
            },
            `${formatNumber(data.outOfStock)} esgotados`,
          )
        : null,
    ),
  )

  const recentPanel = panel(
    "recent-panel",
    h(
      "div",
      { class: "panel-heading" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Auditoria" }),
        h("h2", { text: "Últimas movimentações" }),
      ),
      h(
        "button",
        {
          type: "button",
          class: "button button-secondary button-small",
          onClick: () => navigate("movimentacoes"),
        },
        "Histórico completo",
      ),
    ),
    (data.recentMovements ?? []).length
      ? h(
          "ul",
          { class: "movement-list" },
          data.recentMovements.map(recentMovement),
        )
      : h("p", {
          class: "muted-note",
          text: "As entradas e saídas aparecerão aqui.",
        }),
  )

  const topPanel = panel(
    "ranking-panel",
    h(
      "div",
      { class: "panel-heading compact" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Giro" }),
        h("h2", { text: "Produtos mais movimentados" }),
      ),
    ),
    rankingList(data.topMovedProducts ?? [], {
      emptyMessage: "Ainda não há movimentações suficientes.",
    }),
  )

  const lowTurnoverPanel = panel(
    "ranking-panel",
    h(
      "div",
      { class: "panel-heading compact" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Atenção" }),
        h("h2", { text: "Baixo giro (30 dias)" }),
      ),
    ),
    rankingList(
      (data.lowTurnoverProducts ?? []).map((product) => ({
        name: product.name,
        totalQuantity: product.quantity,
      })),
      { emptyMessage: "Sem dados de giro para o período." },
    ),
  )

  const salesPanel = panel(
    "sales-summary-panel",
    h(
      "div",
      { class: "panel-heading compact" },
      h(
        "div",
        {},
        h("p", { class: "section-kicker", text: "Comercial" }),
        h("h2", { text: "Vendas" }),
      ),
    ),
    h(
      "dl",
      { class: "summary-list" },
      h(
        "div",
        {},
        h("dt", { text: "Vendas registradas" }),
        h("dd", { text: formatNumber(data.salesCount) }),
      ),
      h(
        "div",
        {},
        h("dt", { text: "Receita total" }),
        h("dd", { text: formatCurrency(data.salesRevenue) }),
      ),
    ),
    h(
      "button",
      {
        type: "button",
        class: "button button-secondary button-small",
        onClick: () => navigate("vendas"),
      },
      "Abrir vendas",
    ),
  )

  container.replaceChildren(
    h(
      "div",
      { class: "view-stack" },
      stats,
      h("div", { class: "dashboard-top-grid" }, chartPanel, restockPanel),
      h(
        "div",
        { class: "dashboard-main-grid" },
        recentPanel,
        h(
          "div",
          { class: "side-column" },
          topPanel,
          lowTurnoverPanel,
          salesPanel,
        ),
      ),
      h("p", {
        class: "demo-note-inline",
        text: "Ambiente de demonstração: os dados iniciais podem ter sido gerados automaticamente pelo backend.",
      }),
    ),
  )
}
