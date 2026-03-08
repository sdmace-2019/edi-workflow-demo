<template>
  <div>
    <h1>Dashboard</h1>

    <div v-if="loading" class="loading">Loading…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <template v-else>
      <!-- ── Summary stat cards ────────────────────────────── -->
      <div class="cards">
        <div class="card">
          <div class="card-label">Total Claims</div>
          <div class="card-value">{{ data.claims.total }}</div>
        </div>
        <div class="card">
          <div class="card-label">Total Billed</div>
          <div class="card-value">{{ fmt(data.claims.totalBilled) }}</div>
        </div>
        <div class="card">
          <div class="card-label">Total Payments</div>
          <div class="card-value">{{ data.payments.total }}</div>
        </div>
        <div class="card">
          <div class="card-label">Total Paid</div>
          <div class="card-value">{{ fmt(data.payments.totalPaid) }}</div>
        </div>
        <div class="card">
          <div class="card-label">Total Adjustment</div>
          <div class="card-value">{{ fmt(data.payments.totalAdj) }}</div>
        </div>
        <div class="card">
          <div class="card-label">Validation Results</div>
          <div class="card-value">
            <span class="badge badge-pass"
              >{{ data.validation.passCount }} Pass</span
            >
            &nbsp;
            <span class="badge badge-fail"
              >{{ data.validation.failCount }} Fail</span
            >
          </div>
        </div>
      </div>

      <!-- ── EDI Intake section ────────────────────────────── -->
      <div class="panel">
        <h2>EDI Intake</h2>
        <p class="hint">
          Scans <code>edi-samples/</code>, parses any 837 and 835 files found,
          and saves new claims and payments to the database. Duplicate records
          are silently skipped.
        </p>
        <button
          class="btn btn-primary"
          :disabled="ingesting"
          @click="runIngest"
        >
          {{ ingesting ? "Processing…" : "Load Sample EDI" }}
        </button>

        <!-- Ingest result table -->
        <div v-if="ingestResult" class="ingest-result">
          <p>
            Files processed:
            <strong>{{ ingestResult.filesProcessed }}</strong> &nbsp;|&nbsp;
            Claims saved:
            <strong>{{ ingestResult.totalClaimsSaved }}</strong> &nbsp;|&nbsp;
            Payments saved:
            <strong>{{ ingestResult.totalPaymentsSaved }}</strong>
          </p>
          <table style="margin-top: 1rem">
            <thead>
              <tr>
                <th>File</th>
                <th>Type</th>
                <th>Claims ✓</th>
                <th>Claims skipped</th>
                <th>Payments ✓</th>
                <th>Payments skipped</th>
                <th>Note</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in ingestResult.results" :key="r.fileName">
                <td>{{ r.fileName }}</td>
                <td>{{ r.transactionSet }}</td>
                <td>{{ r.claimsSaved }}</td>
                <td>{{ r.claimsSkipped }}</td>
                <td>{{ r.paymentsSaved }}</td>
                <td>{{ r.paymentsSkipped }}</td>
                <td>{{ r.message }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="ingestError" class="error" style="margin-top: 0.75rem">
          {{ ingestError }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getDashboard, triggerIngest } from "../api/index.js";

const data = ref(null);
const loading = ref(true);
const error = ref(null);
const ingesting = ref(false);
const ingestResult = ref(null);
const ingestError = ref(null);

const fmt = (n) =>
  n != null
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(n)
    : "—";

onMounted(async () => {
  try {
    data.value = await getDashboard();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});

async function runIngest() {
  ingesting.value = true;
  ingestResult.value = null;
  ingestError.value = null;
  try {
    ingestResult.value = await triggerIngest();
    // Refresh the stat cards after ingest so counts update immediately
    data.value = await getDashboard();
  } catch (e) {
    ingestError.value = e.message;
  } finally {
    ingesting.value = false;
  }
}
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}
.card {
  background: white;
  border-radius: 8px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.card-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #718096;
  margin-bottom: 0.5rem;
}
.card-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: #1e3a5f;
}

.panel {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.hint {
  font-size: 0.875rem;
  color: #718096;
  margin-bottom: 1rem;
}
.hint code {
  background: #edf2f7;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 0.85rem;
}
.ingest-result {
  margin-top: 1rem;
}
.ingest-result p {
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}
</style>
