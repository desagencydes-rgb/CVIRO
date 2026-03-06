package com.cvgen.service;

import com.cvgen.config.JpaUtil;
import com.cvgen.model.CV;
import com.cvgen.model.Experience;
import com.cvgen.model.Skill;
import com.cvgen.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service layer for CV CRUD operations.
 * All mutations are scoped to the authenticated user to prevent cross-user
 * access.
 */
@ApplicationScoped
public class CVService {

    private static final Logger LOG = Logger.getLogger(CVService.class.getName());

    // ─── CV operations ───────────────────────────────────────────────

    /**
     * Returns all CVs belonging to the given user, ordered by last update DESC.
     */
    public List<CV> getCVsForUser(User user) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<CV> results = em.createNamedQuery("CV.findByUser", CV.class)
                    .setParameter("user", em.merge(user))
                    .getResultList();
            em.getTransaction().commit();
            return results;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOG.log(Level.SEVERE, "Error fetching CVs for user", e);
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Finds a CV by ID, verifying ownership.
     */
    public Optional<CV> findById(Long cvId, User owner) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<CV> results = em.createNamedQuery("CV.findByIdAndUser", CV.class)
                    .setParameter("id", cvId)
                    .setParameter("user", em.merge(owner))
                    .getResultList();

            // To prevent "Unable to access lob stream" in PostgreSQL,
            // the summary field needs to be accessed while the transaction is active.
            if (!results.isEmpty()) {
                results.get(0).getSummary();
            }

            em.getTransaction().commit();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOG.log(Level.SEVERE, "Error finding CV by id", e);
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Creates and persists a new CV for the given user.
     */
    public CV createCV(String title, String fullName, User owner) {
        CV cv = new CV(title, fullName, owner);
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User managedOwner = em.merge(owner);
            cv.setUser(managedOwner);
            em.persist(cv);
            em.getTransaction().commit();
            return cv;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            LOG.log(Level.SEVERE, "Error creating CV", e);
            throw new RuntimeException("Could not create CV", e);
        } finally {
            em.close();
        }
    }

    /**
     * Merges updated CV fields (basic info from step 1 of the editor).
     */
    public CV updateBasicInfo(CV cv) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CV merged = em.merge(cv);
            em.getTransaction().commit();
            return merged;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            throw new RuntimeException("Could not update CV", e);
        } finally {
            em.close();
        }
    }

    /**
     * Deletes a CV by ID, verifying user ownership first.
     */
    public boolean deleteCV(Long cvId, User owner) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Optional<CV> cvOpt = findById(cvId, owner);
            if (cvOpt.isEmpty())
                return false;

            em.getTransaction().begin();
            CV managed = em.merge(cvOpt.get());
            em.remove(managed);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            LOG.log(Level.SEVERE, "Error deleting CV id=" + cvId, e);
            return false;
        } finally {
            em.close();
        }
    }

    // ─── Experience operations ───────────────────────────────────────

    /**
     * Adds a new Experience to an existing CV.
     */
    public Experience addExperience(Long cvId, User owner, Experience experience) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CV cv = em.find(CV.class, cvId);
            if (cv == null || !cv.getUser().getId().equals(owner.getId())) {
                throw new SecurityException("Unauthorized access to CV id=" + cvId);
            }
            experience.setCv(cv);
            em.persist(experience);
            em.getTransaction().commit();
            return experience;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            throw new RuntimeException("Could not add experience", e);
        } finally {
            em.close();
        }
    }

    // ─── Skill operations ────────────────────────────────────────────

    /**
     * Adds a new Skill to an existing CV.
     */
    public Skill addSkill(Long cvId, User owner, Skill skill) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CV cv = em.find(CV.class, cvId);
            if (cv == null || !cv.getUser().getId().equals(owner.getId())) {
                throw new SecurityException("Unauthorized access to CV id=" + cvId);
            }
            skill.setCv(cv);
            em.persist(skill);
            em.getTransaction().commit();
            return skill;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            throw new RuntimeException("Could not add skill", e);
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves a fully loaded CV (with eagerly fetched experiences and skills).
     * Safe for use in PDF generation and preview.
     */
    public Optional<CV> getFullCV(Long cvId, User owner) {
        return findById(cvId, owner);
    }
}
