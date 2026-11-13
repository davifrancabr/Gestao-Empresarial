/**
 * Estado de sessão/loja compartilhado entre os módulos (sem persistência de
 * dados de negócio). Apenas a loja ativa é guardada como preferência de UI.
 */

const STORE_PREFERENCE_KEY = "gestao-active-store"

export const state = {
  session: null,
  storeId: null,
  route: "dashboard",
}

export function role() {
  return state.session?.user?.role ?? "OPERADOR"
}

export function isAdmin() {
  return role() === "ADMIN"
}

export function canManage() {
  return role() === "ADMIN" || role() === "GERENTE"
}

export function canAdjust() {
  return canManage()
}

export function canManageUsers() {
  return isAdmin()
}

export function canManageStores() {
  return isAdmin()
}

export function storeName(storeId = state.storeId) {
  return (
    state.session?.stores?.find((store) => store.id === storeId)?.name ?? "Loja"
  )
}

export function displayRole(value = role()) {
  return (
    { ADMIN: "Administrador", GERENTE: "Gerente", OPERADOR: "Operador" }[
      value
    ] ?? value
  )
}

export function readStoredStore() {
  try {
    return window.localStorage.getItem(STORE_PREFERENCE_KEY)
  } catch {
    return null
  }
}

export function persistStore(storeId) {
  try {
    if (storeId) window.localStorage.setItem(STORE_PREFERENCE_KEY, storeId)
    else window.localStorage.removeItem(STORE_PREFERENCE_KEY)
  } catch {
    // Preferência de UI é opcional; falha de armazenamento não bloqueia o uso.
  }
}

export function navigate(route) {
  const target = `#/${route}`
  if (window.location.hash === target) {
    window.dispatchEvent(new HashChangeEvent("hashchange"))
  } else {
    window.location.hash = target
  }
}

export function clearSession() {
  state.session = null
  state.storeId = null
  state.route = "dashboard"
}
