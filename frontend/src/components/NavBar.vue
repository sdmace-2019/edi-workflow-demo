<template>
  <nav class="navbar">
    <span class="brand">&#x2695;&#xFE0F; EDI Workflow Demo</span>
    <div class="links">
      <!--
        router-link-active is added automatically by Vue Router when the
        link's path matches the current URL — used below for the underline style.
      -->
      <RouterLink to="/">Dashboard</RouterLink>
      <RouterLink to="/claims">Claims</RouterLink>
      <RouterLink to="/payments">Payments</RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1e3a5f;
  color: white;
  padding: 0.85rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
.brand {
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.01em;
}
.links {
  display: flex;
  gap: 2rem;
}
.links a {
  color: #a0b8d8;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  padding-bottom: 3px;
  border-bottom: 2px solid transparent;
  transition:
    color 0.15s,
    border-color 0.15s;
}
.links a.router-link-active,
.links a.router-link-exact-active {
  color: white;
  border-bottom-color: #63b3ed;
}
</style>
