package com.keyloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;

@Entity
@Table(name = "service_type")
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** Competency a technician must hold to perform this service. */
    @Column(name = "required_skill", nullable = false)
    private String requiredSkill;

    // Storefront attributes (populated by migration).
    @Column
    private Integer price;

    @Column
    private String description;

    protected ServiceType() {
    }

    public ServiceType(String code, String name, int durationMinutes, String requiredSkill) {
        this.code = code;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredSkill = requiredSkill;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Duration getDuration() {
        return Duration.ofMinutes(durationMinutes);
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public Integer getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }
}
