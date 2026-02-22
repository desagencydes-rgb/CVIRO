package com.cvgen.model;

import jakarta.persistence.*;

/**
 * JPA Entity — Skills table.
 * Represents a technical or soft skill on a CV, with a proficiency level.
 */
@Entity
@Table(name = "skills")
public class Skill {

    /**
     * Proficiency levels for a skill.
     */
    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Level level = Level.INTERMEDIATE;

    @Column(name = "category", length = 50)
    private String category; // e.g. "Programming", "Language", "Tools"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    // ─── Constructors ────────────────────────────────────────────────

    public Skill() {}

    public Skill(String name, Level level, CV cv) {
        this.name = name;
        this.level = level;
        this.cv = cv;
    }

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public CV getCv() { return cv; }
    public void setCv(CV cv) { this.cv = cv; }

    /**
     * Returns the proficiency level as a percentage for CSS progress bars.
     * BEGINNER=25, INTERMEDIATE=50, ADVANCED=75, EXPERT=100
     */
    public int getLevelPercent() {
        return switch (level) {
            case BEGINNER -> 25;
            case INTERMEDIATE -> 50;
            case ADVANCED -> 75;
            case EXPERT -> 100;
        };
    }
}
