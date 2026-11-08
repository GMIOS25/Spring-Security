/**
 * Spring Security 7.x Live Telemetry & Architecture HUD Drawer
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";

export function renderSecurityHud(container) {
  const telemetry = State.telemetry;
  const activeChain = telemetry.lastActiveChain || 3;
  const user = State.user;

  container.innerHTML = `
    <!-- Floating Quick Trigger -->
    <button id="floatingHudTrigger" class="security-hud-trigger" aria-label="Open Spring Security 7.x Inspector">
      <span class="hud-pulse-dot"></span>
      ${Icons.shield("icon")}
      <span>Security Inspector</span>
      <span id="hudTriggerBadge" class="hud-trigger-badge">
        Chain ${activeChain}
      </span>
    </button>

    <!-- Slide-over Drawer Backdrop -->
    <div id="hudDrawerBackdrop" class="security-drawer-backdrop" aria-hidden="true">
      <aside class="security-drawer" role="dialog" aria-modal="true" aria-labelledby="hudDrawerTitle">
        
        <!-- Header -->
        <div class="security-drawer-header">
          <div class="hud-header-title-wrap">
            <div class="hud-header-icon">${Icons.shield("icon")}</div>
            <div>
              <h2 id="hudDrawerTitle" class="hud-title">Spring Security 7.x Live HUD</h2>
              <div class="hud-subtitle">Architecture, FilterChains & Defenses</div>
            </div>
          </div>
          <button id="closeHudDrawerBtn" class="hud-close-btn" aria-label="Close Security HUD">
            ${Icons.close("icon")}
          </button>
        </div>

        <!-- Body -->
        <div class="security-drawer-body">
          
          <!-- Filter Chains Visualizer -->
          <div class="telemetry-card">
            <div class="telemetry-card-title">
              ${Icons.grid("icon")} Active Filter Chains (@Order Hierarchy)
            </div>
            
            <div class="chain-flow">
              <!-- Chain 1 -->
              <div id="chainBadge1" class="chain-badge ${activeChain === 1 ? "active" : ""}">
                <div>
                  <strong>Chain 1 (@Order 1): Public Feed</strong>
                  <div class="hud-chain-sub">GET /api/v1/feed/public/** • Stateless • permitAll</div>
                </div>
                <span class="profile-badge ott chain-status-tag ${activeChain === 1 ? "" : "hidden"}">ACTIVE NOW</span>
              </div>

              <!-- Chain 2 -->
              <div id="chainBadge2" class="chain-badge ${activeChain === 2 ? "active" : ""}">
                <div>
                  <strong>Chain 2 (@Order 2): Stateless API</strong>
                  <div class="hud-chain-sub">Bearer JWT • /auth/token • No CSRF • Rate Limited</div>
                </div>
                <span class="profile-badge ott chain-status-tag ${activeChain === 2 ? "" : "hidden"}">ACTIVE NOW</span>
              </div>

              <!-- Chain 3 -->
              <div id="chainBadge3" class="chain-badge ${activeChain === 3 ? "active" : ""}">
                <div>
                  <strong>Chain 3 (@Order 3): Web Session (Default)</strong>
                  <div class="hud-chain-sub">Cookie Session • XOR CSRF • Max 2 Sessions • MFA</div>
                </div>
                <span class="profile-badge ott chain-status-tag ${activeChain === 3 ? "" : "hidden"}">ACTIVE NOW</span>
              </div>
            </div>
          </div>

          <!-- Current Principal & Context -->
          <div class="telemetry-card">
            <div class="telemetry-card-title">
              ${Icons.user("icon")} SecurityContextHolder Principal
            </div>

            <div class="hud-principal-meta">
              <div><strong>Username:</strong> <code id="hudPrincipalUsername">${user ? user.username : "anonymousUser"}</code></div>
              <div><strong>Auth Channel:</strong> <code id="hudPrincipalMode">${State.authMode.toUpperCase()} (${State.authMode === "jwt" ? "Bearer Token" : "Cookie Session"})</code></div>
              <div><strong>Granted Authorities:</strong></div>
              <div id="hudPrincipalRoles" class="hud-roles-wrap">
                ${(user?.roles || ["ROLE_ANONYMOUS"]).map(r => `
                  <span class="profile-badge sm ${r.includes("ADMIN") ? "admin" : r.includes("MODERATOR") ? "moderator" : r.includes("FACTOR") ? "ott" : ""}">${r}</span>
                `).join("")}
              </div>
            </div>
          </div>

          <!-- CSRF Token (XOR Masked) -->
          <div class="telemetry-card">
            <div class="telemetry-card-title">
              ${Icons.lock("icon")} XOR Masked CSRF Token (BREACH Defense)
            </div>
            <p class="hud-note">
              Spring Security 7.x <code>XorCsrfTokenRequestAttributeHandler</code> XORs the raw token with a dynamic per-request random pad to defend against side-channel compression attacks.
            </p>
            <div class="code-box" id="hudCsrfTokenBox">
              ${State.csrf?.token || telemetry.csrfMasked || "Token not yet requested"}
            </div>
            <button id="btnRefreshCsrf" class="hud-action-btn-secondary">
              Re-fetch CSRF Token
            </button>
          </div>

          <!-- BOLA / IDOR Interactive Defense Simulator -->
          <div class="telemetry-card danger-card">
            <div class="telemetry-card-title danger">
              ${Icons.shield("icon")} BOLA / IDOR Defense Tester (Hidden Resource Policy)
            </div>
            <p class="hud-note">
              Post #2 is Alice's PRIVATE post. If Bob attempts to access it, Spring Security's hidden resource policy must return <strong>404 Not Found</strong> instead of 403 Forbidden to hide the existence of the resource.
            </p>
            
            <button id="btnRunBolaAttack" class="hud-action-btn-danger">
              Run BOLA Probe: GET /api/v1/posts/2
            </button>

            <div id="hudBolaResult" class="code-box hud-code-sm">
              ${telemetry.bolaTestResult ? JSON.stringify(telemetry.bolaTestResult, null, 2) : "Click button above to probe BOLA policy."}
            </div>
          </div>

          <!-- Last Request Telemetry -->
          <div class="telemetry-card">
            <div class="telemetry-card-title">
              ${Icons.globe("icon")} Last HTTP Dispatch Telemetry
            </div>
            <div class="code-box hud-code-telemetry" id="hudLastTelemetryBox">
              ${telemetry.lastRequest ? JSON.stringify({
                request: telemetry.lastRequest,
                response: telemetry.lastResponse
              }, null, 2) : "No requests dispatched yet in this session."}
            </div>
          </div>

        </div>
      </aside>
    </div>
  `;

  // Bind Open/Close
  const trigger = container.querySelector("#floatingHudTrigger");
  const backdrop = container.querySelector("#hudDrawerBackdrop");
  const closeBtn = container.querySelector("#closeHudDrawerBtn");

  const openDrawer = () => {
    updateHudContent(container);
    backdrop.classList.add("open");
    backdrop.setAttribute("aria-hidden", "false");
  };

  const closeDrawer = () => {
    backdrop.classList.remove("open");
    backdrop.setAttribute("aria-hidden", "true");
  };

  trigger?.addEventListener("click", openDrawer);
  closeBtn?.addEventListener("click", closeDrawer);
  backdrop?.addEventListener("click", (e) => {
    if (e.target === backdrop) closeDrawer();
  });

  // Re-fetch CSRF
  container.querySelector("#btnRefreshCsrf")?.addEventListener("click", async () => {
    State.setCsrf(null);
    const csrf = await api.ensureCsrfToken();
    const box = container.querySelector("#hudCsrfTokenBox");
    if (box) box.textContent = csrf ? csrf.token : "Failed to load CSRF token";
    Toast.success("CSRF token renewed (new XOR mask generated)");
    updateHudContent(container);
  });

  // BOLA Probe
  container.querySelector("#btnRunBolaAttack")?.addEventListener("click", async () => {
    const btn = container.querySelector("#btnRunBolaAttack");
    btn.disabled = true;
    btn.textContent = "Probing...";
    const resBox = container.querySelector("#hudBolaResult");

    try {
      const result = await api.testBolaAttack(2);
      if (resBox) resBox.textContent = JSON.stringify(result, null, 2);
      if (result.status === 404) {
        Toast.success("Defense verified: 404 Not Found returned!");
      } else {
        Toast.info(`Probe completed with status: ${result.status}`);
      }
    } catch (err) {
      if (resBox) resBox.textContent = `Error: ${err.message}`;
    } finally {
      btn.disabled = false;
      btn.textContent = "Run BOLA Probe: GET /api/v1/posts/2";
      updateHudContent(container);
    }
  });

  // Reactive Subscription
  State.subscribe((event) => {
    if (event.startsWith("telemetry") || event.startsWith("user") || event.startsWith("csrf") || event.startsWith("authMode")) {
      updateHudContent(container);
    }
  });
}

export function updateHudContent(container) {
  if (!container) return;
  const telemetry = State.telemetry;
  const activeChain = telemetry.lastActiveChain || 3;
  const user = State.user;

  // 1. Floating trigger badge
  const triggerBadge = container.querySelector("#hudTriggerBadge");
  if (triggerBadge) triggerBadge.textContent = `Chain ${activeChain}`;

  // 2. Chain Badges
  [1, 2, 3].forEach(chainNum => {
    const badge = container.querySelector(`#chainBadge${chainNum}`);
    if (badge) {
      const isActive = chainNum === activeChain;
      badge.classList.toggle("active", isActive);
      const tag = badge.querySelector(".chain-status-tag");
      if (tag) tag.classList.toggle("hidden", !isActive);
    }
  });

  // 3. Principal Details
  const usernameEl = container.querySelector("#hudPrincipalUsername");
  if (usernameEl) usernameEl.textContent = user ? user.username : "anonymousUser";

  const modeEl = container.querySelector("#hudPrincipalMode");
  if (modeEl) modeEl.textContent = `${State.authMode.toUpperCase()} (${State.authMode === "jwt" ? "Bearer Token" : "Cookie Session"})`;

  const rolesEl = container.querySelector("#hudPrincipalRoles");
  if (rolesEl) {
    const rolesList = user?.roles || ["ROLE_ANONYMOUS"];
    rolesEl.innerHTML = rolesList.map(r => `
      <span class="profile-badge sm ${r.includes("ADMIN") ? "admin" : r.includes("MODERATOR") ? "moderator" : r.includes("FACTOR") ? "ott" : ""}">${r}</span>
    `).join("");
  }

  // 4. CSRF box
  const csrfBox = container.querySelector("#hudCsrfTokenBox");
  if (csrfBox) {
    csrfBox.textContent = State.csrf?.token || telemetry.csrfMasked || "Token not yet requested";
  }

  // 5. Last Dispatch Box
  const lastBox = container.querySelector("#hudLastTelemetryBox");
  if (lastBox) {
    lastBox.textContent = telemetry.lastRequest ? JSON.stringify({
      request: telemetry.lastRequest,
      response: telemetry.lastResponse
    }, null, 2) : "No requests dispatched yet in this session.";
  }

  // 6. BOLA result box
  const bolaBox = container.querySelector("#hudBolaResult");
  if (bolaBox && telemetry.bolaTestResult) {
    bolaBox.textContent = JSON.stringify(telemetry.bolaTestResult, null, 2);
  }
}
