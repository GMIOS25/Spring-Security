/**
 * Create Post Modal Component
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";

export function renderCreateModal(container) {
  const imagePresets = [
    { label: "Tokyo Sunset Skyline", url: "/images/posts/p1.svg" },
    { label: "Cozy Dev Workspace", url: "/images/posts/p2.svg" },
    { label: "Alpine Mountain Horizon", url: "/images/posts/p3.svg" },
    { label: "Cyberpunk Neo-Shinjuku", url: "/images/posts/p4.svg" },
    { label: "Abstract Modern Art", url: "/images/posts/default.svg" },
    { label: "Zero Trust Architecture", url: "/images/explore/explore-1.svg" }
  ];

  container.innerHTML = `
    <div id="createModalBackdrop" class="modal-backdrop" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="createModalTitle">
      <div class="modal-dialog create-modal-dialog">
        <!-- Modal Header -->
        <div class="modal-header">
          <h2 id="createModalTitle" class="modal-title">Create New Post</h2>
          <button id="closeCreateModalBtn" class="modal-close-btn" aria-label="Close modal">
            ${Icons.close("icon")}
          </button>
        </div>

        <!-- Modal Body -->
        <form id="createPostForm" class="modal-body">
          <!-- Preset Image Selector -->
          <div class="form-group">
            <label class="form-label">
              Choose Curated Photo or Enter Custom Asset
            </label>
            <div class="image-presets-grid" role="group" aria-label="Preset images">
              ${imagePresets.map((p, idx) => `
                <button type="button" class="preset-chip ${idx === 0 ? "selected" : ""}" data-url="${p.url}" aria-label="${p.label}">
                  <img src="${p.url}" alt="${p.label}" />
                </button>
              `).join("")}
            </div>
          </div>

          <!-- Image URL Input -->
          <div class="form-group">
            <label for="postImageUrlInput" class="form-label">
              Image URL or Local Asset Path
            </label>
            <input type="text" id="postImageUrlInput" value="${imagePresets[0].url}" placeholder="/images/posts/p1.svg" required />
          </div>

          <!-- Live Preview Box -->
          <div class="create-post-preview-box">
            <img id="postPreviewImg" src="${imagePresets[0].url}" alt="Post Preview" class="create-post-preview-img" />
          </div>

          <!-- Caption & Hashtags -->
          <div class="form-group">
            <div class="caption-label-row">
              <label for="postCaptionInput" class="form-label">Caption</label>
              <span id="charCounter" class="char-counter">0 / 500</span>
            </div>
            <textarea id="postCaptionInput" rows="3" placeholder="Write a caption... Use #hashtags to tag topics!" maxlength="500" required></textarea>
            <div class="hashtag-pills-row">
              <button type="button" class="hashtag-pill" data-tag="#SpringSecurity7">#SpringSecurity7</button>
              <button type="button" class="hashtag-pill" data-tag="#SpringBoot4">#SpringBoot4</button>
              <button type="button" class="hashtag-pill" data-tag="#ZeroTrust">#ZeroTrust</button>
              <button type="button" class="hashtag-pill" data-tag="#Developer">#Developer</button>
            </div>
          </div>

          <!-- Visibility Select -->
          <div class="form-group">
            <label for="postVisibilitySelect" class="form-label">
              Resource Visibility (Spring Security RBAC)
            </label>
            <select id="postVisibilitySelect">
              <option value="PUBLIC">PUBLIC — Anyone can view (Chain 1 &amp; 3)</option>
              <option value="FOLLOWERS_ONLY">FOLLOWERS_ONLY — Protected follower access</option>
              <option value="PRIVATE">PRIVATE — Owner &amp; Staff only (BOLA Protected)</option>
            </select>
          </div>
        </form>

        <!-- Modal Footer -->
        <div class="modal-footer">
          <button type="button" id="cancelCreateBtn" class="btn-cancel">
            Cancel
          </button>
          <button type="submit" form="createPostForm" id="submitCreateBtn" class="btn-submit-post">
            Share Post
          </button>
        </div>
      </div>
    </div>
  `;

  // Bind Preset Chips
  container.querySelectorAll(".preset-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      container.querySelectorAll(".preset-chip").forEach(c => c.classList.remove("selected"));
      chip.classList.add("selected");
      const url = chip.getAttribute("data-url");
      const input = container.querySelector("#postImageUrlInput");
      const preview = container.querySelector("#postPreviewImg");
      if (input) input.value = url;
      if (preview) preview.src = url;
    });
  });

  // URL Input Live Preview
  const urlInput = container.querySelector("#postImageUrlInput");
  const previewImg = container.querySelector("#postPreviewImg");
  urlInput?.addEventListener("input", (e) => {
    const val = e.target.value.trim();
    if (val && previewImg) {
      previewImg.src = val;
    }
  });

  // Hashtag Quick Insertion
  const captionInput = container.querySelector("#postCaptionInput");
  const charCounter = container.querySelector("#charCounter");

  captionInput?.addEventListener("input", () => {
    const len = captionInput.value.length;
    if (charCounter) charCounter.textContent = `${len} / 500`;
  });

  container.querySelectorAll(".hashtag-pill").forEach(pill => {
    pill.addEventListener("click", () => {
      const tag = pill.getAttribute("data-tag");
      if (captionInput) {
        captionInput.value = captionInput.value ? `${captionInput.value} ${tag}` : tag;
        captionInput.focus();
        if (charCounter) charCounter.textContent = `${captionInput.value.length} / 500`;
      }
    });
  });

  // Close handlers
  const backdrop = container.querySelector("#createModalBackdrop");
  const closeBtn = container.querySelector("#closeCreateModalBtn");
  const cancelBtn = container.querySelector("#cancelCreateBtn");

  const closeModal = () => {
    backdrop?.classList.remove("open");
    backdrop?.setAttribute("aria-hidden", "true");
  };

  closeBtn?.addEventListener("click", closeModal);
  cancelBtn?.addEventListener("click", closeModal);
  backdrop?.addEventListener("click", (e) => {
    if (e.target === backdrop) closeModal();
  });

  // Form Submit (Create Post)
  const form = container.querySelector("#createPostForm");
  const submitBtn = container.querySelector("#submitCreateBtn");

  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const caption = captionInput.value.trim();
    const imageUrl = urlInput.value.trim();
    const visibility = container.querySelector("#postVisibilitySelect").value;

    if (!caption || !imageUrl) {
      Toast.warning("Please fill in both caption and image URL.");
      return;
    }

    submitBtn.classList.add("loading");
    submitBtn.textContent = "Publishing...";

    try {
      const newPost = await api.createPost(caption, imageUrl, visibility);
      Toast.success(`Post #${newPost.id} published successfully!`);
      closeModal();
      form.reset();
      if (charCounter) charCounter.textContent = "0 / 500";
      // Auto-reload feed
      await api.getFeedPosts();
      State.setActiveTab("feed");
    } catch (err) {
      Toast.danger(`Failed to create post: ${err.message}`);
    } finally {
      submitBtn.classList.remove("loading");
      submitBtn.textContent = "Share Post";
    }
  });
}
