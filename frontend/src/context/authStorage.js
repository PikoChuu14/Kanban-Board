export const FLOWOPS_AUTH_STORAGE_KEYS = ["token", "user"];

export function clearFlowOpsAuthStorage(storage = window.localStorage) {
  FLOWOPS_AUTH_STORAGE_KEYS.forEach((key) => storage.removeItem(key));
}
