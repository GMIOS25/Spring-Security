/**
 * Toast Notification System
 */
import { Icons } from "../icons.js";

class ToastManager {
  constructor() {
    this.container = null;
  }

  ensureContainer() {
    if (!this.container) {
      this.container = document.createElement("div");
      this.container.className = "toast-container";
      this.container.setAttribute("role", "region");
      this.container.setAttribute("aria-live", "polite");
      this.container.setAttribute("aria-label", "Notifications");
      document.body.appendChild(this.container);
    }
    return this.container;
  }

  show({ message, type = "info", durationMs = 3500 }) {
    const container = this.ensureContainer();

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.setAttribute("role", "status");

    let iconHtml = Icons.shield();
    if (type === "success") iconHtml = Icons.check();
    if (type === "danger") iconHtml = Icons.alertTriangle();
    if (type === "warning") iconHtml = Icons.alertTriangle();

    toast.innerHTML = `
      <div class="toast-icon">${iconHtml}</div>
      <div class="toast-message">${message}</div>
      <button class="toast-close" aria-label="Close notification">
        ${Icons.close("icon")}
      </button>
    `;

    const closeBtn = toast.querySelector(".toast-close");
    const removeToast = () => {
      toast.classList.add("toast-leaving");
      setTimeout(() => toast.remove(), 200);
    };

    closeBtn.addEventListener("click", removeToast);
    if (durationMs > 0) {
      setTimeout(removeToast, durationMs);
    }

    container.appendChild(toast);
  }

  success(msg, dur) { this.show({ message: msg, type: "success", durationMs: dur }); }
  danger(msg, dur) { this.show({ message: msg, type: "danger", durationMs: dur }); }
  warning(msg, dur) { this.show({ message: msg, type: "warning", durationMs: dur }); }
  info(msg, dur) { this.show({ message: msg, type: "info", durationMs: dur }); }
}

export const Toast = new ToastManager();
