<template>
  <div>
    <h1>Payments</h1>

    <div v-if="loading" class="loading">Loading payments…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <table v-else>
      <thead>
        <tr>
          <th>Payment ID</th>
          <th>Claim ID</th>
          <th>Provider ID</th>
          <th>Payer ID</th>
          <th>Payment Date</th>
          <th>Paid</th>
          <th>Adjustment</th>
          <th>PLB Adj</th>
          <th>Method</th>
          <th>ERA Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in payments" :key="p.paymentId">
          <td>{{ p.paymentId }}</td>
          <td>
            <!--
              If we have a claimId, link to the claim detail page.
              Some payment records may not map to a parsed claim (e.g. PLB rows).
            -->
            <RouterLink v-if="p.claimId" :to="'/claims/' + p.claimId">
              {{ p.claimId }}
            </RouterLink>
            <span v-else>—</span>
          </td>
          <td>{{ p.providerId }}</td>
          <td>{{ p.payerId }}</td>
          <td>{{ fmtDate(p.paymentDate) }}</td>
          <td>{{ fmt(p.paymentAmount) }}</td>
          <td>{{ fmt(p.adjAmount) }}</td>
          <td>{{ fmt(p.plbAmount) }}</td>
          <td>{{ p.paymentMethod }}</td>
          <td>
            <span class="badge badge-received">{{
              p.eraStatus || "PROCESSED"
            }}</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getPayments } from "../api/index.js";

const payments = ref([]);
const loading = ref(true);
const error = ref(null);

const fmt = (n) =>
  n != null
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(n)
    : "—";

// LocalDate arrives as "2026-01-01" (ISO string) due to write-dates-as-timestamps=false.
// Array fallback retained for safety.
const fmtDate = (v) => {
  if (!v) return "—";
  if (Array.isArray(v))
    return `${v[0]}-${String(v[1]).padStart(2, "0")}-${String(v[2]).padStart(2, "0")}`;
  return String(v);
};

onMounted(async () => {
  try {
    payments.value = await getPayments();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>
