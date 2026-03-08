import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";

// createApp() instantiates the Vue application.
// .use(router) registers Vue Router so <RouterLink> and <RouterView> work everywhere.
// .mount('#app') attaches the app to the <div id="app"> in index.html.
createApp(App).use(router).mount("#app");
