package com.bup.campus;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lost_found_items")
public class LostFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String name;
    private String category;
    private String place;
    private String reporterId;
    private String reporterName;
    private String status;

    @Column(columnDefinition = "LONGTEXT")
    private String image;

    private LocalDateTime createdAt;

    protected LostFoundItem() {}

    public LostFoundItem(String type, String name, String category, String place,
                         String reporterId, String reporterName) {
        this.type = type;
        this.name = name;
        this.category = category;
        this.place = place;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.status = "Open";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPlace() { return place; }
    public String getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public String getStatus() { return status; }
    public String getImage() { return image; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setImage(String image) { this.image = image; }
}