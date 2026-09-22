package com.keyloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_bay")
public class ServiceBay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @Column(nullable = false)
    private String name;

    protected ServiceBay() {
    }

    public ServiceBay(Dealership dealership, String name) {
        this.dealership = dealership;
        this.name = name;
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
}
