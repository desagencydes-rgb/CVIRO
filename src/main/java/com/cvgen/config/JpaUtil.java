package com.cvgen.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.logging.Logger;

/**
 * Thread-safe singleton for the JPA EntityManagerFactory.
 *
 * Uses synchronized double-checked locking to guarantee only ONE factory
 * is ever created — preventing H2 file-lock conflicts on hot-redeploy.
 */
public final class JpaUtil {

    private static final Logger LOG = Logger.getLogger(JpaUtil.class.getName());
    private static final String PERSISTENCE_UNIT = "cvgen-pu";

    private static volatile EntityManagerFactory emf;
    private static final Object LOCK = new Object();

    private JpaUtil() {
    }

    /**
     * Returns the shared EntityManagerFactory, creating it on first call (lazy,
     * thread-safe).
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            synchronized (LOCK) {
                if (emf == null || !emf.isOpen()) {
                    LOG.info("[CVPro] Creating EntityManagerFactory...");
                    emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
                    LOG.info("[CVPro] EntityManagerFactory created successfully.");
                }
            }
        }
        return emf;
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
        synchronized (LOCK) {
            if (emf != null && emf.isOpen()) {
                emf.close();
                emf = null;
                LOG.info("[CVPro] EntityManagerFactory closed.");
            }
        }
    }
}
