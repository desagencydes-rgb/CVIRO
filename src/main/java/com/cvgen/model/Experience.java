package com.cvgen.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA Entity — Experiences table.
 * Represents a work experience entry on a CV.
 */
@Entity
@Table(name = "experiences")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Column(name = "company", nullable = false, length = 100)
    private String company;

    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Null means "Present / Current Job"
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    private boolean current = false;

    @Column(name = "location", length = 100)
    private String location;

    @Lob
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    // ─── Constructors ────────────────────────────────────────────────

    public Experience() {}

    public Experience(String jobTitle, String company, LocalDate startDate, CV cv) {
        this.jobTitle = jobTitle;
        this.company = company;
        this.startDate = startDate;
        this.cv = cv;
    }

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CV getCv() { return cv; }
    public void setCv(CV cv) { this.cv = cv; }

    /**
     * Returns a display-friendly date range string, e.g. "Jan 2020 – Present"
     */
    public String getDateRange() {
        String start = startDate != null ? startDate.getMonth().name().substring(0, 3) + " " + startDate.getYear() : "";
        String end = current || endDate == null ? "Present" :
                endDate.getMonth().name().substring(0, 3) + " " + endDate.getYear();
        return start + " – " + end;
    }
}
