/**
 * Profile View Component (User Stats, Posts, Bookmarks & MFA Security Settings)
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";
import { getAvatarUrl, getPostImageUrl } from "../avatar-helper.js";

let profileSubTab = "posts"; // 'posts' | 'saved' | 'security'

export function renderProfile(container) {
  const user = State.user || {
    username: "anonymous",
    roles: ["ROLE_ANONYMOUS"],
    authorities: []
  };

  const isStaff = user.roles && (user.roles.includes("ROLE_ADMIN") || user.roles.includes("ROLE_MODERATOR"));
  const hasOtt = (user.authorities && user.authorities.includes("FACTOR_OTT")) || (user.roles && user.roles.includes("FACTOR_OTT"));

  const myPosts = State.posts.filter(p => p.authorUsername === user.username);
  const savedPosts = State.posts.filter(p => State.savedPostIds.has(p.id));

  const avatarUrl = getAvatarUrl(user.username);

  container.innerHTML = `
    <div class="profile-container">
      <!-- Profile Header -->
      <header class="profile-header">
        <div class="profile-avatar-wrap">
          <img src="${avatarUrl}" alt="${user.username}" class="profile-avatar" />
        </div>

        <div class="profile-info">
          <div class="profile-name-row">
            <h1 class="profile-username">${user.username}</h1>
            ${isStaff ? Icons.verified("icon") : ""}
            <button id="profileEditBtn" class="profile-edit-btn">
              Switch User
            </button>
            <button id="profileLogoutBtn" class="profile-logout-btn">
              Logout
            </button>
          </div>

          <!-- Roles & Authorities Badges -->
          <div class="profile-badges-row">
            ${(user.roles || []).map(r => `
              <span class="profile-badge ${r.includes("ADMIN") ? "admin" : r.includes("MODERATOR") ? "moderator" : ""}">
                ${r}
              </span>
            `).join("")}
            ${hasOtt ? `<span class="profile-badge ott">FACTOR_OTT Verified</span>` : ""}
            <span class="profile-badge">Mode: ${State.authMode.toUpperCase()}</span>
          </div>

          <!-- Stats Counters -->
          <div class="profile-stats-row">
            <div class="profile-stat"><strong>${myPosts.length}</strong> posts</div>
            <div class="profile-stat"><strong>1.2k</strong> followers</div>
            <div class="profile-stat"><strong>480</strong> following</div>
          </div>
        </div>
      </header>

      <!-- Profile Tabs -->
      <nav class="profile-tabs" role="tablist">
        <button class="profile-tab ${profileSubTab === "posts" ? "active" : ""}" data-subtab="posts">
          ${Icons.grid()}
          <span>POSTS</span>
        </button>
        <button class="profile-tab ${profileSubTab === "saved" ? "active" : ""}" data-subtab="saved">
          ${Icons.bookmark()}
          <span>SAVED (${savedPosts.length})</span>
        </button>
        <button class="profile-tab ${profileSubTab === "security" ? "active" : ""}" data-subtab="security">
          ${Icons.shield()}
          <span>SECURITY & MFA</span>
        </button>
      </nav>

      <!-- Subtab Content -->
      <div id="profileTabContent">
        ${renderSubTabContent(profileSubTab, myPosts, savedPosts, user, hasOtt)}
      </div>
    </div>
  `;

  // Bind Switch User / Logout
  container.querySelector("#profileEditBtn")?.addEventListener("click", () => {
    document.querySelector("#authModalBackdrop")?.classList.add("open");
  });

  container.querySelector("#profileLogoutBtn")?.addEventListener("click", async () => {
    await api.logout();
    Toast.info("Logged out.");
    State.setActiveTab("feed");
  });

  // Bind Subtab Switching
  container.querySelectorAll(".profile-tab").forEach(tab => {
    tab.addEventListener("click", () => {
      profileSubTab = tab.getAttribute("data-subtab");
      renderProfile(container);
    });
  });

  // Attach post card click handlers to view in feed
  container.querySelectorAll(".explore-card[data-post-id]").forEach(card => {
    const jumpToPost = () => {
      const postId = card.getAttribute("data-post-id");
      State.setActiveTab("feed");
      setTimeout(() => {
        const postEl = document.querySelector(`#post-${postId}`);
        if (postEl) {
          postEl.scrollIntoView({ behavior: "smooth", block: "center" });
          postEl.classList.add("highlight");
          setTimeout(() => { postEl.classList.remove("highlight"); }, 2000);
        }
      }, 150);
    };

    card.addEventListener("click", jumpToPost);
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        jumpToPost();
      }
    });
  });

  // Attach Security / MFA listeners if on security tab
  if (profileSubTab === "security") {
    attachSecurityTabListeners(container, user);
  }
}

function renderSubTabContent(subTab, myPosts, savedPosts, user, hasOtt) {
  if (subTab === "posts") {
    if (myPosts.length === 0) {
      return `
        <div class="profile-empty-state">
          ${Icons.create("icon")}
          <p>No posts shared yet by @${user.username}</p>
        </div>
      `;
    }
    return `
      <div class="explore-grid">
        ${myPosts.map(p => `
          <div class="explore-card" data-post-id="${p.id}" tabindex="0" role="button" aria-label="View post #${p.id} in feed">
            <img src="${getPostImageUrl(p.imageUrl, p.id)}" alt="${p.caption}" />
            <div class="explore-card-overlay">
              <div class="explore-stat">${Icons.heartFilled()} <span>${Number(p.id) * 7 + 12}</span></div>
            </div>
          </div>
        `).join("")}
      </div>
    `;
  }

  if (subTab === "saved") {
    if (savedPosts.length === 0) {
      return `
        <div class="profile-empty-state">
          ${Icons.bookmark("icon")}
          <p>Save photos and videos that you want to see again.</p>
        </div>
      `;
    }
    return `
      <div class="explore-grid">
        ${savedPosts.map(p => `
          <div class="explore-card" data-post-id="${p.id}" tabindex="0" role="button" aria-label="View saved post #${p.id} in feed">
            <img src="${getPostImageUrl(p.imageUrl, p.id)}" alt="${p.caption}" />
            <div class="explore-card-overlay">
              <div class="explore-stat">${Icons.heartFilled()} <span>Saved</span></div>
            </div>
          </div>
        `).join("")}
      </div>
    `;
  }

  if (subTab === "security") {
    return `
      <div class="profile-security-wrap">
        
        <!-- Factor 1 & Factor 2 Card -->
        <div class="telemetry-card">
          <div class="telemetry-card-title">
            ${Icons.shield("icon")} Multi-Factor Authentication (Spring Security 7.x)
          </div>
          <p class="profile-security-desc">
            High-privilege actions (like account deletion) strictly require <strong>both</strong> primary password factor and One-Time Token (OTT) factor via <code>AllRequiredFactorsAuthorizationManager</code>.
          </p>

          <div class="profile-factor-list">
            <!-- Factor 1 -->
            <div class="profile-factor-item">
              <div class="profile-factor-info">
                <span class="icon-success">${Icons.check("icon")}</span>
                <div>
                  <div class="profile-factor-title">Factor 1: FACTOR_PASSWORD</div>
                  <div class="profile-factor-subtitle">Primary credentials verified</div>
                </div>
              </div>
              <span class="profile-badge badge-active">ACTIVE</span>
            </div>

            <!-- Factor 2 -->
            <div class="profile-factor-item">
              <div class="profile-factor-info">
                <span class="${hasOtt ? "icon-success" : "icon-warning"}">
                  ${hasOtt ? Icons.check("icon") : Icons.alertTriangle("icon")}
                </span>
                <div>
                  <div class="profile-factor-title">Factor 2: FACTOR_OTT (Magic Link Token)</div>
                  <div class="profile-factor-subtitle">
                    ${hasOtt ? "Token validated and merged into SecurityContext" : "Not yet claimed for this session"}
                  </div>
                </div>
              </div>
              <span class="profile-badge ${hasOtt ? "badge-active" : "badge-pending"}">
                ${hasOtt ? "VERIFIED" : "PENDING"}
              </span>
            </div>
          </div>
        </div>

        <!-- OTT Generator & Claim Panel -->
        <div class="telemetry-card">
          <div class="telemetry-card-title">
            ${Icons.key("icon")} Request & Claim One-Time Token (OTT)
          </div>
          <p class="profile-security-desc">
            Generate a single-use login token dispatched by <code>InMemoryOneTimeTokenService</code>.
          </p>

          <div class="profile-btn-row">
            <button id="btnGenerateOtt" class="btn-primary-action">
              Generate OTT Magic Link
            </button>
            <button id="btnFetchLatestOtt" class="btn-secondary-action">
              Auto-Fetch Token
            </button>
          </div>

          <div class="profile-input-row">
            <input type="text" id="ottTokenInput" placeholder="Enter or paste OTT token..." class="profile-ott-input" />
            <button id="btnConsumeOtt" class="btn-success-action">
              Consume & Claim FACTOR_OTT
            </button>
          </div>
        </div>

        <!-- Danger Zone: Permanent Account Deletion -->
        <div class="telemetry-card telemetry-card-danger">
          <div class="telemetry-card-title title-danger">
            ${Icons.alertTriangle("icon")} Danger Zone: Purge Account
          </div>
          <p class="profile-danger-desc">
            Permanently delete user <code>@${user.username}</code> and cascade delete all owned posts and tokens.
          </p>
          <div class="profile-danger-action">
            <button id="btnDeleteAccount" class="btn-danger-action">
              Delete My Account (DELETE /api/v1/users/me)
            </button>
          </div>
        </div>

      </div>
    `;
  }

  return "";
}

function attachSecurityTabListeners(container, user) {
  const tokenInput = container.querySelector("#ottTokenInput");

  // Generate OTT
  container.querySelector("#btnGenerateOtt")?.addEventListener("click", async () => {
    try {
      const res = await api.generateOtt(user.username);
      if (res.ok) {
        Toast.success("One-Time Token generated and sent to email log!");
        // Try to auto-fill
        const token = await api.getLatestOttToken();
        if (token && tokenInput) {
          tokenInput.value = token;
          Toast.info(`Auto-detected token: ${token.substring(0, 8)}...`);
        }
      } else {
        Toast.danger(`OTT generation error: ${res.statusText}`);
      }
    } catch (err) {
      Toast.danger(`Error: ${err.message}`);
    }
  });

  // Auto-Fetch
  container.querySelector("#btnFetchLatestOtt")?.addEventListener("click", async () => {
    const token = await api.getLatestOttToken();
    if (token && tokenInput) {
      tokenInput.value = token;
      Toast.info(`Retrieved token from outbox: ${token}`);
    } else {
      Toast.warning("No tokens found in outbox. Click 'Generate' first.");
    }
  });

  // Consume OTT
  container.querySelector("#btnConsumeOtt")?.addEventListener("click", async () => {
    const token = tokenInput?.value?.trim();
    if (!token) {
      Toast.warning("Please enter or generate a token first.");
      return;
    }
    try {
      await api.loginOtt(token);
      Toast.success("FACTOR_OTT successfully acquired! You now hold Multi-Factor Authentication rights.");
      renderProfile(container);
    } catch (err) {
      Toast.danger(`OTT verification failed: ${err.message}`);
    }
  });

  // Delete Account
  container.querySelector("#btnDeleteAccount")?.addEventListener("click", async () => {
    if (!confirm(`Are you sure you want to permanently delete @${user.username}?`)) return;

    try {
      const res = await api.deleteMyAccount();
      if (res.ok) {
        Toast.success(`User @${user.username} purged completely.`);
        await api.logout();
        State.setActiveTab("feed");
      } else if (res.status === 403 || res.status === 401) {
        Toast.danger("Spring Security 7.x MFA Rejection: Account deletion requires both FACTOR_PASSWORD and FACTOR_OTT!");
        alert("SPRING SECURITY 7.x DEFENSE TRIGGERED:\n\nRequest denied! Endpoint DELETE /api/v1/users/me is guarded by AllRequiredFactorsAuthorizationManager. You must acquire FACTOR_OTT first.");
      } else {
        Toast.danger(`Deletion failed with status: ${res.status}`);
      }
    } catch (err) {
      Toast.danger(`Deletion failed: ${err.message}`);
    }
  });
}
