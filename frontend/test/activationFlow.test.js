import assert from "node:assert/strict";
import test from "node:test";

import { clearFlowOpsAuthStorage } from "../src/context/authStorage.js";
import { activateAccount } from "../src/pages/activationFlow.js";

function storageWith(initialEntries) {
  const entries = new Map(Object.entries(initialEntries));
  return {
    getItem: (key) => entries.get(key) ?? null,
    removeItem: (key) => entries.delete(key),
  };
}

test("successful activation clears only FlowOps authentication state", async () => {
  const storage = storageWith({ token: "bob-token", user: "legacy-user", theme: "dark" });

  await activateAccount({
    token: "activation-token",
    password: "password1",
    request: async (_url, options) => {
      assert.deepEqual(options.headers, { "Content-Type": "application/json" });
      assert.equal("Authorization" in options.headers, false);
      return { ok: true };
    },
    onSuccess: () => clearFlowOpsAuthStorage(storage),
  });

  assert.equal(storage.getItem("token"), null);
  assert.equal(storage.getItem("user"), null);
  assert.equal(storage.getItem("theme"), "dark");
});

test("failed or already-used activation leaves the existing session intact", async () => {
  const storage = storageWith({ token: "bob-token", user: "legacy-user" });

  await assert.rejects(
    activateAccount({
      token: "invalid-token",
      password: "password1",
      request: async () => ({ ok: false, json: async () => ({ detail: "Activation token is invalid or already used." }) }),
      onSuccess: () => clearFlowOpsAuthStorage(storage),
    }),
    /invalid or already used/,
  );

  assert.equal(storage.getItem("token"), "bob-token");
  assert.equal(storage.getItem("user"), "legacy-user");
});
