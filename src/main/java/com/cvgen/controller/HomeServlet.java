package com.cvgen.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Landing page servlet.
 * URL: /home (also the welcome-file default)
 */
@WebServlet(name = "HomeServlet", urlPatterns = { "/home", "" })
public class HomeServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // If user is already logged in, redirect straight to dashboard
        if (getAuthenticatedUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }

        render("home", req, resp);
    }
}
