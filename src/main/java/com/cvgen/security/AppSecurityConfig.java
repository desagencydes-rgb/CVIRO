package com.cvgen.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;

/**
 * Jakarta Security 3.0 Application Configuration.
 *
 * Activates:
 * 1. FormAuthenticationMechanism — form-based login via /login page
 * 2. DatabaseIdentityStore — validates credentials against the 'users' table
 *
 * NOTE: This bean requires Jersey JAX-RS on the classpath because Soteria
 * internally uses UriBuilder from JAX-RS. See pom.xml for
 * jersey-container-servlet.
 */
@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(loginPage = "/login", errorPage = "/login?error=true", useForwardToLogin = false // Use
                                                                                                                                                           // redirect
                                                                                                                                                           // (not
                                                                                                                                                           // forward)
                                                                                                                                                           // so
                                                                                                                                                           // URL
                                                                                                                                                           // changes
                                                                                                                                                           // in
                                                                                                                                                           // browser
))
@DatabaseIdentityStoreDefinition(dataSourceLookup = "java:comp/env/jdbc/cvgen", callerQuery = "SELECT password FROM users WHERE username = ?", groupsQuery = "SELECT role FROM users WHERE username = ?", hashAlgorithm = Pbkdf2PasswordHash.class, hashAlgorithmParameters = {
        "Pbkdf2PasswordHash.Iterations=2048",
        "Pbkdf2PasswordHash.Algorithm=PBKDF2WithHmacSHA256",
        "Pbkdf2PasswordHash.SaltSizeBytes=32"
})
@ApplicationScoped
public class AppSecurityConfig {
    // This class serves as the configuration carrier via annotations.
    // No implementation needed — Jakarta Security reads the annotations at startup.
}
