package com.cvgen.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Thread-safe singleton utility for managing the JPA EntityManagerFactory.
 *
 * Usage in servlets/services:
 * EntityManager em = JpaUtil.getEntityManager();
 * try {
 * // ... operations ...
 * } finally {
 * em.close();
 * }
 */
public final class JpaUtil {

    private static final Logger LOG = Logger.getLogger(JpaUtil.class.getName());
    private static final String PERSISTENCE_UNIT = "cvgen-pu";
    private static final AtomicReference<EntityManagerFactory> EMF = new AtomicReference<>();

    private JpaUtil() {
    }

    /**
     * Returns the shared EntityManagerFactory, creating it on first call.
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        EMF.compareAndSet(null, Persistence.createEntityManagerFactory(PERSISTENCE_UNIT));
        return EMF.get();
    }

    /**
     * Opens a new EntityManager. Caller is responsible for closing it.
     */
    public static jakarta.persistence.EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Shuts down the EntityManagerFactory. Called on application context destroy.
     */
    public static void shutdown() {
        EntityManagerFactory emf = EMF.getAndSet(null);
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOG.info("[CVPro] EntityManagerFactory closed.");
        }
    }
}
