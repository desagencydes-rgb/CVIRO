package com.cvgen.controller;

import com.cvgen.model.*;
import com.cvgen.service.CVService;
import com.cvgen.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Multi-step CV editor servlet.
 *
 * URL scheme:
 * GET/POST /editor/new → Step 1 (create new CV + save basic info)
 * GET/POST /editor/{id}/step/2 → Step 2 (work experience)
 * GET/POST /editor/{id}/step/3 → Step 3 (skills)
 * GET /editor/{id}/preview → Final preview
 */
@WebServlet(name = "CVEditorServlet", urlPatterns = "/editor/*")
public class CVEditorServlet extends BaseServlet {

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

        String pathInfo = req.getPathInfo(); // e.g. /new or /42/step/2
        if (pathInfo == null)
            pathInfo = "/new";

        if ("/new".equals(pathInfo)) {
            // Show the step-1 form for a brand new CV
            req.setAttribute("step", 1);
            req.setAttribute("cv", new CV());
            render("editor", req, resp);
            return;
        }

        // Parse /id/step/N or /id/preview
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            resp.sendError(404);
            return;
        }

        try {
            long cvId = Long.parseLong(parts[1]);
            Optional<CV> cvOpt = cvService.findById(cvId, user);
            if (cvOpt.isEmpty()) {
                resp.sendError(404, "CV not found");
                return;
            }

            CV cv = cvOpt.get();

            if (parts.length >= 4 && "step".equals(parts[2])) {
                int step = Integer.parseInt(parts[3]);
                req.setAttribute("step", step);
                req.setAttribute("cv", cv);
                render("editor", req, resp);
            } else if (parts.length >= 3 && "preview".equals(parts[2])) {
                req.setAttribute("cv", cv);
                render("preview", req, resp);
            } else {
                resp.sendError(404);
            }
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = resolveUser(req);
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null)
            pathInfo = "/new";

        if ("/new".equals(pathInfo)) {
            handleStep1New(req, resp, user);
            return;
        }

        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            resp.sendError(404);
            return;
        }

        try {
            long cvId = Long.parseLong(parts[1]);
            Optional<CV> cvOpt = cvService.findById(cvId, user);
            if (cvOpt.isEmpty()) {
                resp.sendError(404, "CV not found");
                return;
            }

            CV cv = cvOpt.get();

            if (parts.length >= 4 && "step".equals(parts[2])) {
                int step = Integer.parseInt(parts[3]);
                switch (step) {
                    case 1 -> handleStep1Update(req, resp, cv, user);
                    case 2 -> handleStep2Experience(req, resp, cv, user);
                    case 3 -> handleStep3Skills(req, resp, cv, user);
                    default -> resp.sendError(404);
                }
            } else {
                resp.sendError(404);
            }
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    // ─── Step handlers ───────────────────────────────────────────────

    /** Creates a brand-new CV and saves step-1 data in one shot. */
    private void handleStep1New(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {

        String title = req.getParameter("title");
        String fullName = req.getParameter("fullName");

        if (title == null || title.isBlank() || fullName == null || fullName.isBlank()) {
            req.setAttribute("step", 1);
            req.setAttribute("cv", new CV());
            req.setAttribute("editorError", "Title and full name are required.");
            render("editor", req, resp);
            return;
        }

        CV cv = cvService.createCV(title.trim(), fullName.trim(), user);
        applyBasicInfo(req, cv);
        cvService.updateBasicInfo(cv);

        resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/step/2");
    }

    /** Updates basic info for an existing CV (step 1 revisit). */
    private void handleStep1Update(HttpServletRequest req, HttpServletResponse resp, CV cv, User user)
            throws IOException {

        applyBasicInfo(req, cv);
        cvService.updateBasicInfo(cv);
        resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/step/2");
    }

    /**
     * Adds one experience entry and stays on step 2, or advances to step 3 via
     * "Next".
     */
    private void handleStep2Experience(HttpServletRequest req, HttpServletResponse resp, CV cv, User user)
            throws IOException {

        String action = req.getParameter("_action");

        if ("next".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/step/3");
            return;
        }

        // "add" action: persist a new experience entry
        Experience exp = new Experience();
        exp.setJobTitle(nvl(req.getParameter("jobTitle")));
        exp.setCompany(nvl(req.getParameter("company")));
        exp.setLocation(nvl(req.getParameter("location")));
        exp.setDescription(nvl(req.getParameter("description")));
        exp.setCurrent("on".equals(req.getParameter("isCurrent")));

        String startDateStr = req.getParameter("startDate");
        if (startDateStr != null && !startDateStr.isBlank()) {
            exp.setStartDate(LocalDate.parse(startDateStr));
        }
        String endDateStr = req.getParameter("endDate");
        if (!exp.isCurrent() && endDateStr != null && !endDateStr.isBlank()) {
            exp.setEndDate(LocalDate.parse(endDateStr));
        }

        if (exp.getJobTitle() != null && exp.getCompany() != null) {
            cvService.addExperience(cv.getId(), user, exp);
        }

        resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/step/2");
    }

    /** Adds one skill entry or advances to preview. */
    private void handleStep3Skills(HttpServletRequest req, HttpServletResponse resp, CV cv, User user)
            throws IOException {

        String action = req.getParameter("_action");

        if ("finish".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/preview");
            return;
        }

        String skillName = nvl(req.getParameter("skillName"));
        String levelStr = req.getParameter("skillLevel");
        String category = nvl(req.getParameter("category"));

        if (skillName != null) {
            Skill.Level level = Skill.Level.INTERMEDIATE;
            try {
                level = Skill.Level.valueOf(levelStr);
            } catch (Exception ignored) {
            }

            Skill skill = new Skill(skillName, level, null);
            skill.setCategory(category);
            cvService.addSkill(cv.getId(), user, skill);
        }

        resp.sendRedirect(req.getContextPath() + "/editor/" + cv.getId() + "/step/3");
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void applyBasicInfo(HttpServletRequest req, CV cv) {
        cv.setTitle(nvl(req.getParameter("title")));
        cv.setFullName(nvl(req.getParameter("fullName")));
        cv.setJobTitle(nvl(req.getParameter("jobTitle")));
        cv.setEmail(nvl(req.getParameter("email")));
        cv.setPhone(nvl(req.getParameter("phone")));
        cv.setLocation(nvl(req.getParameter("location")));
        cv.setLinkedinUrl(nvl(req.getParameter("linkedinUrl")));
        cv.setWebsiteUrl(nvl(req.getParameter("websiteUrl")));
        cv.setSummary(nvl(req.getParameter("summary")));
    }

    private User resolveUser(HttpServletRequest req) {
        String username = getAuthenticatedUser(req);
        if (username == null)
            return null;
        User cached = req.getSession(false) != null
                ? (User) req.getSession().getAttribute("currentUser")
                : null;
        if (cached != null && cached.getUsername().equals(username))
            return cached;
        return userService.findByUsername(username).map(u -> {
            req.getSession(true).setAttribute("currentUser", u);
            return u;
        }).orElse(null);
    }

    /** Returns null for blank/null strings. */
    private String nvl(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
