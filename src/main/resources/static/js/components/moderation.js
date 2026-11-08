/**
 * Staff Moderation Command Center Component
 * Guarded by Spring Security Authority: POST_DELETE_ANY / ROLE_MODERATOR / ROLE_ADMIN
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";

export function renderModeration(container) {
  const user = State.user;
  const isStaff = user && user.roles && (
    user.roles.includes("ROLE_ADMIN") ||
    user.roles.includes("ROLE_MODERATOR") ||
    user.roles.includes("POST_DELETE_ANY")
  );

  if (!isStaff) {
    container.innerHTML = `
      <div class="mod-access-denied-card">
        <div class="mod-access-icon">
          ${Icons.shield("icon")}
        </div>
        <h2 class="mod-access-title">Staff Clearance Required</h2>
        <p class="mod-access-desc">
          The Moderation Command Center is restricted to accounts with <code>ROLE_MODERATOR</code>, <code>ROLE_ADMIN</code>, or authority <code>POST_DELETE_ANY</code>.
        </p>
        <div class="mod-access-help">
          Current account: <strong>@${user ? user.username : "anonymous"}</strong><br/>
          Authorities: <code>${user ? (user.roles || []).join(", ") || "ROLE_ANONYMOUS" : "None"}</code>
        </div>
        <button id="modSwitchBtn" class="mod-switch-btn">
          Switch to Moderator Account (@mod_alice)
        </button>
      </div>
    `;

    container.querySelector("#modSwitchBtn")?.addEventListener("click", async () => {
      try {
        await api.loginSession("mod_alice", "Password123!");
        Toast.success("Switched to Staff Moderator @mod_alice");
        renderModeration(container);
      } catch (err) {
        Toast.danger(`Switch failed: ${err.message}`);
      }
    });

    return;
  }

  // Sample moderation violation queue
  const reports = [
    { id: 4, reason: "Phishing & Fake Auth Link", postCaption: "Bob public post", reporter: "alice", timestamp: "12m ago" },
    { id: 1, reason: "Suspicious API Activity", postCaption: "Alice public post", reporter: "system_heuristic", timestamp: "45m ago" }
  ];

  container.innerHTML = `
    <div class="mod-container">
      <div class="mod-header-row">
        <div>
          <h1 class="mod-title">
            ${Icons.shield("icon")} Moderation Command Center
          </h1>
          <p class="mod-subtitle">
            Authenticated as <strong>@${user.username}</strong> with authority <code>POST_DELETE_ANY</code>
          </p>
        </div>
        <button id="refreshReportsBtn" class="mod-refresh-btn">
          Refresh Queue
        </button>
      </div>

      <!-- Violation Reports Queue -->
      <div class="mod-queue-list">
        ${reports.length === 0 ? `
          <div class="mod-empty-queue">
            ${Icons.check("icon")}
            <p>All queues clear! No pending violation reports.</p>
          </div>
        ` : reports.map(r => `
          <div class="telemetry-card" id="report-${r.id}">
            <div class="mod-report-header">
              <span class="profile-badge mod-badge-danger">
                FLAGGED REPORT #${r.id}
              </span>
              <span class="mod-report-time">${r.timestamp}</span>
            </div>
            <h3 class="mod-report-title">${r.reason}</h3>
            <p class="mod-report-caption">
              Target Post Caption: <em>"${r.postCaption}"</em>
            </p>
            <div class="mod-report-reporter">
              Reported by: <strong>@${r.reporter}</strong> • Status: <strong>Pending Review</strong>
            </div>
            <div class="mod-report-actions">
              <button class="dismiss-report-btn mod-dismiss-btn" data-report-id="${r.id}">
                Dismiss Report
              </button>
              <button class="delete-violating-post-btn mod-delete-btn" data-post-id="${r.id}">
                ${Icons.trash("icon")} Enforce Deletion (POST_DELETE_ANY)
              </button>
            </div>
          </div>
        `).join("")}
      </div>
    </div>
  `;

  // Bind Dismiss Actions
  container.querySelectorAll(".dismiss-report-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-report-id");
      const card = container.querySelector(`#report-${id}`);
      if (card) {
        card.classList.add("deleting");
        setTimeout(() => card.remove(), 250);
        Toast.info(`Report #${id} dismissed.`);
      }
    });
  });

  // Bind Delete Violating Post Actions
  container.querySelectorAll(".delete-violating-post-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const postId = Number(btn.getAttribute("data-post-id"));
      if (!confirm(`Are you sure you want to enforce removal of Post #${postId} as Staff?`)) return;

      btn.classList.add("loading");
      try {
        await api.deletePost(postId);
        Toast.success(`Post #${postId} purged via staff authority POST_DELETE_ANY!`);
        const card = container.querySelector(`#report-${postId}`);
        if (card) {
          card.classList.add("deleting");
          setTimeout(() => card.remove(), 250);
        }
      } catch (err) {
        Toast.danger(`Action rejected: ${err.message}`);
        btn.classList.remove("loading");
      }
    });
  });

  container.querySelector("#refreshReportsBtn")?.addEventListener("click", () => {
    Toast.success("Moderation queue refreshed.");
    renderModeration(container);
  });
}
