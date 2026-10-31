package com.bup.campus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_requests")
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requesterId;
    private String requesterName;
    private String type;
    private String location;
    private String urgency;

    @Column(length = 1000)
    private String description;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected EmergencyRequest() {}

    public EmergencyRequest(String requesterId, String requesterName, String type,
                            String location, String urgency, String description) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.type = type;
        this.location = location;
        this.urgency = urgency;
        this.description = description;
        this.status = "Pending";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public String getRequesterName() { return requesterName; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public String getUrgency() { return urgency; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}