package com.keyloop.dto;

/** Lightweight vehicle projection for reference-data listings (used by the UI). */
public record VehicleView(Long id, String vin, String make, String model, String customerName) {
}
