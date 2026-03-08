<template>
  <div>
    <RouterLink to="/claims" class="back-link">← Back to Claims</RouterLink>

    <div v-if="loading" class="loading">Loading claim…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <template v-else>
      <h1>Claim: {{ claim.claimId }}</h1>

      <!-- ── Claim detail card ───────────────────────────── -->
      <div class="detail-card">
        <div class="detail-grid">
          <div>
            <span class="label">Patient ID</span>
            <span>{{ claim.patientId }}</span>
          </div>
          <div>
            <span class="label">Provider ID</span>
            <span>{{ claim.providerId }}</span>
          </div>
          <div>
            <span class="label">Payer ID</span>
            <span>{{ claim.payerId }}</span>
          </div>
          <div>
            <span class="label">Service Date</span>
            <span>{{ fmtDate(claim.serviceDate) }}</span>
          </div>
          <div>
            <span class="label">Procedure Code</span>
            <span>{{ claim.procedureCode }}</span>
          </div>
          <div>
            <span class="label">Diagnosis Code</span>
            <span>{{ claim.diagnosisCode }}</span>
          </div>
          <div>
            <span class="label">Billed Amount</span>
            <span>{{ fmt(claim.billedAmount) }}</span>
          </div>
          <div>
            <span class="label">Status</span>
            <span class="badge badge-received">{{ claim.claimStatus }}</span>
          </div>
          <div>
            <span class="label">EDI Type</span>
            <span>{{ claim.ediType }}</span>
          </div>
        </div>
      </div>

      <!-- ── Validation results ─────────────────────────── -->
      <div class="section-header">
        <h2>Validation Results</h2>
        <button
          class="btn btn-primary"
          :disabled="validating"
          @click="runValidate"
        >
          {{ validating ? "Running…" : "Re-validate" }}
        </button>
      </div>

      <div v-if="loadingValidations" class="loading">Loading validations…</div>

      <table v-else-if="validations.length">
        <thead>
          <tr>
            <th>Rule</th>
            <th>Status</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in validations" :key="v.validationType">
            <td>{{ v.validationType }}</td>
            <td>
              <span
                :class="[
                  'badge',
                  v.status === 'PASS' ? 'badge-pass' : 'badge-fail',
                ]"
              >
                {{ v.status }}
              </span>
            </td>
            <td>{{ v.message }}</td>
          </tr>
        </tbody>
      </table>

      <p v-else style="color: #718096; margin-top: 0.5rem">
        No validation results yet — click <strong>Re-validate</strong> to run.
      </p>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getClaim, getValidations, validateClaim } from "../api/index.js";

// useRoute() gives access to the current route, including :claimId param
const route = useRoute();
const claimId = route.params.claimId;

const claim = ref(null);
const validations = ref([]);
const loading = ref(true);
const loadingValidations = ref(false);
const error = ref(null);
const validating = ref(false);

const fmt = (n) =>
  n != null
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(n)
    : "—";

const fmtDate = (v) => {
  if (!v) return "—";
  if (Array.isArray(v))
    return `${v[0]}-${String(v[1]).padStart(2, "0")}-${String(v[2]).padStart(2, "0")}`;
  return String(v);
};

async function loadValidations() {
  loadingValidations.value = true;
  try {
    validations.value = await getValidations(claimId);
  } catch {
    validations.value = [];
  } finally {
    loadingValidations.value = false;
  }
}

async function runValidate() {
  validating.value = true;
  try {
    validations.value = await validateClaim(claimId);
  } catch (e) {
    console.error("Validate failed:", e);
  } finally {
    validating.value = false;
  }
}

onMounted(async () => {
  try {
    claim.value = await getClaim(claimId);
    await loadValidations();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 1.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.detail-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 2rem;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
}
.detail-grid > div {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.label {
  font-size: 0.73rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #718096;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.section-header h2 {
  margin: 0;
}
</style>
