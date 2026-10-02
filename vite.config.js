import { defineConfig } from 'vite';

// Bundles src/main/frontend/main.js (all Vaadin web components + Lumo theme)
// into target/classes/static/assets so Spring Boot serves it as /assets/main.js.
// File names are kept stable (no hashes) so the Thymeleaf templates can reference them directly.
export default defineConfig({
  publicDir: false,
  build: {
    outDir: 'target/classes/static/assets',
    emptyOutDir: true,
    rollupOptions: {
      input: 'src/main/frontend/main.js',
      output: {
        entryFileNames: '[name].js',
        chunkFileNames: '[name].js',
        assetFileNames: '[name][extname]',
      },
    },
  },
});
