/**
 * Camada única de comunicação com a API Java (Spring Boot).
 *
 * - Sessão via cookie HttpOnly (credentials: "include").
 * - CSRF por cookie/header (XSRF-TOKEN / X-XSRF-TOKEN), conforme Spring Security.
 * - Erros padronizados em ApiError (status, mensagem, fieldErrors).
 */

const baseUrl = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1"
).replace(/\/+$/, "")

export class ApiError extends Error {
  constructor(
    message,
    { status = 0, error = "Erro", fieldErrors = null } = {},
  ) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.error = error
    this.fieldErrors = fieldErrors ?? null
  }
}

let csrf = { headerName: "X-XSRF-TOKEN", token: null }
let csrfRequest = null

function buildUrl(path, query) {
  const url = new URL(`${baseUrl}${path.startsWith("/") ? path : `/${path}`}`)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined || value === "") continue
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function readBody(response) {
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

/**
 * Busca (ou reaproveita) o token CSRF da sessão atual.
 */
export async function ensureCsrf(force = false) {
  if (csrf.token && !force) return csrf
  if (csrfRequest) return csrfRequest

  csrfRequest = (async () => {
    const response = await fetch(buildUrl("/auth/csrf"), {
      credentials: "include",
      headers: { Accept: "application/json" },
    })
    if (!response.ok) {
      throw new ApiError("Não foi possível iniciar a sessão segura.", {
        status: response.status,
      })
    }
    const data = await response.json()
    csrf = { headerName: data.headerName, token: data.token }
    return csrf
  })()

  try {
    return await csrfRequest
  } finally {
    csrfRequest = null
  }
}

export function resetCsrf() {
  csrf = { headerName: "X-XSRF-TOKEN", token: null }
}

export async function request(
  path,
  { method = "GET", body, query, signal, headers = {} } = {},
) {
  const upperMethod = method.toUpperCase()
  const requestHeaders = { Accept: "application/json", ...headers }

  if (body !== undefined) {
    requestHeaders["Content-Type"] = "application/json"
  }
  if (!["GET", "HEAD", "OPTIONS"].includes(upperMethod)) {
    const token = await ensureCsrf()
    if (token.token) requestHeaders[token.headerName] = token.token
  }

  let response
  try {
    response = await fetch(buildUrl(path, query), {
      method: upperMethod,
      headers: requestHeaders,
      credentials: "include",
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (cause) {
    if (cause?.name === "AbortError") throw cause
    throw new ApiError(
      "Falha de comunicação com o servidor. Verifique sua conexão e tente novamente.",
      {
        status: 0,
        error: "Sem conexão",
      },
    )
  }

  const data = await readBody(response)

  if (!response.ok) {
    if (response.status === 401 && !path.startsWith("/auth/")) {
      window.dispatchEvent(new CustomEvent("gestao:unauthorized"))
    }
    throw new ApiError(
      data?.message || "Não foi possível concluir a operação.",
      {
        status: response.status,
        error: data?.error || "Erro",
        fieldErrors: data?.fieldErrors ?? null,
      },
    )
  }

  return data
}

export const api = {
  get: (path, options) => request(path, { ...options, method: "GET" }),
  post: (path, body, options) =>
    request(path, { ...options, method: "POST", body }),
  put: (path, body, options) =>
    request(path, { ...options, method: "PUT", body }),
  delete: (path, options) => request(path, { ...options, method: "DELETE" }),

  csrf: () => ensureCsrf(true),

  login: (credentials) =>
    request("/auth/login", { method: "POST", body: credentials }),
  logout: () => request("/auth/logout", { method: "POST" }),
  me: () => request("/auth/me"),
}

export { baseUrl }
