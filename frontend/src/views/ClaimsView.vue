<template>
  <div>
    <div class="page-header">
      <h1>Claims</h1>
      <button
        class="btn btn-secondary"
        :disabled="validating"
        @click="runValidateAll"
      >
        {{ validating ? "Running…" : "Validate All" }}
      </button>
    </div>

    <div v-if="validateMsg" class="validate-msg">{{ validateMsg }}</div>

    <div v-if="loading" class="loading">Loading claims…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <template v-else>
      <table>
        <thead>
          <tr>
            <th>Claim ID</th>
            <th>Patient ID</th>
            <th>Provider ID</th>
            <th>Payer ID</th>
            <th>Service Date</th>
            <th>Procedure</th>
            <th>Billed</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in claims" :key="c.claimId">
            <td>
              <!--
                RouterLink generates an <a> tag that uses the History API
                instead of a full page reload. The claimId is passed as a
                route param to ClaimDetailView.
              -->
              <RouterLink :to="'/claims/' + c.claimId">{{
                c.claimId
              }}</RouterLink>
            </td>
            <td>{{ c.patientId }}</td>
            <td>{{ c.providerId }}</td>
            <td>{{ c.payerId }}</td>
            <td>{{ fmtDate(c.serviceDate) }}</td>
            <td>{{ c.procedureCode }}</td>
            <td>{{ fmt(c.billedAmount) }}</td>
            <td>
              <span class="badge badge-received">{{ c.claimStatus }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination controls — only rendered when multiple pages exist -->
      <div v-if="totalPages > 1" class="pagination">
        <button
          class="btn btn-secondary"
          :disabled="page === 0"
          @click="changePage(page - 1)"
        >
          ‹ Prev
        </button>
        <span>Page {{ page + 1 }} of {{ totalPages }}</span>
        <button
          class="btn btn-secondary"
          :disabled="page >= totalPages - 1"
          @click="changePage(page + 1)"
        >
          Next ›
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getClaims, validateAll } from "../api/index.js";

const claims = ref([]);
const loading = ref(true);
const error = ref(null);
const page = ref(0);
const totalPages = ref(1);
const validating = ref(false);
const validateMsg = ref(null);

// Currency formatter
const fmt = (n) =>
  n != null
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(n)
    : "—";

// Date formatter — Spring serializes LocalDate as "2026-01-15" (ISO string)
// when write-dates-as-timestamps=false is set. Array format handled as fallback.
const fmtDate = (v) => {
  if (!v) return "—";
  if (Array.isArray(v))
    return `${v[0]}-${String(v[1]).padStart(2, "0")}-${String(v[2]).padStart(2, "0")}`;
  return String(v);
};

async function load() {
  loading.value = true;
  error.value = null;
  try {
    // getClaims() returns a Spring Page object:
    // { content: Claim[], totalPages, totalElements, number, size, ... }
    const res = await getClaims(page.value);
    claims.value = res.content;
    totalPages.value = res.totalPages;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

function changePage(p) {
  page.value = p;
  load();
}

async function runValidateAll() {
  validating.value = true;
  validateMsg.value = null;
  try {
    const res = await validateAll();
    validateMsg.value = `Validation complete — ${res.logsWritten} logs written`;
  } catch (e) {
    validateMsg.value = "Validation failed: " + e.message;
  } finally {
    validating.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.page-header h1 {
  margin: 0;
}

.validate-msg {
  padding: 0.75rem 1rem;
  background: #c6f6d5;
  color: #276749;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  font-size: 0.9rem;
}
</style>
