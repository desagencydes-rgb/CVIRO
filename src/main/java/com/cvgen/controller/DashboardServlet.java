package com.cvgen.controller;

import com.cvgen.model.CV;
import com.cvgen.model.User;
import com.cvgen.service.CVService;
import com.cvgen.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Dashboard servlet — displays a logged-in user's CV list.
 *
 * URL: /dashboard
 * GET → list CVs
 * POST → handle delete action (HTML forms only support GET/POST)
 */
@WebServlet(name = "DashboardServlet", urlPatterns = "/dashboard")
public class DashboardServlet extends BaseServlet {

    private final CVService cvService = new CVService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = resolveUser(req);
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        List<CV> cvs = cvService.getCVsForUser(user);
        req.setAttribute("cvs", cvs);
        req.setAttribute("user", user);

        if ("true".equals(req.getParameter("deleted"))) {
            req.setAttribute("successMessage", "Resume deleted successfully.");
        }
        if ("true".equals(req.getParameter("created"))) {
            req.setAttribute("successMessage", "New resume created! Fill in your details below.");
        }

        render("dashboard", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = resolveUser(req);
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        String action = req.getParameter("_action");

        if ("delete".equals(action)) {
            String cvIdParam = req.getParameter("cvId");
            try {
                long cvId = Long.parseLong(cvIdParam);
                cvService.deleteCV(cvId, user);
                resp.sendRedirect(req.getContextPath() + "/dashboard?deleted=true");
            } catch (NumberFormatException e) {
                resp.sendRedirect(req.getContextPath() + "/dashboard");
            }
        } else {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }

    /**
     * Resolves the currently authenticated User entity from the principal name.
     */
    private User resolveUser(HttpServletRequest req) {
        String username = getAuthenticatedUser(req);
        if (username == null)
            return null;

        // Cache in session to avoid repeated DB lookups
        User cached = null;
        if (req.getSession(false) != null) {
            cached = (User) req.getSession().getAttribute("currentUser");
        }
        if (cached != null && cached.getUsername().equals(username))
            return cached;

        Optional<User> user = userService.findByUsername(username);
        user.ifPresent(u -> req.getSession(true).setAttribute("currentUser", u));
        return user.orElse(null);
    }
}
