package com.cvgen.controller;

import com.cvgen.config.ThymeleafConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.io.IOException;

/**
 * Abstract base class for all CVPro servlets.
 * Provides a convenient {@code render()} method to render Thymeleaf templates.
 */
public abstract class BaseServlet extends HttpServlet {

    /**
     * Renders a Thymeleaf template to the response.
     *
     * @param templateName template path relative to /WEB-INF/templates/ (without
     *                     .html suffix)
     * @param req          the HTTP request
     * @param resp         the HTTP response
     */
    protected void render(String templateName,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException, ServletException {

        resp.setContentType("text/html;charset=UTF-8");

        TemplateEngine engine = (TemplateEngine) getServletContext()
                .getAttribute(ThymeleafConfig.TEMPLATE_ENGINE_KEY);
        JakartaServletWebApplication webApp = (JakartaServletWebApplication) getServletContext()
                .getAttribute(ThymeleafConfig.WEB_APP_KEY);

        IServletWebExchange exchange = webApp.buildExchange(req, resp);
        WebContext ctx = new WebContext(exchange, req.getLocale());

        // Make all request attributes available to templates (SEO data etc.)
        java.util.Enumeration<String> attrs = req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr = attrs.nextElement();
            ctx.setVariable(attr, req.getAttribute(attr));
        }
        // Also forward session attributes
        if (req.getSession(false) != null) {
            java.util.Enumeration<String> sessionAttrs = req.getSession().getAttributeNames();
            while (sessionAttrs.hasMoreElements()) {
                String attr = sessionAttrs.nextElement();
                ctx.setVariable(attr, req.getSession().getAttribute(attr));
            }
        }

        // Context path and URI helpers for links & active states
        ctx.setVariable("contextPath", req.getContextPath());
        ctx.setVariable("currentUri", req.getRequestURI());
        // Session-based auth: expose loggedInUser to templates (replaces principal)
        String loggedInUser = null;
        if (req.getSession(false) != null) {
            loggedInUser = (String) req.getSession().getAttribute("loggedInUser");
        }
        ctx.setVariable("loggedInUser", loggedInUser);
        // Legacy principal (for container-managed endpoints, may be null)
        ctx.setVariable("principal", req.getUserPrincipal());

        engine.process(templateName, ctx, resp.getWriter());
    }

    /**
     * Returns the authenticated username from session, or null if not logged in.
     */
    protected String getAuthenticatedUser(HttpServletRequest req) {
        if (req.getSession(false) == null)
            return null;
        return (String) req.getSession().getAttribute("loggedInUser");
    }

    /** Redirects to the login page. */
    protected void redirectToLogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
