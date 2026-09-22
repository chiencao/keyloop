package com.keyloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "dealership")
public class Dealership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Local opening time; bookings must start at or after this. */
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    /** Local closing time; bookings must end at or before this. */
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    // Storefront attributes (populated by migration; read-only in the app).
    @Column
    private String address;

    @Column
    private Double rating;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    /** Marks the demo user's usual/preferred shop (stand-in for a per-user preference). */
    @Column
    private boolean usual;

    protected Dealership() {
    }

    public Dealership(String name, LocalTime openingTime, LocalTime closingTime) {
        this.name = name;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public String getAddress() {
        return address;
    }

    public Double getRating() {
        return rating;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean isUsual() {
        return usual;
    }
}
