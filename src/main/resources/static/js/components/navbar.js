/**
 * Navigation Bar Component (Desktop Sidebar & Mobile Nav)
 */
import { State } from "../state.js";
import { Icons } from "../icons.js";
import { getAvatarUrl } from "../avatar-helper.js";

export function renderNavbar(container) {
  const isStaff = State.user && State.user.roles && (
    State.user.roles.includes("ROLE_ADMIN") ||
    State.user.roles.includes("ROLE_MODERATOR") ||
    State.user.roles.includes("POST_DELETE_ANY")
  );

  const username = State.user ? State.user.username : "Guest (Explore)";
  const avatarUrl = State.user
    ? getAvatarUrl(State.user.username)
    : "/images/avatars/default.svg";

  const themeIcon = State.theme === "dark" ? Icons.sun() : Icons.moon();

  container.innerHTML = `
    <!-- Desktop Sidebar -->
    <aside class="sidebar" aria-label="Main Navigation">
      <div class="sidebar-brand">
        <span class="brand-logo-icon">${Icons.logo("icon")}</span>
        <span class="brand-logo-text">InstaClone</span>
        <span class="brand-badge">Sec 7.x</span>
      </div>

      <nav class="sidebar-nav">
        <button class="nav-item ${State.activeTab === "feed" ? "active" : ""}" data-tab="feed" aria-label="Home Feed">
          ${Icons.home()}
          <span>Home</span>
        </button>

        <button class="nav-item ${State.activeTab === "explore" ? "active" : ""}" data-tab="explore" aria-label="Explore Trending">
          ${Icons.explore()}
          <span>Explore</span>
        </button>

        <button class="nav-item" id="sidebarCreateBtn" aria-label="Create Post">
          ${Icons.create()}
          <span>Create</span>
        </button>

        ${isStaff ? `
        <button class="nav-item ${State.activeTab === "moderation" ? "active" : ""}" data-tab="moderation" aria-label="Moderation Reports Queue">
          ${Icons.shield()}
          <span>Moderation</span>
          <span class="nav-badge">Staff</span>
        </button>
        ` : ""}

        <button class="nav-item ${State.activeTab === "profile" ? "active" : ""}" data-tab="profile" aria-label="User Profile">
          ${Icons.user()}
          <span>Profile</span>
        </button>

        <button class="nav-item" id="sidebarThemeToggle" aria-label="Toggle Theme">
          ${themeIcon}
          <span>Theme (${State.theme === "dark" ? "Light" : "Dark"})</span>
        </button>

        <button class="nav-item" id="sidebarSecurityHudBtn" aria-label="Open Security Inspector HUD">
          ${Icons.key()}
          <span>Security HUD</span>
          <span class="nav-badge nav-badge-accent">Chain ${State.telemetry.lastActiveChain}</span>
        </button>
      </nav>

      <div class="sidebar-user">
        <img class="sidebar-avatar" src="${avatarUrl}" alt="${username}'s avatar" />
        <div class="sidebar-user-info">
          <div class="sidebar-user-name">${username}</div>
          <div class="sidebar-user-role">
            ${State.user ? (State.user.roles || []).join(", ") || "Standard User" : "Anonymous"}
          </div>
        </div>
        <button id="sidebarSwitchUserBtn" class="sidebar-switch-btn" title="Switch Account">
          ${Icons.logout("icon")}
        </button>
      </div>
    </aside>

    <!-- Mobile Top Header -->
    <header class="mobile-header">
      <div class="mobile-brand">
        <span class="brand-logo-icon">${Icons.logo("icon")}</span>
        <span class="brand-logo-text brand-logo-mobile">InstaClone</span>
      </div>
      <div class="mobile-actions">
        <button id="mobileThemeToggle" class="mobile-icon-btn" aria-label="Toggle Theme">
          ${themeIcon}
        </button>
        <button id="mobileSecurityBtn" class="mobile-icon-btn mobile-security-btn" aria-label="Open Security Inspector">
          ${Icons.shield()}
        </button>
      </div>
    </header>

    <!-- Mobile Bottom Navigation -->
    <nav class="bottom-nav" aria-label="Mobile Navigation">
      <button class="bottom-nav-item ${State.activeTab === "feed" ? "active" : ""}" data-tab="feed" aria-label="Home Feed">
        ${Icons.home()}
      </button>
      <button class="bottom-nav-item ${State.activeTab === "explore" ? "active" : ""}" data-tab="explore" aria-label="Explore">
        ${Icons.explore()}
      </button>
      <button class="bottom-nav-item" id="mobileCreateBtn" aria-label="Create Post">
        ${Icons.create()}
      </button>
      ${isStaff ? `
      <button class="bottom-nav-item ${State.activeTab === "moderation" ? "active" : ""}" data-tab="moderation" aria-label="Moderation">
        ${Icons.shield()}
      </button>
      ` : ""}
      <button class="bottom-nav-item ${State.activeTab === "profile" ? "active" : ""}" data-tab="profile" aria-label="Profile">
        ${Icons.user()}
      </button>
    </nav>
  `;

  // Bind Tab Click Handlers
  container.querySelectorAll("[data-tab]").forEach(btn => {
    btn.addEventListener("click", () => {
      const tab = btn.getAttribute("data-tab");
      State.setActiveTab(tab);
    });
  });

  // Theme Toggles
  const toggleTheme = () => {
    State.setTheme(State.theme === "dark" ? "light" : "dark");
  };
  container.querySelector("#sidebarThemeToggle")?.addEventListener("click", toggleTheme);
  container.querySelector("#mobileThemeToggle")?.addEventListener("click", toggleTheme);

  // Security HUD Triggers
  const openHud = () => {
    const backdrop = document.querySelector("#hudDrawerBackdrop");
    if (backdrop) {
      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
    }
  };
  container.querySelector("#sidebarSecurityHudBtn")?.addEventListener("click", openHud);
  container.querySelector("#mobileSecurityBtn")?.addEventListener("click", openHud);

  // Switch User Dialog Trigger
  container.querySelector("#sidebarSwitchUserBtn")?.addEventListener("click", () => {
    const backdrop = document.querySelector("#authModalBackdrop");
    if (backdrop) {
      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
    }
  });

  // Create Modal Triggers
  const openCreate = () => {
    const backdrop = document.querySelector("#createModalBackdrop");
    if (backdrop) {
      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
      document.querySelector("#postCaptionInput")?.focus();
    }
  };
  container.querySelector("#sidebarCreateBtn")?.addEventListener("click", openCreate);
  container.querySelector("#mobileCreateBtn")?.addEventListener("click", openCreate);
}
