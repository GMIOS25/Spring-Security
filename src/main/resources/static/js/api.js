/**
 * InstaClone Unified API Client
 * Spring Security 7.x Compliant (Session + XOR CSRF & Stateless JWT Bearer)
 */
import { State } from "./state.js";

class ApiClient {
  constructor() {
    this.csrfPromise = null;
  }

  /**
   * Determine which Spring Security Filter Chain handles this request
   */
  classifyChain(method, url, hasBearer) {
    const path = url.split("?")[0];
    if (method.toUpperCase() === "GET" && path.startsWith("/api/v1/feed/public/")) {
      return 1; // Chain 1: Public Feed (Stateless, permitAll)
    }
    if (
      hasBearer ||
      path === "/api/v1/auth/register" ||
      path === "/api/v1/auth/token" ||
      path === "/api/v1/auth/refresh"
    ) {
      return 2; // Chain 2: Stateless JWT API
    }
    return 3; // Chain 3: Web Session & Form (CSRF Protected)
  }

  /**
   * Ensure fresh CSRF Token is cached
   */
  async ensureCsrfToken() {
    if (State.csrf && State.csrf.token) {
      return State.csrf;
    }
    if (!this.csrfPromise) {
      this.csrfPromise = (async () => {
        try {
          const res = await fetch("/api/v1/csrf/token", {
            method: "GET",
            credentials: "same-origin"
          });
          if (res.ok) {
            const data = await res.json();
            State.setCsrf(data);
            return data;
          }
        } catch (err) {
          console.warn("CSRF token fetch failed:", err);
        } finally {
          this.csrfPromise = null;
        }
        return null;
      })();
    }
    return this.csrfPromise;
  }

