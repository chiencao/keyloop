package com.keyloop.service;

import com.keyloop.domain.Appointment;
import com.keyloop.domain.Customer;
import com.keyloop.domain.Dealership;
import com.keyloop.domain.ServiceBay;
import com.keyloop.domain.ServiceType;
import com.keyloop.domain.Technician;
import com.keyloop.domain.Vehicle;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.BookingRequest;
import com.keyloop.exception.BusinessRuleException;
import com.keyloop.exception.NoAvailabilityException;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.repository.AppointmentRepository;
import com.keyloop.repository.DealershipRepository;
import com.keyloop.repository.ServiceBayRepository;
import com.keyloop.repository.ServiceTypeRepository;
import com.keyloop.repository.TechnicianRepository;
import com.keyloop.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the booking decision logic with all repositories mocked, so no
 * Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private DealershipRepository dealershipRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ServiceTypeRepository serviceTypeRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private ServiceBayRepository serviceBayRepository;
    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks private BookingService bookingService;

    private Dealership dealership;
    private ServiceType oilChange;
    private Vehicle vehicle;
    private Technician technician;
    private ServiceBay bay;

    private static final LocalDateTime NINE_AM = LocalDateTime.of(2030, 1, 1, 9, 0);

    @BeforeEach
    void setUp() {
        dealership = new Dealership("Downtown", LocalTime.of(8, 0), LocalTime.of(18, 0));
        oilChange = new ServiceType("OIL_CHANGE", "Oil Change", 30, "GENERAL");
        Customer customer = new Customer("Alice", "alice@example.com", "555");
        vehicle = new Vehicle(customer, "VIN123", "Toyota", "Corolla", 2020);
        technician = new Technician(dealership, "Carlos", Set.of("GENERAL"));
        bay = new ServiceBay(dealership, "Bay A");
    }

    private BookingRequest request() {
        return new BookingRequest(1L, 1L, 1L, NINE_AM);
    }

    private void stubLookups() {
        when(serviceTypeRepository.findById(1L)).thenReturn(Optional.of(oilChange));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(dealershipRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dealership));
    }

    @Test
    void booksWhenTechnicianAndBayAvailable() {
        stubLookups();
        when(technicianRepository.findAvailable(any(), eq("GENERAL"), any(), any(), any()))
                .thenReturn(List.of(technician));
        when(serviceBayRepository.findAvailable(any(), any(), any(), any()))
                .thenReturn(List.of(bay));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = bookingService.book(request());

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.technician().name()).isEqualTo("Carlos");
        assertThat(response.serviceBay().name()).isEqualTo("Bay A");
        assertThat(response.startTime()).isEqualTo(NINE_AM);
        assertThat(response.endTime()).isEqualTo(NINE_AM.plusMinutes(30));
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void throwsWhenNoQualifiedTechnician() {
        stubLookups();
        when(technicianRepository.findAvailable(any(), eq("GENERAL"), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> bookingService.book(request()))
                .isInstanceOf(NoAvailabilityException.class)
                .hasMessageContaining("technician");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void throwsWhenNoBayAvailable() {
        stubLookups();
        when(technicianRepository.findAvailable(any(), eq("GENERAL"), any(), any(), any()))
                .thenReturn(List.of(technician));
        when(serviceBayRepository.findAvailable(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> bookingService.book(request()))
                .isInstanceOf(NoAvailabilityException.class)
                .hasMessageContaining("bay");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void throwsWhenOutsideOpeningHours() {
        stubLookups();
        BookingRequest early = new BookingRequest(1L, 1L, 1L, LocalDateTime.of(2030, 1, 1, 7, 45));

        assertThatThrownBy(() -> bookingService.book(early))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("opening hours");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void throwsWhenServiceTypeMissing() {
        when(serviceTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.book(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ServiceType");
    }
}
