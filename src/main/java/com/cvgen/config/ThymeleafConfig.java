package com.cvgen.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * Thymeleaf Template Engine Configuration.
 *
 * Initializes the TemplateEngine as a ServletContext attribute so all
 * servlets can share a single cached engine instance.
 *
 * Template resolution:
 * - Prefix : /WEB-INF/templates/
 * - Suffix : .html
 * - Mode : HTML5
 * - Cache : disabled in dev (enable for prod)
 */
@WebListener
public class ThymeleafConfig implements ServletContextListener {

    /** Key used to store the TemplateEngine in the ServletContext */
    public static final String TEMPLATE_ENGINE_KEY = "templateEngine";
    public static final String WEB_APP_KEY = "thymeleafWebApp";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        // Wrap the ServletContext for Thymeleaf's Jakarta EE support
        JakartaServletWebApplication webApp = JakartaServletWebApplication.buildApplication(ctx);

        // Resolver: maps template names to /WEB-INF/templates/*.html
        WebApplicationTemplateResolver resolver = new WebApplicationTemplateResolver(webApp);
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // Set to true in production

        // Template engine wired to the resolver
        TemplateEngine engine = new TemplateEngine();
        engine.addTemplateResolver(resolver);

        // Make available to all servlets via ServletContext
        ctx.setAttribute(TEMPLATE_ENGINE_KEY, engine);
        ctx.setAttribute(WEB_APP_KEY, webApp);

        ctx.log("[CVPro] Thymeleaf TemplateEngine initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Clean up JPA resources
        JpaUtil.shutdown();
        sce.getServletContext().log("[CVPro] Application shutdown complete.");
    }
}
