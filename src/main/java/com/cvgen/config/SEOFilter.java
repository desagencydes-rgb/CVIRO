package com.cvgen.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * SEO Filter — CVPro SEO Engine.
 *
 * Intercepts every request and injects dynamic SEO meta-tag attributes
 * (seoTitle, seoDescription, seoKeywords) into the request scope.
 *
 * Thymeleaf templates consume these via:
 * <title th:text="${seoTitle}">CVPro</title>
 * <meta name="description" th:content="${seoDescription}" />
 */
@WebFilter(filterName = "SEOFilter", urlPatterns = "/*")
public class SEOFilter implements Filter {

    /**
     * Maps URI path prefixes to SEO metadata tuples: [title, description, keywords]
     */
    private static final Map<String, String[]> SEO_MAP = Map.ofEntries(
            Map.entry("/home", new String[] {
                    "CVPro — Build Your Professional Resume Online | Free CV Generator",
                    "Create a stunning professional resume in minutes with CVPro. " +
                            "Download as PDF. Trusted by thousands of job seekers.",
                    "cv generator, resume builder, free resume, professional cv, pdf resume"
            }),
            Map.entry("/register", new String[] {
                    "Create a Free Account | CVPro Resume Builder",
                    "Sign up for CVPro and build your professional resume for free. " +
                            "Secure and fast registration.",
                    "sign up, create account, free resume builder"
            }),
            Map.entry("/login", new String[] {
                    "Sign In to CVPro | Resume Builder",
                    "Log in to CVPro to access your professional resumes and continue building your career.",
                    "login, sign in, resume builder, cv generator"
            }),
            Map.entry("/dashboard", new String[] {
                    "My Resumes Dashboard | CVPro",
                    "View, edit, and download all your professional resumes in one place.",
                    "my resumes, dashboard, cv management"
            }),
            Map.entry("/editor", new String[] {
                    "CV Editor — Build Your Resume Step by Step | CVPro",
                    "Use CVPro's step-by-step editor to craft the perfect resume with your experience, " +
                            "skills, and personal information.",
                    "cv editor, resume editor, step by step resume"
            }),
            Map.entry("/download", new String[] {
                    "Download Your Resume as PDF | CVPro",
                    "Download your professionally crafted resume as a high-quality PDF file instantly.",
                    "download resume, pdf resume, download cv"
            }));

    private static final String[] DEFAULT_SEO = {
            "CVPro — Professional CV Generator & Career Platform",
            "CVPro helps you create, manage, and download professional resumes with ease. " +
                    "Optimized for SEO and built for career success.",
            "cv generator, resume builder, professional resume, career platform"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpReq) {
            String contextPath = httpReq.getContextPath();
            String uri = httpReq.getRequestURI();

            // Strip context path to get the relative URI
            String relativePath = uri.startsWith(contextPath)
                    ? uri.substring(contextPath.length())
                    : uri;

            // Find the best matching SEO entry (longest prefix match)
            String[] seo = DEFAULT_SEO;
            int bestMatchLen = 0;
            for (Map.Entry<String, String[]> entry : SEO_MAP.entrySet()) {
                String key = entry.getKey();
                if (relativePath.startsWith(key) && key.length() > bestMatchLen) {
                    seo = entry.getValue();
                    bestMatchLen = key.length();
                }
            }

            request.setAttribute("seoTitle", seo[0]);
            request.setAttribute("seoDescription", seo[1]);
            request.setAttribute("seoKeywords", seo[2]);
        }

        chain.doFilter(request, response);
    }
}
