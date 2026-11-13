import { api, ApiError, ensureCsrf } from "../services/api.js"
import {
  clearFieldErrors,
  focusFirstError,
  setButtonLoading,
  setFieldError,
} from "../utils/dom.js"
import { validateLogin } from "../utils/validators.js"

/**
 * Tela de login. A sessão é mantida por cookie HttpOnly emitido pelo backend.
 */
export function initLogin({ onAuthenticated }) {
  const view = document.querySelector("#login-view")
  const form = document.querySelector("#login-form")
  const email = document.querySelector("#login-email")
  const password = document.querySelector("#login-password")
  const submit = document.querySelector("#login-submit")
  const globalError = document.querySelector("#login-error")

  function showError(message) {
    globalError.textContent = message
    globalError.hidden = false
  }

  function clearError() {
    globalError.textContent = ""
    globalError.hidden = true
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault()
    clearFieldErrors(form)
    clearError()

    const values = { email: email.value.trim(), password: password.value }
    const errors = validateLogin(values)
    if (Object.keys(errors).length > 0) {
      for (const [field, message] of Object.entries(errors))
        setFieldError(form, field, message)
      focusFirstError(form)
      return
    }

    setButtonLoading(submit, true, "Entrando…")
    try {
      await ensureCsrf()
      const session = await api.login(values)
      form.reset()
      onAuthenticated(session)
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        for (const [field, message] of Object.entries(error.fieldErrors))
          setFieldError(form, field, message)
        focusFirstError(form)
      }
      showError(
        error?.status === 401
          ? "E-mail ou senha inválidos. Confira os dados e tente novamente."
          : error?.message || "Não foi possível entrar. Tente novamente.",
      )
      password.focus()
    } finally {
      setButtonLoading(submit, false)
    }
  })

  email.addEventListener("input", clearError)

  return {
    show() {
      view.hidden = false
      document.querySelector("#app-shell").hidden = true
      document.querySelector("#boot-screen").hidden = true
      window.setTimeout(() => email.focus(), 50)
    },
    hide() {
      view.hidden = true
      clearError()
    },
  }
}
