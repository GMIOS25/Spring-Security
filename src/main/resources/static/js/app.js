/**
 * InstaClone Application Bootstrap & Route Coordinator
 */
import { State } from "./state.js";
import { api } from "./api.js";
import { Toast } from "./components/toast.js";
import { initGlobalMediaFallbacks } from "./avatar-helper.js";
import { renderNavbar } from "./components/navbar.js";
import { renderStories, closeStoryViewer } from "./components/stories.js";
import { renderFeed } from "./components/feed.js";
import { renderExplore } from "./components/explore.js";
import { renderProfile } from "./components/profile.js";
import { renderModeration } from "./components/moderation.js";
import { renderCreateModal } from "./components/create-modal.js";
import { renderAuthModal } from "./components/auth-modal.js";
import { renderSecurityHud } from "./components/security-hud.js";

async function init() {
  // 0. Initialize CSP-compliant media error fallbacks
  initGlobalMediaFallbacks();

  // 1. Initialize Theme
  document.documentElement.setAttribute("data-theme", State.theme);

  // 2. Fetch initial CSRF Token
  await api.ensureCsrfToken();

  // 3. Attempt to authenticate or auto-login with Alice for immediate rich demo
  let user = await api.getCurrentUser();
  if (!user) {
    try {
      await api.loginSession("alice", "Password123!");
      user = State.user;
    } catch {
      // Fallback to anonymous explore
    }
  }

  // 4. Load initial feed posts
  try {
    await api.getFeedPosts();
  } catch (err) {
    console.warn("Feed fetch error:", err);
  }

  // 5. Mount Components
  mountAllComponents();

  // 6. Setup State Change Subscriptions
  State.subscribe((event, data) => {
    if (event === "tab:change") {
      updateActiveTabDisplay(data);
    } else if (event === "user:change" || event === "posts:change" || event === "authMode:change") {
      mountAllComponents();
    } else if (event === "telemetry:change") {
      updateTelemetryUI();
    }
  });

  // 7. Global Keyboard Shortcuts
  window.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeStoryViewer();
      document.querySelectorAll(".modal-backdrop.open, .security-drawer-backdrop.open").forEach(el => {
        el.classList.remove("open");
        el.setAttribute("aria-hidden", "true");
      });
    }
    // Press 'c' to create post if not focused in an input/textarea
    if (e.key.toLowerCase() === "c" && !["INPUT", "TEXTAREA"].includes(document.activeElement?.tagName)) {
      const backdrop = document.querySelector("#createModalBackdrop");
      if (backdrop) {
        backdrop.classList.add("open");
        backdrop.setAttribute("aria-hidden", "false");
        document.querySelector("#postCaptionInput")?.focus();
      }
    }
  });

  Toast.success("InstaClone loaded • Spring Security 7.x active");
}

function mountAllComponents() {
  const navbarMount = document.querySelector("#navbarMount");
  const storiesMount = document.querySelector("#storiesMount");
  const feedMount = document.querySelector("#feedMount");
  const exploreMount = document.querySelector("#exploreMount");
  const profileMount = document.querySelector("#profileMount");
  const moderationMount = document.querySelector("#moderationMount");
  const createModalMount = document.querySelector("#createModalMount");
  const authModalMount = document.querySelector("#authModalMount");
  const securityHudMount = document.querySelector("#securityHudMount");

  if (navbarMount) renderNavbar(navbarMount);
  if (storiesMount) renderStories(storiesMount);
  if (feedMount) renderFeed(feedMount);
  if (exploreMount) renderExplore(exploreMount);
  if (profileMount) renderProfile(profileMount);
  if (moderationMount) renderModeration(moderationMount);
  if (createModalMount) renderCreateModal(createModalMount);
  if (authModalMount) renderAuthModal(authModalMount);
  if (securityHudMount) renderSecurityHud(securityHudMount);

  updateActiveTabDisplay(State.activeTab);
}

function updateActiveTabDisplay(tab) {
  const feedSection = document.querySelector("#feedSection");
  const exploreSection = document.querySelector("#exploreSection");
  const profileSection = document.querySelector("#profileSection");
  const moderationSection = document.querySelector("#moderationSection");

  if (feedSection) feedSection.classList.toggle("hidden", tab !== "feed");
  if (exploreSection) exploreSection.classList.toggle("hidden", tab !== "explore");
  if (profileSection) profileSection.classList.toggle("hidden", tab !== "profile");
  if (moderationSection) moderationSection.classList.toggle("hidden", tab !== "moderation");

  // Re-render sub-views on navigation if needed
  if (tab === "moderation") {
    const modMount = document.querySelector("#moderationMount");
    if (modMount) renderModeration(modMount);
  } else if (tab === "profile") {
    const profMount = document.querySelector("#profileMount");
    if (profMount) renderProfile(profMount);
  }

  // Update navbar active states
  document.querySelectorAll("[data-tab]").forEach(el => {
    el.classList.toggle("active", el.getAttribute("data-tab") === tab);
  });

  window.scrollTo({ top: 0, behavior: "smooth" });
}

function updateTelemetryUI() {
  const trigger = document.querySelector("#floatingHudTrigger");
  if (trigger) {
    const activeChain = State.telemetry.lastActiveChain || 3;
    const badge = trigger.querySelector("span:last-child");
    if (badge) badge.textContent = `Chain ${activeChain}`;
  }
}

// Start application when DOM is ready
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
