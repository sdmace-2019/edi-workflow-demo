import { createRouter, createWebHistory } from "vue-router";
import DashboardView from "../views/DashboardView.vue";
import ClaimsView from "../views/ClaimsView.vue";
import ClaimDetailView from "../views/ClaimDetailView.vue";
import PaymentsView from "../views/PaymentsView.vue";

// Each route maps a URL path to a View component.
// createWebHistory() uses the HTML5 History API (clean URLs, no #hash).
const routes = [
  { path: "/", name: "Dashboard", component: DashboardView },
  { path: "/claims", name: "Claims", component: ClaimsView },
  { path: "/claims/:claimId", name: "ClaimDetail", component: ClaimDetailView },
  { path: "/payments", name: "Payments", component: PaymentsView },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
