/**
 * Feed Component (Posts Stream, Double-Tap Like, Comments, Fast Switcher)
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";
import { getAvatarUrl, getPostImageUrl } from "../avatar-helper.js";

export function renderFeed(container) {
  const currentUsername = State.user ? State.user.username : null;
  const isStaff = State.user && State.user.roles && (
    State.user.roles.includes("ROLE_ADMIN") ||
    State.user.roles.includes("ROLE_MODERATOR") ||
    State.user.roles.includes("POST_DELETE_ANY")
  );

  // Fast account switcher demo bar
  const demoUsers = [
    { id: "alice", label: "alice", role: "User", badge: "badge-user" },
    { id: "bob", label: "bob", role: "BOLA", badge: "badge-bola" },
    { id: "charlie", label: "charlie", role: "CSRF", badge: "badge-csrf" },
    { id: "mod_alice", label: "mod", role: "Staff", badge: "badge-mod" },
    { id: "cuong_admin", label: "cuong", role: "Admin", badge: "badge-admin" }
  ];

  container.innerHTML = `
    <!-- 1-Click Fast Account Switcher Bar -->
    <div class="user-switcher-bar" role="toolbar" aria-label="Quick Account Switcher">
      <div class="switcher-lead">
        <span class="switcher-lead-icon">${Icons.user("icon")}</span>
        <span class="switcher-lead-text">Switcher:</span>
      </div>
      <div class="switcher-pills-wrap">
        ${demoUsers.map(u => `
          <button class="switcher-pill ${currentUsername === u.id ? "active" : ""}" data-switch-username="${u.id}" title="Switch to @${u.id}">
            <img class="switcher-pill-avatar" src="${getAvatarUrl(u.id)}" alt="${u.id}" />
            <span class="switcher-pill-name">@${u.label}</span>
            <span class="switcher-role-tag ${u.badge}">${u.role}</span>
          </button>
        `).join("")}
        <button class="switcher-pill switcher-more-btn" id="switchCustomBtn">
          <span>More...</span>
        </button>
      </div>
    </div>

    <!-- Feed Stream -->
    <div id="postsStream" class="content-feed">
      ${State.posts.length === 0 ? `
        <div class="feed-empty-card">
          <div class="feed-empty-icon">
            ${Icons.create("icon")}
          </div>
          <h3 class="feed-empty-title">No posts in feed yet</h3>
          <p class="feed-empty-desc">
            Create your first post or switch accounts to explore protected resources!
          </p>
          <button id="emptyCreateBtn" class="feed-empty-create-btn">
            Create Post
          </button>
        </div>
      ` : State.posts.map(post => renderPostCard(post, currentUsername, isStaff)).join("")}
    </div>
  `;

  // Bind Fast Switcher Clicks
  container.querySelectorAll("[data-switch-username]").forEach(btn => {
    btn.addEventListener("click", async () => {
      const username = btn.getAttribute("data-switch-username");
      btn.classList.add("loading");
      try {
        await api.loginSession(username, "Password123!");
        Toast.success(`Switched account to @${username} (Session + CSRF)`);
        await api.getFeedPosts();
        renderFeed(container);
      } catch (err) {
        Toast.danger(`Switch failed: ${err.message}`);
      } finally {
        btn.classList.remove("loading");
      }
    });
  });

  container.querySelector("#switchCustomBtn")?.addEventListener("click", () => {
    const backdrop = document.querySelector("#authModalBackdrop");
    if (backdrop) {
      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
    }
  });

  container.querySelector("#emptyCreateBtn")?.addEventListener("click", () => {
    const backdrop = document.querySelector("#createModalBackdrop");
    if (backdrop) {
      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
      document.querySelector("#postCaptionInput")?.focus();
    }
  });

  // Attach card event listeners
  attachPostCardListeners(container);

  // Render desktop sidebar
  renderFeedSidebar(document.querySelector("#feedSidebarMount"));
}

function renderPostCard(post, currentUsername, isStaff) {
  const isOwner = currentUsername && post.authorUsername === currentUsername;
  const canDelete = isOwner || isStaff;
  const isLiked = State.likedPostIds.has(post.id);
  const isSaved = State.savedPostIds.has(post.id);
  const visibilityClass = (post.visibility || "PUBLIC").toLowerCase();
  const authorAvatar = getAvatarUrl(post.authorUsername);
  const mediaUrl = getPostImageUrl(post.imageUrl, post.id);

  // Format hashtags
  const formattedCaption = (post.caption || "").replace(
    /(#\w+)/g,
    '<span class="hashtag">$1</span>'
  );

  return `
    <article class="post-card" id="post-${post.id}" data-post-id="${post.id}">
      <!-- Post Header -->
      <header class="post-header">
        <div class="post-header-author">
          <div class="post-avatar-ring">
            <img class="post-author-avatar" src="${authorAvatar}" alt="${post.authorUsername}" />
          </div>
          <div class="post-author-meta">
            <div class="post-author-name">
              <span>${post.authorUsername}</span>
              ${post.authorUsername === "cuong_admin" || post.authorUsername === "mod_alice" ? Icons.verified("icon") : ""}
            </div>
            <span class="post-timestamp">Post #${post.id}</span>
          </div>
        </div>

        <div class="post-header-actions">
          <span class="post-visibility-badge ${visibilityClass}">
            ${post.visibility === "PRIVATE" ? Icons.lock("icon") : post.visibility === "FOLLOWERS_ONLY" ? Icons.user("icon") : Icons.globe("icon")}
            ${post.visibility || "PUBLIC"}
          </span>

          ${canDelete ? `
            <button class="action-btn delete-post-btn" data-post-id="${post.id}" title="${isOwner ? 'Delete your post' : 'Delete as Staff (POST_DELETE_ANY)'}">
              ${Icons.trash("icon")}
            </button>
          ` : ""}
        </div>
      </header>

      <!-- Post Media with Double-Tap Burst -->
      <div class="post-media-wrap" data-post-id="${post.id}">
        <img class="post-image" src="${mediaUrl}" alt="Post by ${post.authorUsername}" loading="lazy" />
        <div class="heart-burst">${Icons.heartFilled()}</div>
      </div>

      <!-- Action Buttons -->
      <div class="post-actions">
        <div class="post-actions-group">
          <button class="action-btn like-btn ${isLiked ? "liked" : ""}" data-post-id="${post.id}" aria-label="${isLiked ? "Unlike" : "Like"}">
            ${isLiked ? Icons.heartFilled() : Icons.heart()}
          </button>
          <button class="action-btn comment-btn" data-post-id="${post.id}" aria-label="Comment">
            ${Icons.comment()}
          </button>
          <button class="action-btn share-btn" data-post-id="${post.id}" aria-label="Share">
            ${Icons.share()}
          </button>
        </div>

        <button class="action-btn bookmark-btn ${isSaved ? "saved" : ""}" data-post-id="${post.id}" aria-label="${isSaved ? "Remove bookmark" : "Save post"}">
          ${isSaved ? Icons.bookmarkFilled() : Icons.bookmark()}
        </button>
      </div>

      <!-- Details & Caption -->
      <div class="post-details">
        <div class="post-likes-count" id="likes-count-${post.id}">
          ${(isLiked ? 1 : 0) + (Number(post.id) * 7 + 12)} likes
        </div>

        <div class="post-caption">
          <span class="caption-username">${post.authorUsername}</span>
          ${formattedCaption}
        </div>

        <div class="post-comments-list" id="comments-${post.id}">
          <div class="comment-row">
            <span class="comment-author">sec_bot</span>
            <span>Protected by Spring Security 7.x FilterChain</span>
          </div>
        </div>
      </div>

      <!-- Inline Comment Input -->
      <form class="post-comment-form" data-post-id="${post.id}">
        <input type="text" class="post-comment-input" placeholder="Add a comment..." required />
        <button type="submit" class="post-comment-submit">Post</button>
      </form>
    </article>
  `;
}

function attachPostCardListeners(container) {
  // Double-tap or double-click to like
  container.querySelectorAll(".post-media-wrap").forEach(media => {
    let lastTap = 0;
    const triggerLike = () => {
      const postId = Number(media.getAttribute("data-post-id"));
      const burst = media.querySelector(".heart-burst");
      burst?.classList.remove("animate");
      void burst?.offsetWidth; // trigger reflow
      burst?.classList.add("animate");

      if (!State.likedPostIds.has(postId)) {
        State.toggleLike(postId);
        const card = document.querySelector(`#post-${postId}`);
        const likeBtn = card?.querySelector(".like-btn");
        likeBtn?.classList.add("liked");
        if (likeBtn) likeBtn.innerHTML = Icons.heartFilled();
        const likesCount = card?.querySelector(`#likes-count-${postId}`);
        if (likesCount) {
          const count = (Number(postId) * 7 + 13);
          likesCount.textContent = `${count} likes`;
        }
      }
    };

    media.addEventListener("dblclick", triggerLike);
    media.addEventListener("touchend", (e) => {
      const now = Date.now();
      if (now - lastTap < 300) {
        triggerLike();
      }
      lastTap = now;
    });
  });

  // Like button click
  container.querySelectorAll(".like-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const postId = Number(btn.getAttribute("data-post-id"));
      State.toggleLike(postId);
      const isLiked = State.likedPostIds.has(postId);
      btn.classList.toggle("liked", isLiked);
      btn.innerHTML = isLiked ? Icons.heartFilled() : Icons.heart();

      const countElem = container.querySelector(`#likes-count-${postId}`);
      if (countElem) {
        const base = Number(postId) * 7 + 12;
        countElem.textContent = `${base + (isLiked ? 1 : 0)} likes`;
      }
    });
  });

  // Bookmark button click
  container.querySelectorAll(".bookmark-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const postId = Number(btn.getAttribute("data-post-id"));
      State.toggleBookmark(postId);
      const isSaved = State.savedPostIds.has(postId);
      btn.classList.toggle("saved", isSaved);
      btn.innerHTML = isSaved ? Icons.bookmarkFilled() : Icons.bookmark();
      Toast.info(isSaved ? "Post saved to your bookmarks" : "Post removed from bookmarks");
    });
  });

  // Share button click
  container.querySelectorAll(".share-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const postId = btn.getAttribute("data-post-id");
      const url = `${window.location.origin}/#post-${postId}`;
      navigator.clipboard?.writeText(url);
      Toast.success("Post link copied to clipboard!");
    });
  });

  // Focus comment input
  container.querySelectorAll(".comment-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const postId = btn.getAttribute("data-post-id");
      const input = container.querySelector(`.post-comment-form[data-post-id="${postId}"] input`);
      input?.focus();
    });
  });

  // Add Comment Form Submit
  container.querySelectorAll(".post-comment-form").forEach(form => {
    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const postId = Number(form.getAttribute("data-post-id"));
      const input = form.querySelector(".post-comment-input");
      const text = input.value.trim();
      if (!text) return;

      try {
        await api.addComment(postId, text);
        const list = container.querySelector(`#comments-${postId}`);
        if (list) {
          const row = document.createElement("div");
          row.className = "comment-row";
          row.innerHTML = `
            <span class="comment-author">${State.user ? State.user.username : "guest"}</span>
            <span>${text}</span>
          `;
          list.appendChild(row);
        }
        input.value = "";
        Toast.success("Comment posted!");
      } catch (err) {
        Toast.danger(`Comment failed: ${err.message}`);
      }
    });
  });

  // Delete Post
  container.querySelectorAll(".delete-post-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const postId = Number(btn.getAttribute("data-post-id"));
      if (!confirm(`Are you sure you want to delete Post #${postId}?`)) return;

      btn.classList.add("loading");
      try {
        await api.deletePost(postId);
        Toast.success(`Post #${postId} deleted successfully.`);
        const card = container.querySelector(`#post-${postId}`);
        if (card) {
          card.classList.add("deleting");
          setTimeout(() => card.remove(), 250);
        }
      } catch (err) {
        Toast.danger(`Delete error: ${err.message}`);
        btn.classList.remove("loading");
      }
    });
  });
}

export function renderFeedSidebar(container) {
  if (!container) return;
  const user = State.user;
  const username = user ? user.username : "guest";
  const userRoles = user ? (user.roles || []).join(", ") : "Public Visitor";
  const userAvatar = getAvatarUrl(username);

  const suggestions = [
    { id: "alice", name: "Alice", desc: "Demo User • Spring Sec 7.x", avatar: getAvatarUrl("alice") },
    { id: "bob", name: "Bob", desc: "BOLA & IDOR Tester", avatar: getAvatarUrl("bob") },
    { id: "charlie", name: "Charlie", desc: "CSRF XOR Security", avatar: getAvatarUrl("charlie") },
    { id: "mod_alice", name: "Mod Alice", desc: "Staff Moderator", avatar: getAvatarUrl("mod_alice") },
    { id: "cuong_admin", name: "Admin Cuong", desc: "Full Admin Lead", avatar: getAvatarUrl("cuong_admin") }
  ].filter(s => s.id !== username);

  container.innerHTML = `
    <!-- Current User Mini Card -->
    <div class="feed-sidebar-user">
      <img class="feed-sidebar-user-avatar" src="${userAvatar}" alt="${username}" />
      <div class="feed-sidebar-user-info">
        <span class="feed-sidebar-user-name">${username}</span>
        <span class="feed-sidebar-user-role">${userRoles}</span>
      </div>
      <button class="feed-sidebar-switch-btn" id="feedSidebarSwitchBtn">
        Switch
      </button>
    </div>

    <!-- Suggestions Header -->
    <div class="feed-sidebar-heading">
      <span>Suggested for you</span>
      <button id="feedSidebarSeeAllBtn" class="feed-sidebar-see-all">
        See All
      </button>
    </div>

    <!-- Suggestions List -->
    <div class="feed-sidebar-suggestions">
      ${suggestions.map(s => `
        <div class="feed-suggestion-item">
          <img class="feed-suggestion-avatar" src="${s.avatar}" alt="${s.name}" />
          <div class="feed-suggestion-info">
            <span class="feed-suggestion-name">${s.name}</span>
            <span class="feed-suggestion-desc">${s.desc}</span>
          </div>
          <button class="feed-suggestion-action-btn" data-switch-username="${s.id}">
            Switch
          </button>
        </div>
      `).join("")}
    </div>

    <!-- Spring Security Status Card -->
    <div class="feed-sidebar-security-badge">
      <div class="security-badge-header">
        ${Icons.shield("icon")}
        <span>Zero Trust Defense Active</span>
      </div>
      <div class="security-badge-body">
        Filter Chain: <strong>Chain ${State.telemetry.lastActiveChain}</strong> • CSP: <strong>default-src 'self'</strong> • CSRF: <strong>XOR Masked</strong>
      </div>
    </div>

    <!-- Footer Copyright & Links -->
    <footer class="feed-sidebar-footer">
      <div class="feed-sidebar-links">
        <a href="#about">About</a> •
        <a href="#help">Help</a> •
        <a href="#privacy">Privacy</a> •
        <a href="#terms">Terms</a> •
        <a href="#security">Spring 7.x</a>
      </div>
      <div class="feed-sidebar-copy">
        © 2026 INSTACLONE FROM SPRING SECURITY &amp; BOOT 4.0
      </div>
    </footer>
  `;

  // Bind clicks
  container.querySelectorAll("[data-switch-username]").forEach(btn => {
    btn.addEventListener("click", async () => {
      const u = btn.getAttribute("data-switch-username");
      btn.classList.add("loading");
      try {
        await api.loginSession(u, "Password123!");
        Toast.success(`Switched account to @${u}`);
        await api.getFeedPosts();
        renderFeed(document.querySelector("#feedMount"));
      } catch (err) {
        Toast.danger(`Switch failed: ${err.message}`);
      } finally {
        btn.classList.remove("loading");
      }
    });
  });

  container.querySelector("#feedSidebarSwitchBtn")?.addEventListener("click", () => {
    document.querySelector("#authModalBackdrop")?.classList.add("open");
  });
}
