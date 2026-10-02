# Thymeleaf + Vaadin web components

A Spring Boot 4 / Spring MVC app that renders pages with **Thymeleaf** and uses
**Vaadin web components** (`<vaadin-grid>`, `<vaadin-text-field>`, `<vaadin-app-layout>`, …)
as plain HTML tags. No Vaadin Flow on the server: it's ordinary controllers, models, and templates.

## How it works

- `src/main/frontend/main.js` imports the Vaadin components and the Lumo theme from npm.
  The Maven build (`frontend-maven-plugin`) installs Node and runs Vite, which bundles them into
  `target/classes/static/assets/main.{js,css}`. Every page loads that bundle in `layout.html`.
- **Data in:** Thymeleaf JavaScript inlining (`/*[[${contacts}]]*/`) turns the model into JSON
  for `grid.items` (`contacts/list.html`).
- **Forms:** Vaadin fields forward `name` to a real `<input>` in their light DOM, so a normal
  `<form method="post">` submits them and Spring MVC binds them. Bean Validation errors go back
  onto the components through `th:invalid` / `th:error-message` (`contacts/form.html`).
  `<vaadin-button type="submit">` and the Enter key are wired to `form.requestSubmit()` in `main.js`.
- Flash messages show up as Vaadin notifications.

## Run

```bash
mvn spring-boot:run          # http://localhost:8080
```

While working on the frontend, run `npm run watch` next to it. Vite then rebuilds the bundle into
`target/classes` whenever you save.

## Caveats

- Only the components are open-source web components. Server-side Java APIs (Binder, Flow
  components, routing) aren't available here.
- Fields whose shown value differs from the value you want to submit (date picker, combo box,
  select, multi-select) don't post the right thing by themselves. Add a hidden input and sync
  it from the component's `value-changed` event.
