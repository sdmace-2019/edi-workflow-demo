import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // Any request to /api/* is forwarded to the Spring Boot backend.
      // This avoids CORS browser restrictions during development and keeps
      // the frontend code free of hardcoded backend URLs.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
