package com.cvgen.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity — Users table.
 * Stores authentication credentials (password is hashed via Pbkdf2).
 * One user owns many CVs.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@NamedQueries({
    @NamedQuery(name = "User.findByUsername",
                query = "SELECT u FROM User u WHERE u.username = :username"),
    @NamedQuery(name = "User.findByEmail",
                query = "SELECT u FROM User u WHERE u.email = :email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /**
     * Stored as a Pbkdf2PasswordHash encoded string.
     * Never store plain-text passwords.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Application role (USER or ADMIN).
     * Used by Jakarta Security identity store for authorization.
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role = "USER";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CV> cvs = new ArrayList<>();

    // ─── Constructors ────────────────────────────────────────────────

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<CV> getCvs() { return cvs; }
    public void setCvs(List<CV> cvs) { this.cvs = cvs; }

    // ─── Helpers ─────────────────────────────────────────────────────

    public void addCV(CV cv) {
        cvs.add(cv);
        cv.setUser(this);
    }

    public void removeCV(CV cv) {
        cvs.remove(cv);
        cv.setUser(null);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "', role='" + role + "'}";
    }
}
