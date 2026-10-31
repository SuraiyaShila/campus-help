package com.bup.campus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_requests")
public class ClaimRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long itemId;
    private String itemName;
    private String itemType;
    private String claimerId;
    private String claimerName;

    @Column(length = 1000)
    private String note;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ClaimRequest() {}

    public ClaimRequest(Long itemId, String itemName, String itemType,
                        String claimerId, String claimerName, String note) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.claimerId = claimerId;
        this.claimerName = claimerName;
        this.note = note;
        this.status = "Pending";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getItemType() { return itemType; }
    public String getClaimerId() { return claimerId; }
    public String getClaimerName() { return claimerName; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt == null ? createdAt : updatedAt; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}