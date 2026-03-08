<template>
  <div id="app">
    <NavBar />
    <!--
      <RouterView> is the slot where the active page component renders.
      Navigating between routes swaps the component here without reloading
      the page or re-rendering the NavBar.
    -->
    <main class="container">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import NavBar from "./components/NavBar.vue";
</script>

<!--
  Global styles live here (no "scoped" attribute) so they apply to all
  child components — table, button, badge patterns are defined once.
-->
<style>
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family:
    -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: #f0f4f8;
  color: #2d3748;
  line-height: 1.5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: #1e3a5f;
}
h2 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #2d3748;
}

/* ── Tables ─────────────────────────────────────────────── */
table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
thead {
  background: #2b4c7e;
  color: white;
}
th,
td {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.875rem;
}
tbody tr:nth-child(even) {
  background: #f7fafc;
}
tbody tr:hover {
  background: #ebf4ff;
}

/* ── Badges ─────────────────────────────────────────────── */
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.badge-pass {
  background: #c6f6d5;
  color: #276749;
}
.badge-fail {
  background: #fed7d7;
  color: #9b2c2c;
}
.badge-received {
  background: #bee3f8;
  color: #2a4a7f;
}

/* ── Buttons ─────────────────────────────────────────────── */
.btn {
  padding: 0.5rem 1.1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
  transition: background 0.15s;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-primary {
  background: #2b4c7e;
  color: white;
}
.btn-primary:hover:not(:disabled) {
  background: #1e3a5f;
}
.btn-secondary {
  background: #e2e8f0;
  color: #4a5568;
}
.btn-secondary:hover:not(:disabled) {
  background: #cbd5e0;
}

/* ── Feedback states ─────────────────────────────────────── */
.error {
  color: #c53030;
  padding: 1rem;
  background: #fff5f5;
  border-radius: 6px;
}
.loading {
  color: #718096;
  padding: 1rem;
}

a {
  color: #2b4c7e;
  text-decoration: none;
}
a:hover {
  text-decoration: underline;
}
</style>
