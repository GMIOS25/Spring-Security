/**
 * InstaClone Asset & Media Resilience Helper
 * Ensures 100% CSP-compliant and fault-tolerant media rendering
 */

const KNOWN_AVATARS = {
  alice: "/images/avatars/alice.svg",
  bob: "/images/avatars/bob.svg",
  charlie: "/images/avatars/charlie.svg",
  mod_alice: "/images/avatars/mod_alice.svg",
  cuong_admin: "/images/avatars/cuong_admin.svg"
};

const POST_FALLBACKS = [
  "/images/posts/p1.svg",
  "/images/posts/p2.svg",
  "/images/posts/p3.svg",
  "/images/posts/p4.svg"
];

/**
 * Returns a CSP-compliant local SVG avatar URL for a given username.
 */
export function getAvatarUrl(username) {
  if (!username) return "/images/avatars/default.svg";
  const clean = String(username).toLowerCase().trim();
  return KNOWN_AVATARS[clean] || "/images/avatars/default.svg";
}

/**
 * Resolves a post's image URL to a local high-res SVG or handles legacy/external URLs.
 */
export function getPostImageUrl(imageUrl, postId = 1) {
  if (!imageUrl) {
    const idx = Math.abs(Number(postId) || 0) % POST_FALLBACKS.length;
    return POST_FALLBACKS[idx];
  }

  // Handle fake legacy local domains from seeders or tutorials
  if (imageUrl.includes("instaclone.local") || imageUrl.includes("img.instaclone.com") || imageUrl.includes("images.instaclone.com")) {
    if (imageUrl.includes("p1") || imageUrl.includes("/1.jpg")) return "/images/posts/p1.svg";
    if (imageUrl.includes("p2") || imageUrl.includes("/2.jpg")) return "/images/posts/p2.svg";
    if (imageUrl.includes("p3") || imageUrl.includes("/3.jpg")) return "/images/posts/p3.svg";
    if (imageUrl.includes("p4") || imageUrl.includes("/4.jpg")) return "/images/posts/p4.svg";
    const idx = Math.abs(Number(postId) || 0) % POST_FALLBACKS.length;
    return POST_FALLBACKS[idx];
  }

  // Handle external unsplash or blocked CDN URLs by routing to local high-fidelity assets
  if (imageUrl.includes("unsplash.com") || imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
    const idx = Math.abs(Number(postId) || 0) % POST_FALLBACKS.length;
    return POST_FALLBACKS[idx];
  }

  return imageUrl;
}

/**
 * Unobtrusive global fallback listener compliant with CSP script-src 'self'
 * (Eliminates all inline onerror="..." handlers which violate CSP)
 */
export function initGlobalMediaFallbacks() {
  if (window._mediaFallbackInitialized) return;
  window._mediaFallbackInitialized = true;

  window.addEventListener(
    "error",
    (event) => {
      const target = event.target;
      if (target && target.tagName === "IMG") {
        if (target.dataset.fallbackApplied) return;
        target.dataset.fallbackApplied = "true";

        const isAvatar =
          target.classList.contains("post-author-avatar") ||
          target.classList.contains("story-avatar") ||
          target.classList.contains("sidebar-avatar") ||
          target.classList.contains("switcher-pill-avatar") ||
          target.classList.contains("feed-sidebar-user-avatar") ||
          target.classList.contains("feed-suggestion-avatar") ||
          target.classList.contains("profile-avatar") ||
          target.id === "storyHeaderAvatar" ||
          (target.alt && target.alt.toLowerCase().includes("avatar"));

        if (isAvatar) {
          target.src = "/images/avatars/default.svg";
        } else {
          target.src = "/images/posts/p1.svg";
        }
      }
    },
    true // Capture phase: image error events do not bubble
  );
}