  /**
   * Core request dispatcher with telemetry instrumentation
   */
  async request(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});

    let hasBearer = false;
    if (State.authMode === "jwt" && State.jwt && State.jwt.accessToken) {
      headers.set("Authorization", `Bearer ${State.jwt.accessToken}`);
      hasBearer = true;
    }

    // Attach CSRF token for state-changing requests when in Session mode
    const isMutating = ["POST", "PUT", "DELETE", "PATCH"].includes(method);
    if (!hasBearer && isMutating) {
      const csrf = await this.ensureCsrfToken();
      if (csrf && csrf.token) {
        headers.set(csrf.headerName || "X-CSRF-TOKEN", csrf.token);
      }
    }

    if (!headers.has("Content-Type") && options.body && typeof options.body === "string") {
      headers.set("Content-Type", "application/json");
    }

    const chain = this.classifyChain(method, url, hasBearer);

    const startTime = performance.now();
    try {
      const response = await fetch(url, {
        ...options,
        method,
        headers,
        credentials: "same-origin"
      });

      const durationMs = Math.round(performance.now() - startTime);

      // Record telemetry
      State.updateTelemetry({
        lastActiveChain: chain,
        lastRequest: {
          method,
          url,
          chain,
          hasBearer,
          timestamp: new Date().toLocaleTimeString()
        },
        lastResponse: {
          status: response.status,
          statusText: response.statusText,
          durationMs
        }
      });

      // Handle automatic JWT refresh on 401 if refreshToken is available
      if (response.status === 401 && hasBearer && State.jwt && State.jwt.refreshToken) {
        const refreshed = await this.refreshToken();
        if (refreshed) {
          headers.set("Authorization", `Bearer ${State.jwt.accessToken}`);
          return fetch(url, { ...options, method, headers, credentials: "same-origin" });
        }
      }

      return response;
    } catch (err) {
      console.error(`API Error on ${method} ${url}:`, err);
      throw err;
    }
  }

  // ------------------------------------------------------------------------
  // Authentication & Session Endpoints
  // ------------------------------------------------------------------------

  async loginSession(username, password) {
    await this.ensureCsrfToken();
    const res = await this.request("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      let errorMsg = "Login failed";
      try {
        const data = await res.json();
        errorMsg = data.message || data.error || errorMsg;
      } catch {}
      throw new Error(errorMsg);
    }
    State.setAuthMode("session");
    State.setJwt(null);
    // Invalidate pre-login CSRF token to pick up new authenticated session CSRF token
    State.setCsrf(null);
    await this.ensureCsrfToken();
    await this.getCurrentUser();
    return res;
  }

  async loginJwt(username, password) {
    const res = await this.request("/api/v1/auth/token", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      let errorMsg = "Failed to issue JWT token";
      try {
        const data = await res.json();
        errorMsg = data.message || data.error || errorMsg;
      } catch {}
      throw new Error(errorMsg);
    }
    const data = await res.json();
    State.setJwt(data);
    State.setAuthMode("jwt");
    await this.getCurrentUser();
    return res;
  }

  async refreshToken() {
    if (!State.jwt || !State.jwt.refreshToken) return false;
    try {
      const res = await fetch("/api/v1/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: State.jwt.refreshToken })
      });
      if (res.ok) {
        const data = await res.json();
        State.setJwt(data);
        return true;
      }
    } catch (err) {
      console.warn("JWT refresh error:", err);
    }
    State.setJwt(null);
    return false;
  }

  async logout() {
    try {
      await this.request("/logout", { method: "POST" });
    } catch (e) {
      console.warn("Server logout notification:", e);
    }
    State.setUser(null);
    State.setJwt(null);
    State.setCsrf(null);
    State.setAuthMode("session");
    await this.ensureCsrfToken();
  }

  async logoutAll() {
    const res = await this.request("/api/v1/auth/logout-all", { method: "POST" });
    State.setUser(null);
    State.setJwt(null);
    State.setCsrf(null);
    return res;
  }

  async register(username, password, email) {
    const res = await this.request("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, email })
    });
    if (!res.ok) {
      let errorMsg = "Registration failed";
      try {
        const data = await res.json();
        errorMsg = data.message || data.error || errorMsg;
      } catch {}
      throw new Error(errorMsg);
    }
    return res;
  }

  async getCurrentUser() {
    try {
      const res = await this.request("/api/v1/auth/me", { method: "GET" });
      if (res.ok) {
        const user = await res.json();
        State.setUser(user);
        return user;
      }
    } catch {
      // Unauthenticated
    }
    State.setUser(null);
    return null;
  }

  // ------------------------------------------------------------------------
  // One-Time Token (OTT) & MFA Endpoints
  // ------------------------------------------------------------------------

  async generateOtt(username) {
    await this.ensureCsrfToken();
    const params = new URLSearchParams();
    params.append("username", username);
    const res = await this.request("/ott/generate", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params.toString()
    });
    if (!res.ok) {
      throw new Error(`Failed to generate OTT token: ${res.statusText}`);
    }
    return res;
  }

  async getLatestOttToken() {
    try {
      const res = await this.request("/ott/latest", { method: "GET" });
      if (res.ok) {
        const data = await res.json();
        return data.token;
      }
    } catch {}
    return null;
  }

  async loginOtt(token) {
    await this.ensureCsrfToken();
    const params = new URLSearchParams();
    params.append("token", token);
    const res = await this.request("/login/ott", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params.toString()
    });
    if (!res.ok && res.status !== 302 && !res.redirected) {
      throw new Error("One-Time Token verification failed. Token may be invalid or expired.");
    }
    // Refresh CSRF and current user after OTT login
    State.setCsrf(null);
    await this.ensureCsrfToken();
    await this.getCurrentUser();
    return res;
  }

  async deleteMyAccount() {
    return this.request("/api/v1/users/me", { method: "DELETE" });
  }

  // ------------------------------------------------------------------------
  // Posts & Feed Endpoints
  // ------------------------------------------------------------------------

  async getPublicExplore() {
    const res = await this.request("/api/v1/feed/public/explore", { method: "GET" });
    if (res.ok) {
      return res.json();
    }
    return [];
  }

  async getFeedPosts() {
    const res = await this.request("/api/v1/posts", { method: "GET" });
    if (res.ok) {
      const posts = await res.json();
      State.setPosts(posts);
      return posts;
    }
    return [];
  }

  async getPostDetail(id) {
    return this.request(`/api/v1/posts/${id}`, { method: "GET" });
  }

  async createPost({ caption, imageUrl, visibility = "PUBLIC" }) {
    const res = await this.request("/api/v1/posts", {
      method: "POST",
      body: JSON.stringify({ caption, imageUrl, visibility })
    });
    if (res.ok) {
      const newPost = await res.json();
      State.addPost(newPost);
      return newPost;
    }
    throw new Error(`Failed to create post: ${res.statusText}`);
  }

  async deletePost(id) {
    const res = await this.request(`/api/v1/posts/${id}`, { method: "DELETE" });
    if (res.ok || res.status === 204) {
      State.removePost(id);
      return true;
    }
    throw new Error(`Delete failed: ${res.statusText}`);
  }

  async addComment(postId, text) {
    const res = await this.request(`/api/v1/posts/${postId}/comments`, {
      method: "POST",
      body: JSON.stringify({ comment: text })
    });
    return res.ok;
  }

  // ------------------------------------------------------------------------
  // Moderation Endpoints
  // ------------------------------------------------------------------------

  async getModerationReports() {
    const res = await this.request("/api/v1/moderation/reports", { method: "GET" });
    if (res.ok) {
      const reports = await res.json();
      State.moderationReports = reports;
      return reports;
    }
    return [];
  }

  // ------------------------------------------------------------------------
  // Security Demonstration: BOLA / IDOR Testing
  // ------------------------------------------------------------------------

  async testBolaAttack(postId = 2) {
    // Post 2 is Alice's PRIVATE post in seed data.
    // When Bob attempts to fetch Post 2, Spring Security's hidden resource policy
    // returns 404 Not Found rather than 403 Forbidden to prevent resource enumeration!
    try {
      const res = await this.request(`/api/v1/posts/${postId}`, { method: "GET" });
      const status = res.status;
      const data = await res.text();
      const result = {
        postId,
        status,
        success: status === 404,
        description: status === 404
          ? "Spring Security 7.x BOLA Defense SUCCESS: Returned 404 Not Found! The resource's existence is hidden from unauthorized users."
          : status === 200
          ? "Access GRANTED (You are owner or staff)."
          : `Response status: ${status}`
      };
      State.updateTelemetry({ bolaTestResult: result });
      return result;
    } catch (err) {
      return { postId, error: err.message };
    }
  }
}

export const api = new ApiClient();
