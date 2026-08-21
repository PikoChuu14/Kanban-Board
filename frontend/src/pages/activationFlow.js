export async function activateAccount({ token, password, onSuccess, request = fetch }) {
  const response = await request("/api/auth/activate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, password }),
  });

  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.detail || data.message || "Activation failed. The link may be expired or already used.");
  }

  onSuccess();
}
