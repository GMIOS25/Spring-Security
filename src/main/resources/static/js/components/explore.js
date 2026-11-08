/**
 * Explore Grid Component with Category Filter & Post Detail Modal
 */
import { State } from "../state.js";
import { api } from "../api.js";
import { Icons } from "../icons.js";
import { Toast } from "./toast.js";

export function renderExplore(container) {
  // Dispatch request to public explore API (Spring Security 7.x Filter Chain 1: Stateless, permitAll)
  api.getPublicExplore().catch(() => {});

  const categories = ["All", "Architecture", "CyberSec", "Photography", "Design", "Spring 7.x"];

  const exploreItems = [
    { id: 101, title: "Zero Trust Architecture", likes: 342, comments: 28, category: "CyberSec", image: "/images/explore/explore-1.svg", author: "sec_engineer" },
    { id: 102, title: "Modernist Concrete Curves", likes: 512, comments: 45, category: "Architecture", image: "/images/explore/explore-2.svg", author: "arch_digest" },
    { id: 103, title: "Clean Desk & Mechanical Keyboard", likes: 890, comments: 64, category: "Design", image: "/images/explore/explore-3.svg", author: "minimal_tech" },
    { id: 104, title: "Cyberpunk Alley at Midnight", likes: 1204, comments: 98, category: "Photography", image: "/images/explore/explore-4.svg", author: "neo_tokyo" },
    { id: 105, title: "Spring Security 7.x Filter Chains", likes: 620, comments: 51, category: "Spring 7.x", image: "/images/explore/explore-5.svg", author: "cuong_admin" },
    { id: 106, title: "Abstract Light Dynamics", likes: 745, comments: 33, category: "Design", image: "/images/explore/explore-6.svg", author: "vector_lab" },
    { id: 107, title: "Coastal Horizon & Solitude", likes: 934, comments: 40, category: "Photography", image: "/images/explore/explore-7.svg", author: "wanderer" },
    { id: 108, title: "Geometric Facade Reflections", likes: 450, comments: 19, category: "Architecture", image: "/images/explore/explore-8.svg", author: "urban_lines" },
    { id: 109, title: "OAuth 2.1 PKCE Flow Visualized", likes: 1102, comments: 87, category: "Spring 7.x", image: "/images/explore/explore-9.svg", author: "alice" }
  ];

  container.innerHTML = `
    <div class="explore-container">
      <!-- Filter Chips -->
      <div class="explore-filter-chips" role="tablist" aria-label="Explore Categories">
        ${categories.map((c, i) => `
          <button class="filter-chip ${i === 0 ? "active" : ""}" data-category="${c}">
            ${c}
          </button>
        `).join("")}
      </div>

      <!-- Explore Masonry Grid -->
      <div class="explore-grid" id="exploreGrid">
        ${exploreItems.map(item => `
          <div class="explore-card" data-id="${item.id}" data-category="${item.category}" tabindex="0" role="button" aria-label="${item.title}">
            <img src="${item.image}" alt="${item.title}" loading="lazy" />
            <div class="explore-card-overlay">
              <div class="explore-stat">
                ${Icons.heartFilled()}
                <span>${item.likes}</span>
              </div>
              <div class="explore-stat">
                ${Icons.comment()}
                <span>${item.comments}</span>
              </div>
            </div>
          </div>
        `).join("")}
      </div>
    </div>

    <!-- Explore Detail Dialog -->
    <div id="exploreDetailBackdrop" class="modal-backdrop explore-detail-backdrop" aria-hidden="true">
      <div class="modal-dialog explore-detail-dialog">
        <div class="modal-header">
          <div class="explore-detail-author">
            <div id="exploreAuthorAvatar" class="explore-detail-avatar"></div>
            <span id="exploreAuthorName" class="explore-detail-name"></span>
          </div>
          <button id="closeExploreDetailBtn" class="modal-close-btn" aria-label="Close modal">
            ${Icons.close("icon")}
          </button>
        </div>
        <div class="explore-detail-body">
          <div class="explore-detail-media">
            <img id="exploreDetailImg" src="" alt="" class="explore-detail-img" />
          </div>
          <div class="explore-detail-meta-wrap">
            <h3 id="exploreDetailTitle" class="explore-detail-title"></h3>
            <p id="exploreDetailMeta" class="explore-detail-meta"></p>
            <div class="explore-detail-actions">
              <button id="exploreDetailLikeBtn" class="explore-like-btn">
                ${Icons.heart("icon")} Like
              </button>
              <button id="exploreDetailShareBtn" class="explore-share-btn">
                ${Icons.share("icon")} Share
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

  // Category Filtering
  container.querySelectorAll(".filter-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      container.querySelectorAll(".filter-chip").forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      const category = chip.getAttribute("data-category");

      container.querySelectorAll(".explore-card").forEach(card => {
        const cardCat = card.getAttribute("data-category");
        if (category === "All" || cardCat === category) {
          card.classList.remove("hidden");
        } else {
          card.classList.add("hidden");
        }
      });
    });
  });

  // Open Detail Modal
  const backdrop = container.querySelector("#exploreDetailBackdrop");
  const detailImg = container.querySelector("#exploreDetailImg");
  const detailTitle = container.querySelector("#exploreDetailTitle");
  const detailMeta = container.querySelector("#exploreDetailMeta");
  const authorName = container.querySelector("#exploreAuthorName");
  const authorAvatar = container.querySelector("#exploreAuthorAvatar");
  const closeBtn = container.querySelector("#closeExploreDetailBtn");
  const likeBtn = container.querySelector("#exploreDetailLikeBtn");
  const shareBtn = container.querySelector("#exploreDetailShareBtn");

  let activeItem = null;

  container.querySelectorAll(".explore-card").forEach(card => {
    card.addEventListener("click", () => {
      const id = parseInt(card.getAttribute("data-id"), 10);
      activeItem = exploreItems.find(x => x.id === id);
      if (!activeItem) return;

      detailImg.src = activeItem.image;
      detailTitle.textContent = activeItem.title;
      detailMeta.textContent = `${activeItem.likes} likes • ${activeItem.comments} comments • Category: ${activeItem.category}`;
      authorName.textContent = activeItem.author;
      authorAvatar.innerHTML = `<img src="/images/avatars/default.svg" alt="${activeItem.author}" class="explore-author-img" />`;

      backdrop.classList.add("open");
      backdrop.setAttribute("aria-hidden", "false");
    });
  });

  const closeModal = () => {
    backdrop.classList.remove("open");
    backdrop.setAttribute("aria-hidden", "true");
  };

  closeBtn?.addEventListener("click", closeModal);
  backdrop?.addEventListener("click", (e) => {
    if (e.target === backdrop) closeModal();
  });

  likeBtn?.addEventListener("click", () => {
    if (!activeItem) return;
    activeItem.likes += 1;
    detailMeta.textContent = `${activeItem.likes} likes • ${activeItem.comments} comments • Category: ${activeItem.category}`;
    Toast.success("Post liked!");
  });

  shareBtn?.addEventListener("click", () => {
    navigator.clipboard?.writeText(window.location.href);
    Toast.success("Link copied to clipboard!");
  });
}
