/**
 * Authentication & Fast Switcher Modal Component
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";

let authModalTab = "fast"; // 'fast' | 'custom' | 'ott' | 'register'

export function renderAuthModal(container) {
  const demoUsers = [
    { username: "alice", role: "ROLE_USER", desc: "Standard consumer account with followers" },
    { username: "bob", role: "ROLE_USER", desc: "Secondary account (BOLA/IDOR test subject)" },
    { username: "charlie", role: "ROLE_USER", desc: "Tertiary account (CSRF evaluation subject)" },
    { username: "mod_alice", role: "ROLE_MODERATOR", desc: "Staff moderator (POST_DELETE_ANY clearance)" },
    { username: "cuong_admin", role: "ROLE_ADMIN", desc: "Superuser admin with all authority factors" }
  ];

  container.innerHTML = `
    <div id="authModalBackdrop" class="modal-backdrop" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="authModalTitle">
      <div class="modal-dialog auth-modal-dialog">
        <!-- Modal Header -->
        <div class="modal-header">
          <div>
            <h2 id="authModalTitle" class="modal-title">Authentication &amp; Role Switcher</h2>
            <div class="auth-modal-subtitle">Spring Security 7.x Authentication Manager</div>
          </div>
          <button id="closeAuthModalBtn" class="modal-close-btn" aria-label="Close modal">
            ${Icons.close("icon")}
          </button>
        </div>

        <!-- Tab Selector -->
        <div class="auth-tabs-bar" role="tablist">
          <button class="auth-tab-btn ${authModalTab === "fast" ? "active" : ""}" data-tab="fast">
            Fast Switch (1-Click)
          </button>
          <button class="auth-tab-btn ${authModalTab === "custom" ? "active" : ""}" data-tab="custom">
            Session / JWT Login
          </button>
          <button class="auth-tab-btn ${authModalTab === "ott" ? "active" : ""}" data-tab="ott">
            Magic Link (OTT)
          </button>
          <button class="auth-tab-btn ${authModalTab === "register" ? "active" : ""}" data-tab="register">
            Register New User
          </button>
        </div>

        <!-- Modal Body Content -->
        <div class="modal-body">
          ${renderAuthTabContent(authModalTab, demoUsers)}
        </div>
      </div>
    </div>
  `;

  // Bind Tab Click Handlers
  container.querySelectorAll(".auth-tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      authModalTab = btn.getAttribute("data-tab");
      renderAuthModal(container);
    });
  });

  // Close handlers
  const backdrop = container.querySelector("#authModalBackdrop");
  const closeBtn = container.querySelector("#closeAuthModalBtn");

  const closeModal = () => {
    backdrop?.classList.remove("open");
    backdrop?.setAttribute("aria-hidden", "true");
  };

  closeBtn?.addEventListener("click", closeModal);
  backdrop?.addEventListener("click", (e) => {
    if (e.target === backdrop) closeModal();
  });

  // Attach tab specific action handlers
  attachAuthModalListeners(container, closeModal);
}

function renderAuthTabContent(tab, demoUsers) {
  if (tab === "fast") {
    return `
      <div class="auth-fast-wrap">
        <p class="auth-tab-desc">
          Click to instantly authenticate with pre-seeded test accounts under either <strong>Stateful Session (Chain 3)</strong> or <strong>Stateless JWT Bearer (Chain 2)</strong>:
        </p>

        <div class="auth-card-list">
          ${demoUsers.map(u => `
            <div class="auth-card-item">
              <div class="auth-card-left">
                <div class="auth-card-user">
                  <span>@${u.username}</span>
                  <span class="profile-badge">${u.role}</span>
                </div>
                <div class="auth-card-desc">${u.desc}</div>
              </div>
              <div class="auth-card-actions">
                <button class="fast-login-session-btn" data-username="${u.username}">
                  Session (CSRF)
                </button>
                <button class="fast-login-jwt-btn" data-username="${u.username}">
                  Stateless JWT
                </button>
              </div>
            </div>
          `).join("")}
        </div>
      </div>
    `;
  }

  if (tab === "custom") {
    return `
      <form id="customLoginForm" class="auth-form-column">
        <div>
          <label for="customLoginUsername" class="form-label">Username</label>
          <input type="text" id="customLoginUsername" value="alice" required />
        </div>

        <div>
          <label for="customLoginPassword" class="form-label">Password</label>
          <input type="password" id="customLoginPassword" value="Password123!" required />
        </div>

        <div>
          <label class="form-label">Target Architecture Chain</label>
          <div class="auth-form-radio-group">
            <label class="auth-radio-label">
              <input type="radio" name="authChannelRadio" value="session" checked />
              <span>Chain 3: Session Cookie + XOR CSRF</span>
            </label>
            <label class="auth-radio-label">
              <input type="radio" name="authChannelRadio" value="jwt" />
              <span>Chain 2: Stateless JWT Bearer</span>
            </label>
          </div>
        </div>

        <button type="submit" class="btn-submit-post auth-form-submit">
          Sign In
        </button>

        <div class="auth-oauth-wrap">
          <a href="/oauth2/authorization/github" class="auth-oauth-link">
            Or authenticate via OAuth 2.1 (GitHub SSO)
          </a>
        </div>
      </form>
    `;
  }

  if (tab === "ott") {
    return `
      <div class="auth-form-column">
        <p class="auth-tab-desc">
          One-Time Token (OTT) authentication dispatches a single-use token via <code>MagicLinkMailer</code> and grants <strong>FACTOR_OTT</strong> rights for Step-Up MFA.
        </p>

        <div>
          <label for="ottGenerateUser" class="form-label">Username to target</label>
          <div class="auth-ott-row">
            <input type="text" id="ottGenerateUser" value="alice" class="auth-flex-input" />
            <button id="btnDispatchOttModal" class="btn-primary-action auth-btn-dispatch">
              Dispatch OTT
            </button>
          </div>
        </div>

        <div>
          <label for="ottSubmitToken" class="form-label">Token received</label>
          <div class="auth-ott-row">
            <input type="text" id="ottSubmitToken" placeholder="Paste OTT token or click auto-fetch" class="auth-flex-input" />
            <button id="btnFetchOttModal" class="btn-secondary-action">
              Fetch Outbox
            </button>
          </div>
        </div>

        <button id="btnConsumeOttModal" class="btn-success-action auth-form-submit">
          Consume Token &amp; Authenticate
        </button>
      </div>
    `;
  }

  if (tab === "register") {
    return `
      <form id="registerForm" class="auth-form-column">
        <div>
          <label for="regUsername" class="form-label">Username</label>
          <input type="text" id="regUsername" placeholder="e.g. dev_user" required />
        </div>
        <div>
          <label for="regEmail" class="form-label">Email</label>
          <input type="email" id="regEmail" placeholder="user@example.com" required />
        </div>
        <div>
          <label for="regPassword" class="form-label">Password</label>
          <input type="password" id="regPassword" placeholder="Minimum 8 characters" required />
        </div>
        <button type="submit" class="btn-submit-post auth-form-submit">
          Register &amp; Login
        </button>
      </form>
    `;
  }

  return "";
}

function attachAuthModalListeners(container, closeModal) {
  // Fast Session Login
  container.querySelectorAll(".fast-login-session-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const username = btn.getAttribute("data-username");
      btn.classList.add("loading");
      try {
        await api.loginSession(username, "Password123!");
        Toast.success(`Logged in as @${username} (Session mode)`);
        closeModal();
        await api.getFeedPosts();
        State.setActiveTab("feed");
      } catch (err) {
        Toast.danger(`Login failed: ${err.message}`);
      } finally {
        btn.classList.remove("loading");
      }
    });
  });

  // Fast JWT Login
  container.querySelectorAll(".fast-login-jwt-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const username = btn.getAttribute("data-username");
      btn.classList.add("loading");
      try {
        await api.loginJwt(username, "Password123!");
        Toast.success(`Acquired Stateless JWT Bearer for @${username}`);
        closeModal();
        await api.getFeedPosts();
        State.setActiveTab("feed");
      } catch (err) {
        Toast.danger(`JWT Login failed: ${err.message}`);
      } finally {
        btn.classList.remove("loading");
      }
    });
  });

  // Custom Form Login
  const customForm = container.querySelector("#customLoginForm");
  customForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = container.querySelector("#customLoginUsername").value.trim();
    const password = container.querySelector("#customLoginPassword").value;
    const mode = container.querySelector('input[name="authChannelRadio"]:checked')?.value || "session";

    try {
      if (mode === "session") {
        await api.loginSession(username, password);
        Toast.success(`Session login successful for @${username}`);
      } else {
        await api.loginJwt(username, password);
        Toast.success(`Stateless JWT login successful for @${username}`);
      }
      closeModal();
      await api.getFeedPosts();
      State.setActiveTab("feed");
    } catch (err) {
      Toast.danger(`Authentication failed: ${err.message}`);
    }
  });

  // OTT Generation in Modal
  container.querySelector("#btnDispatchOttModal")?.addEventListener("click", async () => {
    const user = container.querySelector("#ottGenerateUser")?.value?.trim() || "alice";
    try {
      await api.generateOtt(user);
      Toast.success(`OTT token created for @${user}!`);
      const token = await api.getLatestOttToken();
      if (token) {
        const tokenInput = container.querySelector("#ottSubmitToken");
        if (tokenInput) tokenInput.value = token;
      }
    } catch (err) {
      Toast.danger(`Dispatch failed: ${err.message}`);
    }
  });

  // OTT Auto Fetch
  container.querySelector("#btnFetchOttModal")?.addEventListener("click", async () => {
    const token = await api.getLatestOttToken();
    if (token) {
      const tokenInput = container.querySelector("#ottSubmitToken");
      if (tokenInput) tokenInput.value = token;
      Toast.info(`Retrieved token: ${token.substring(0, 8)}...`);
    } else {
      Toast.warning("No token found in outbox.");
    }
  });

  // OTT Consume
  container.querySelector("#btnConsumeOttModal")?.addEventListener("click", async () => {
    const token = container.querySelector("#ottSubmitToken")?.value?.trim();
    if (!token) {
      Toast.warning("Please enter or generate a token first.");
      return;
    }
    try {
      await api.loginOtt(token);
      Toast.success("Successfully authenticated via OTT Magic Link!");
      closeModal();
      await api.getFeedPosts();
      State.setActiveTab("profile");
    } catch (err) {
      Toast.danger(`Verification failed: ${err.message}`);
    }
  });

  // Register Form
  const regForm = container.querySelector("#registerForm");
  regForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = container.querySelector("#regUsername").value.trim();
    const email = container.querySelector("#regEmail").value.trim();
    const password = container.querySelector("#regPassword").value;

    try {
      await api.register(username, email, password);
      Toast.success(`Account @${username} registered successfully!`);
      closeModal();
      await api.loginSession(username, password);
      await api.getFeedPosts();
      State.setActiveTab("feed");
    } catch (err) {
      Toast.danger(`Registration failed: ${err.message}`);
    }
  });
}
