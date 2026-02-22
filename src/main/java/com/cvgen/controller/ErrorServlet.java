package com.cvgen.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Error page servlet.
 * Handles /error/404 and /error/500.
 */
@WebServlet(name = "ErrorServlet", urlPatterns = "/error/*")
public class ErrorServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // /404 or /500
        if (pathInfo == null)
            pathInfo = "/500";

        switch (pathInfo) {
            case "/404" -> {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.setAttribute("errorCode", "404");
                req.setAttribute("errorMessage", "Page Not Found");
                req.setAttribute("errorDetail", "The page you are looking for doesn't exist or has been moved.");
            }
            default -> {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                req.setAttribute("errorCode", "500");
                req.setAttribute("errorMessage", "Internal Server Error");
                req.setAttribute("errorDetail", "Something went wrong on our end. Please try again later.");
            }
        }
        render("error", req, resp);
    }
}
