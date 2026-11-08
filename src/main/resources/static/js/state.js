/**
 * InstaClone Centralized Reactive State Store
 */

const LOCAL_STORAGE_KEY_THEME = "instaclone_theme";
const LOCAL_STORAGE_KEY_JWT = "instaclone_jwt";
const LOCAL_STORAGE_KEY_SAVED = "instaclone_saved_posts";
const LOCAL_STORAGE_KEY_LIKES = "instaclone_liked_posts";

export const State = {
  // Current authenticated user profile
  user: null,

  // Authentication mode: 'session' | 'jwt' | 'anonymous'
  authMode: 'session',

  // JWT Tokens if using Stateless Chain 2
  jwt: (() => {
    try {
      const stored = localStorage.getItem(LOCAL_STORAGE_KEY_JWT);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  })(),

  // Current CSRF Token (obtained from /api/v1/csrf/token)
  csrf: null,

  // Posts in Feed
  posts: [],

  // Stories
  stories: [
    { username: "alice", label: "Alice", avatar: "/images/avatars/alice.svg", seen: false, image: "/images/stories/story-1.svg", caption: "Deep dive into Spring Security 7.x!" },
    { username: "bob", label: "Bob", avatar: "/images/avatars/bob.svg", seen: false, image: "/images/stories/story-2.svg", caption: "Testing IDOR and BOLA boundaries." },
    { username: "charlie", label: "Charlie", avatar: "/images/avatars/charlie.svg", seen: false, image: "/images/stories/story-3.svg", caption: "CSRF XOR Token encryption is slick." },
    { username: "mod_alice", label: "Moderator", avatar: "/images/avatars/mod_alice.svg", seen: false, image: "/images/stories/story-4.svg", caption: "Queue cleared! 0 pending reports." },
    { username: "cuong_admin", label: "Cuong", avatar: "/images/avatars/cuong_admin.svg", seen: false, image: "/images/stories/story-5.svg", caption: "Spring Boot 4.0 & Java 21 live in production!" }
  ],

  // Saved Bookmarks
  savedPostIds: (() => {
    try {
      const stored = localStorage.getItem(LOCAL_STORAGE_KEY_SAVED);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch {
      return new Set();
    }
  })(),

  // Local likes tracking
  likedPostIds: (() => {
    try {
      const stored = localStorage.getItem(LOCAL_STORAGE_KEY_LIKES);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch {
      return new Set();
    }
  })(),

  // Active navigation tab: 'feed' | 'explore' | 'moderation' | 'profile'
  activeTab: 'feed',

  // Theme: 'dark' | 'light'
  theme: localStorage.getItem(LOCAL_STORAGE_KEY_THEME) || 'dark',

  // Moderation reports
  moderationReports: [],

  // Live Security Telemetry Data
  telemetry: {
    lastActiveChain: 3,
    lastRequest: null,
    lastResponse: null,
    rateLimitRemaining: 60,
    csrfMasked: null,
    jwtPayload: null,
    bolaTestResult: null
  },

  // Event listeners for reactive UI updates
  listeners: new Set(),

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  },

  notify(event, data) {
    for (const listener of this.listeners) {
      try {
        listener(event, data);
      } catch (err) {
        console.error("State listener error:", err);
      }
    }
  },

  setTheme(theme) {
    this.theme = theme;
    localStorage.setItem(LOCAL_STORAGE_KEY_THEME, theme);
    document.documentElement.setAttribute("data-theme", theme);
    this.notify("theme:change", theme);
  },

  setUser(user) {
    this.user = user;
    this.notify("user:change", user);
  },

  setAuthMode(mode) {
    this.authMode = mode;
    this.notify("authMode:change", mode);
  },

  setJwt(jwt) {
    this.jwt = jwt;
    if (jwt) {
      localStorage.setItem(LOCAL_STORAGE_KEY_JWT, JSON.stringify(jwt));
    } else {
      localStorage.removeItem(LOCAL_STORAGE_KEY_JWT);
    }
    this.notify("jwt:change", jwt);
  },

  setCsrf(csrf) {
    this.csrf = csrf;
    this.telemetry.csrfMasked = csrf ? csrf.token : null;
    this.notify("csrf:change", csrf);
  },

  setPosts(posts) {
    this.posts = posts;
    this.notify("posts:change", posts);
  },

  addPost(post) {
    this.posts.unshift(post);
    this.notify("posts:change", this.posts);
  },

  removePost(postId) {
    this.posts = this.posts.filter(p => p.id !== postId);
    this.notify("posts:change", this.posts);
  },

  setActiveTab(tab) {
    this.activeTab = tab;
    this.notify("tab:change", tab);
  },

  toggleBookmark(postId) {
    if (this.savedPostIds.has(postId)) {
      this.savedPostIds.delete(postId);
    } else {
      this.savedPostIds.add(postId);
    }
    localStorage.setItem(LOCAL_STORAGE_KEY_SAVED, JSON.stringify([...this.savedPostIds]));
    this.notify("saved:change", this.savedPostIds);
  },

  toggleLike(postId) {
    const isLiked = this.likedPostIds.has(postId);
    if (isLiked) {
      this.likedPostIds.delete(postId);
    } else {
      this.likedPostIds.add(postId);
    }
    localStorage.setItem(LOCAL_STORAGE_KEY_LIKES, JSON.stringify([...this.likedPostIds]));
    this.notify("like:change", { postId, liked: !isLiked });
  },

  updateTelemetry(update) {
    Object.assign(this.telemetry, update);
    this.notify("telemetry:change", this.telemetry);
  }
};
