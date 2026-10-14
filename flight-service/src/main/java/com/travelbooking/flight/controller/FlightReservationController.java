package com.travelbooking.flight.controller;

import com.travelbooking.flight.model.dto.request.FlightReservationRequest;
import com.travelbooking.flight.model.dto.response.FlightReservationResponse;
import com.travelbooking.flight.service.FlightReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flight-reservations")
@RequiredArgsConstructor
public class FlightReservationController {

    private final FlightReservationService service;
    @PostMapping
    public ResponseEntity<FlightReservationResponse> book(@Valid @RequestBody FlightReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bookFlight(request));
    }
    @GetMapping("/by-booking/{bookingId}")
    public FlightReservationResponse getByBooking(@PathVariable UUID bookingId) {
        return service.getByBookingId(bookingId);
    }
    @PostMapping("/by-booking/{bookingId}/cancel")
    public FlightReservationResponse cancel(@PathVariable UUID bookingId) {
        return service.cancelFlight(bookingId);
    }
}
