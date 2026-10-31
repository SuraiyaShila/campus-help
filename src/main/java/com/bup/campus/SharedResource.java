package com.bup.campus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_resources")
public class SharedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String place;
    private String ownerId;
    private String ownerName;
    private String status;
    private LocalDateTime createdAt;

    protected SharedResource() {}

    public SharedResource(String name, String place, String ownerId, String ownerName) {
        this.name = name;
        this.place = place;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.status = "Available";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPlace() { return place; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}