package com.cvgen.service;

import com.cvgen.config.JpaUtil;
import com.cvgen.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for User operations:
 * - Registration with password hashing (Pbkdf2)
 * - User lookup by username / email
 */
@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    // Jakarta Security password hasher — injected via CDI if available,
    // otherwise created manually for use outside a full EE container.
    private Pbkdf2PasswordHash passwordHash;

    private Pbkdf2PasswordHash getPasswordHash() {
        if (passwordHash == null) {
            try {
                passwordHash = Pbkdf2PasswordHash.class.getConstructor().newInstance();
            } catch (Exception e) {
                // Fallback: load via CDI-provided implementation class
                try {
                    Class<?> clazz = Class.forName("org.glassfish.soteria.identitystores.hash.Pbkdf2PasswordHashImpl");
                    passwordHash = (Pbkdf2PasswordHash) clazz.getConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException("Could not instantiate Pbkdf2PasswordHash", ex);
                }
            }
            Map<String, String> params = new HashMap<>();
            params.put("Pbkdf2PasswordHash.Iterations", "2048");
            params.put("Pbkdf2PasswordHash.Algorithm", "PBKDF2WithHmacSHA256");
            params.put("Pbkdf2PasswordHash.SaltSizeBytes", "32");
            passwordHash.initialize(params);
        }
        return passwordHash;
    }

    /**
     * Verifies a plain-text password against the stored hash for a username.
     *
     * @return true if credentials are valid, false otherwise.
     */
    public boolean verifyPassword(String username, String plainPassword) {
        try {
            Optional<User> userOpt = findByUsername(username);
            if (userOpt.isEmpty()) {
                return false;
            }
            String storedHash = userOpt.get().getPassword();
            return getPasswordHash().verify(plainPassword.toCharArray(), storedHash);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Password verification failed for: " + username, e);
            return false;
        }
    }

    /**
     * Registers a new user. Hashes the password before persisting.
     *
     * @return the persisted User
     * @throws IllegalArgumentException if username or email already exists
     */
    public User register(String username, String email, String plainPassword) {

        if (findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if (findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        String hashedPassword = getPasswordHash().generate(plainPassword.toCharArray());

        User user = new User(username, email, hashedPassword);
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            LOG.info("[CVPro] User registered: " + username);
            return user;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            LOG.log(Level.SEVERE, "Error registering user: " + username, e);
            throw new RuntimeException("Registration failed", e);
        } finally {
            em.close();
        }
    }

    /**
     * Finds a user by username (used by security identity store & servlets).
     */
    public Optional<User> findByUsername(String username) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            User user = em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    /**
     * Finds a user by email address.
     */
    public Optional<User> findByEmail(String email) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            User user = em.createNamedQuery("User.findByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
}
