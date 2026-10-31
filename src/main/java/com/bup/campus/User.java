package com.bup.campus;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String universityId;

    private String email;
    private String phone;
    private String department;
    private String role;
    private String passwordHash;

    protected User() {}

    public User(String fullName, String universityId, String email, String phone,
                String department, String role, String passwordHash) {
        this.fullName = fullName;
        this.universityId = universityId;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public String getFullName() { return fullName; }
    public String getUniversityId() { return universityId; }
    public String getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
}