package com.cvgen.controller;

import com.cvgen.model.CV;
import com.cvgen.model.User;
import com.cvgen.service.CVService;
import com.cvgen.service.PDFGeneratorService;
import com.cvgen.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * PDF Download servlet.
 *
 * URL: GET /download/{cvId}
 * Generates a PDF for the given CV (verifying ownership) and streams it to the
 * client.
 */
@WebServlet(name = "DownloadCVServlet", urlPatterns = "/download/*")
public class DownloadCVServlet extends BaseServlet {

    private final CVService cvService = new CVService();
    private final PDFGeneratorService pdfService = new PDFGeneratorService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user = resolveUser(req);
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }

        String pathInfo = req.getPathInfo(); // e.g. /42
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "CV ID required");
            return;
        }

        try {
            long cvId = Long.parseLong(pathInfo.substring(1));
            Optional<CV> cvOpt = cvService.getFullCV(cvId, user);
            if (cvOpt.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CV not found or access denied");
                return;
            }

            CV cv = cvOpt.get();
            String safeFileName = cv.getFullName().replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_CV.pdf";

            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
            resp.setHeader("Cache-Control", "no-cache");

            pdfService.generatePDF(cv, resp.getOutputStream());

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid CV ID");
        }
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
}
