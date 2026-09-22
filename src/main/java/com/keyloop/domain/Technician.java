package com.keyloop.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "technician")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @Column(nullable = false)
    private String name;

    /** Competencies this technician is certified for (matched to ServiceType.requiredSkill). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "technician_skill", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill", nullable = false)
    private Set<String> skills = new HashSet<>();

    protected Technician() {
    }

    public Technician(Dealership dealership, String name, Set<String> skills) {
        this.dealership = dealership;
        this.name = name;
        this.skills = new HashSet<>(skills);
    }

    public Long getId() {
        return id;
    }

    public Dealership getDealership() {
        return dealership;
    }

    public String getName() {
        return name;
    }

    public Set<String> getSkills() {
        return skills;
    }
}
