/**
 * Instagram Stories Component with Full-Screen Player
 */
import { State } from "../state.js";
import { Icons } from "../icons.js";

let activeStoryIndex = -1;
let storyTimer = null;

export function renderStories(container) {
  container.innerHTML = `
    <div class="stories-container" role="region" aria-label="Stories Highlights">
      ${State.stories.map((story, idx) => `
        <button class="story-item" data-story-idx="${idx}" aria-label="View story by ${story.label}">
          <div class="story-ring ${story.seen ? "seen" : ""}">
            <img class="story-avatar" src="${story.avatar}" alt="${story.label}" />
          </div>
          <span class="story-username">${story.label}</span>
        </button>
      `).join("")}
    </div>

    <!-- Story Fullscreen Viewer Modal -->
    <div id="storyViewerBackdrop" class="modal-backdrop story-viewer-backdrop" aria-hidden="true">
      <div id="storyViewerDialog" class="story-viewer-dialog">
        
        <!-- Progress Bar -->
        <div class="story-progress-track">
          <div id="storyProgressBar" class="story-progress-bar"></div>
        </div>

        <!-- Story Header -->
        <div class="story-header-overlay">
          <div class="story-header-user">
            <img id="storyHeaderAvatar" class="story-header-avatar" src="" alt="Author avatar" />
            <div class="story-header-meta">
              <div id="storyHeaderUsername" class="story-header-name"></div>
              <div class="story-header-badge">Verified Story Highlight</div>
            </div>
          </div>
          <button id="closeStoryBtn" class="story-close-btn" aria-label="Close story">
            ${Icons.close("icon")}
          </button>
        </div>

        <!-- Story Image -->
        <div class="story-media-container">
          <img id="storyViewerImg" class="story-viewer-img" src="" alt="Story media" />
          
          <!-- Clickable Tap Targets (Left = Prev, Right = Next) -->
          <div id="storyTapLeft" class="story-tap-left" role="button" aria-label="Previous story"></div>
          <div id="storyTapRight" class="story-tap-right" role="button" aria-label="Next story"></div>

          <!-- Bottom Caption Overlay -->
          <div class="story-caption-overlay">
            <p id="storyCaptionText" class="story-caption-text"></p>
          </div>
        </div>
      </div>
    </div>
  `;

  // Bind Story Item Clicks
  container.querySelectorAll(".story-item").forEach(item => {
    item.addEventListener("click", () => {
      const idx = parseInt(item.getAttribute("data-story-idx"), 10);
      openStoryViewer(idx);
    });
  });

  const viewerBackdrop = container.querySelector("#storyViewerBackdrop");
  const closeBtn = container.querySelector("#closeStoryBtn");
  const tapLeft = container.querySelector("#storyTapLeft");
  const tapRight = container.querySelector("#storyTapRight");

  closeBtn?.addEventListener("click", closeStoryViewer);
  viewerBackdrop?.addEventListener("click", (e) => {
    if (e.target === viewerBackdrop) closeStoryViewer();
  });

  tapLeft?.addEventListener("click", () => {
    if (activeStoryIndex > 0) openStoryViewer(activeStoryIndex - 1);
  });

  tapRight?.addEventListener("click", () => {
    if (activeStoryIndex < State.stories.length - 1) {
      openStoryViewer(activeStoryIndex + 1);
    } else {
      closeStoryViewer();
    }
  });
}

function openStoryViewer(idx) {
  if (idx < 0 || idx >= State.stories.length) return;
  activeStoryIndex = idx;
  const story = State.stories[idx];
  story.seen = true;

  const backdrop = document.querySelector("#storyViewerBackdrop");
  const img = document.querySelector("#storyViewerImg");
  const avatar = document.querySelector("#storyHeaderAvatar");
  const username = document.querySelector("#storyHeaderUsername");
  const caption = document.querySelector("#storyCaptionText");
  const progress = document.querySelector("#storyProgressBar");

  if (!backdrop || !img) return;

  img.src = story.image;
  avatar.src = story.avatar;
  username.textContent = story.label;
  caption.textContent = story.caption;

  backdrop.classList.add("open");
  backdrop.setAttribute("aria-hidden", "false");

  // Animate progress bar (5000ms duration via CSS keyframes)
  if (storyTimer) clearTimeout(storyTimer);
  progress.classList.remove("animating");
  void progress.offsetWidth; // Force reflow to restart CSS animation
  progress.classList.add("animating");

  storyTimer = setTimeout(() => {
    if (activeStoryIndex < State.stories.length - 1) {
      openStoryViewer(activeStoryIndex + 1);
    } else {
      closeStoryViewer();
    }
  }, 5000);
}

export function closeStoryViewer() {
  if (storyTimer) {
    clearTimeout(storyTimer);
    storyTimer = null;
  }
  const progress = document.querySelector("#storyProgressBar");
  if (progress) progress.classList.remove("animating");
  const backdrop = document.querySelector("#storyViewerBackdrop");
  backdrop?.classList.remove("open");
  backdrop?.setAttribute("aria-hidden", "true");
  activeStoryIndex = -1;
  // Re-render rings in #storiesMount to show updated seen state
  const storiesMount = document.querySelector("#storiesMount");
  if (storiesMount) renderStories(storiesMount);
}
