package com.blog.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "visitorId", "visitDate" })
})
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String visitorId;

    @Column(nullable = false)
    private LocalDate visitDate;

    private LocalDateTime visitTime;

    @PrePersist
    protected void onCreate() {
        if (visitTime == null) visitTime = LocalDateTime.now();
        if (visitDate == null) visitDate = visitTime.toLocalDate();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
}