package com.cvgen.controller;

import com.cvgen.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication servlet — handles Login page display, Registration, and
 * Logout.
 *
 * URL mapping:
 * GET /login → Show login form
 * GET /register → Show registration form
 * POST /register → Process registration
 * GET /logout → Invalidate session & redirect
 *
 * Note: Jakarta Security handles POST /j_security_check (form login)
 * transparently.
 * We don't need a POST /login handler.
 */
@WebServlet(name = "AuthServlet", urlPatterns = { "/login", "/register", "/logout" })
public class AuthServlet extends BaseServlet {

    private static final Logger LOG = Logger.getLogger(AuthServlet.class.getName());
    private final UserService userService = new UserService();

    // ─── GET ─────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        switch (path) {
            case "/login" -> renderLogin(req, resp);
            case "/register" -> renderRegister(req, resp);
            case "/logout" -> handleLogout(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ─── POST ────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();

        switch (path) {
            case "/login" -> handleLogin(req, resp);
            case "/register" -> handleRegistration(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    // ─── Handlers ────────────────────────────────────────────────────

    private void renderLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (getAuthenticatedUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        // ?error=true signals a failed login attempt
        if ("true".equals(req.getParameter("error"))) {
            req.setAttribute("loginError", "Invalid username or password. Please try again.");
        }
        render("login", req, resp);
    }

    /**
     * Handles POST /login — validates credentials directly via UserService
     * and tracks login state in HttpSession.
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("j_username");
        String password = req.getParameter("j_password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("loginError", "Username and password are required.");
            render("login", req, resp);
            return;
        }

        if (userService.verifyPassword(username.trim(), password)) {
            // ── Valid credentials ────────────────────────────────
            // Invalidate any old session to prevent session fixation
            if (req.getSession(false) != null) {
                req.getSession().invalidate();
            }
            // Create a fresh session and mark the user as logged in
            req.getSession(true).setAttribute("loggedInUser", username.trim());

            // Redirect to dashboard (or previously saved URL)
            String savedUrl = (String) req.getSession().getAttribute("savedRequestUrl");
            if (savedUrl != null && !savedUrl.isEmpty()) {
                req.getSession().removeAttribute("savedRequestUrl");
                resp.sendRedirect(savedUrl);
            } else {
                resp.sendRedirect(req.getContextPath() + "/dashboard");
            }
        } else {
            // ── Invalid credentials ──────────────────────────────
            req.setAttribute("loginError", "Invalid username or password. Please try again.");
            render("login", req, resp);
        }
    }

    private void renderRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (getAuthenticatedUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        render("register", req, resp);
    }

    private void handleRegistration(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        // ── Client-side validation ────────────────────────────────────
        if (username == null || username.isBlank() ||
                email == null || email.isBlank() ||
                password == null || password.isBlank()) {
            req.setAttribute("registerError", "All fields are required.");
            render("register", req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("registerError", "Passwords do not match.");
            req.setAttribute("formUsername", username);
            req.setAttribute("formEmail", email);
            render("register", req, resp);
            return;
        }

        if (password.length() < 8) {
            req.setAttribute("registerError", "Password must be at least 8 characters.");
            req.setAttribute("formUsername", username);
            req.setAttribute("formEmail", email);
            render("register", req, resp);
            return;
        }

        // ── Business logic ────────────────────────────────────────────
        try {
            userService.register(username.trim(), email.trim().toLowerCase(), password);
            // Registration succeeded → redirect to login with success message
            resp.sendRedirect(req.getContextPath() + "/login?registered=true");
        } catch (IllegalArgumentException e) {
            req.setAttribute("registerError", e.getMessage());
            req.setAttribute("formUsername", username);
            req.setAttribute("formEmail", email);
            render("register", req, resp);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) {
                msg += " | Cause: " + e.getCause().getMessage();
            }
            req.setAttribute("registerError", "Unexpected Error: " + msg);
            LOG.log(Level.SEVERE, "Registration failed", e);
            render("register", req, resp);
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        // Jakarta Security logout
        try {
            req.logout();
        } catch (Exception ignored) {
        }

        // Invalidate the HTTP session
        if (req.getSession(false) != null) {
            req.getSession().invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/home");
    }
}
