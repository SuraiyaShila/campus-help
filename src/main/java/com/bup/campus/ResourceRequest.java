package com.bup.campus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_requests")
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resourceId;
    private String resourceName;
    private String ownerId;
    private String ownerName;
    private String requesterId;
    private String requesterName;

    @Column(length = 500)
    private String message;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ResourceRequest() {}

    public ResourceRequest(Long resourceId, String resourceName, String ownerId, String ownerName,
                           String requesterId, String requesterName, String message) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.message = message;
        this.status = "Pending";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getRequesterId() { return requesterId; }
    public String getRequesterName() { return requesterName; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt == null ? createdAt : updatedAt; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}