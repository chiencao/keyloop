package com.keyloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A confirmed booking that reserves a technician and a service bay for a
 * contiguous window. Times are dealership-local wall-clock (LocalDateTime).
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private Dealership dealership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_bay_id", nullable = false)
    private ServiceBay serviceBay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Appointment() {
    }

    public Appointment(Dealership dealership, Customer customer, Vehicle vehicle, Technician technician,
                       ServiceBay serviceBay, ServiceType serviceType,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.dealership = dealership;
        this.customer = customer;
        this.vehicle = vehicle;
        this.technician = technician;
        this.serviceBay = serviceBay;
        this.serviceType = serviceType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Dealership getDealership() {
        return dealership;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Technician getTechnician() {
        return technician;
    }

    public ServiceBay getServiceBay() {
        return serviceBay;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    /** Technician sign-off: work is done, which releases the technician and bay for the window. */
    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }
}
