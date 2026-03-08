// Central API client — all fetch calls live here so the base URL is
// defined in exactly one place. The Vite dev proxy rewrites /api/* to
// http://localhost:8080/api/*, so no hardcoded backend host is needed.

const BASE = "/api";

async function apiFetch(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) throw new Error(`API error ${res.status} on ${path}`);
  return res.json();
}

// Dashboard
export const getDashboard = () => apiFetch("/dashboard");

// Claims — getClaims() returns a Spring Page (content[], totalPages, number, …)
export const getClaims = (page = 0, size = 20) =>
  apiFetch(`/claims?page=${page}&size=${size}`);
export const getClaim = (claimId) =>
  apiFetch(`/claims/${encodeURIComponent(claimId)}`);
export const getValidations = (claimId) =>
  apiFetch(`/claims/${encodeURIComponent(claimId)}/validations`);
export const validateClaim = (claimId) =>
  apiFetch(`/claims/${encodeURIComponent(claimId)}/validate`, {
    method: "POST",
  });
export const validateAll = () =>
  apiFetch("/claims/validate-all", { method: "POST" });

// Payments
export const getPayments = () => apiFetch("/payments");

// Intake
export const triggerIngest = () =>
  apiFetch("/intake/samples/ingest", { method: "POST" });
