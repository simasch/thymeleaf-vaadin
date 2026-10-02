// Registers the Vaadin web components used by the Thymeleaf templates.
// Thymeleaf renders plain <vaadin-*> tags on the server; this bundle upgrades them in the browser.
import '@vaadin/vaadin-lumo-styles/lumo.css';
import '@vaadin/app-layout';
import '@vaadin/app-layout/vaadin-drawer-toggle.js';
import '@vaadin/button';
import '@vaadin/email-field';
import '@vaadin/form-layout';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/horizontal-layout';
import '@vaadin/side-nav';
import '@vaadin/text-field';
import { Notification } from '@vaadin/notification';
import './styles.css';

// Vaadin field components put a real <input> into their light DOM and forward the
// `name` attribute to it, so they take part in a normal HTML form POST.
// <vaadin-button> is not a native submit button though, so submit buttons and
// the Enter key are wired up here.
document.addEventListener('click', (event) => {
  const button = event.target.closest('vaadin-button[type="submit"]');
  const form = button?.closest('form');
  if (form && !button.disabled) {
    form.requestSubmit();
  }
});

document.addEventListener('keydown', (event) => {
  if (event.key !== 'Enter' || event.target.localName !== 'input') {
    return;
  }
  const form = event.target.closest('form');
  if (form?.querySelector('vaadin-button[type="submit"]')) {
    event.preventDefault();
    form.requestSubmit();
  }
});

// Flash messages rendered by Thymeleaf as <template data-notification="...">
// are shown as Vaadin notifications.
document.querySelectorAll('[data-notification]').forEach((element) => {
  Notification.show(element.dataset.notification, {
    position: 'bottom-end',
    theme: element.dataset.theme ?? 'success',
    duration: 3000,
  });
});

// Exposed for page-specific inline scripts (e.g. grid renderers).
window.vaadin = { Notification };
