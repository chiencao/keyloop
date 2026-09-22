package com.keyloop.web;

import com.keyloop.domain.AppointmentStatus;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Back-office endpoints for a dealership service desk. No authentication in this
 * sample — the client selects a dealership (a stand-in for a staff login).
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Service-desk management of appointments")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "List a dealership's appointments (optionally by status)")
    @GetMapping("/appointments")
    public List<AppointmentResponse> list(@RequestParam Long dealershipId,
                                          @RequestParam(required = false) AppointmentStatus status) {
        return adminService.list(dealershipId, status);
    }

    @Operation(summary = "Technician sign-off — mark work done and release the slot")
    @PostMapping("/appointments/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return adminService.complete(id);
    }

    @Operation(summary = "Cancel an appointment and release the slot")
    @PostMapping("/appointments/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return adminService.cancel(id);
    }
}
