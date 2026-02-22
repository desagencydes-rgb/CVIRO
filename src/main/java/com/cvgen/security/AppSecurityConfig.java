package com.cvgen.security;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Security configuration placeholder.
 * 
 * Authentication is handled directly by AuthServlet via HttpSession
 * (req.login → UserService password verify → session-based tracking).
 * 
 * Soteria @FormAuthenticationMechanismDefinition
 * and @DatabaseIdentityStoreDefinition
 * annotations are intentionally NOT used here — they conflict with Tomcat's
 * built-in authenticator and make j_security_check unreachable.
 * 
 * If you wish to enable container-managed security in the future, add
 * both annotations back AND a matching <login-config> in web.xml.
 */
@ApplicationScoped
public class AppSecurityConfig {
    // Configuration class — no implementation required.
}
