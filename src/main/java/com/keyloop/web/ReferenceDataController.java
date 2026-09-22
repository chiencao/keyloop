package com.keyloop.web;

import com.keyloop.dto.VehicleView;
import com.keyloop.repository.DealershipRepository;
import com.keyloop.repository.ServiceTypeRepository;
import com.keyloop.repository.VehicleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.List;

/**
 * Read-only reference data so clients can discover valid ids for booking.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reference data", description = "Lookup dealerships, service types and vehicles")
public class ReferenceDataController {

    private final DealershipRepository dealershipRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final VehicleRepository vehicleRepository;

    public ReferenceDataController(DealershipRepository dealershipRepository,
                                   ServiceTypeRepository serviceTypeRepository,
                                   VehicleRepository vehicleRepository) {
        this.dealershipRepository = dealershipRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public record DealershipView(Long id, String name, LocalTime openingTime, LocalTime closingTime) {
    }

    public record ServiceTypeView(Long id, String code, String name, int durationMinutes, String requiredSkill) {
    }

    @Operation(summary = "List dealerships")
    @GetMapping("/dealerships")
    public List<DealershipView> dealerships() {
        return dealershipRepository.findAll().stream()
                .map(d -> new DealershipView(d.getId(), d.getName(), d.getOpeningTime(), d.getClosingTime()))
                .toList();
    }

    @Operation(summary = "List service types")
    @GetMapping("/service-types")
    public List<ServiceTypeView> serviceTypes() {
        return serviceTypeRepository.findAll().stream()
                .map(s -> new ServiceTypeView(s.getId(), s.getCode(), s.getName(),
                        s.getDurationMinutes(), s.getRequiredSkill()))
                .toList();
    }

    @Operation(summary = "List vehicles")
    @GetMapping("/vehicles")
    public List<VehicleView> vehicles() {
        return vehicleRepository.listAll();
    }
}
