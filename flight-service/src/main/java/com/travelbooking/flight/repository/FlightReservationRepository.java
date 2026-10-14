package com.travelbooking.flight.repository;
import com.travelbooking.flight.model.entity.FlightReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface FlightReservationRepository extends JpaRepository<FlightReservation,UUID> {
    Optional<FlightReservation> findByBookingId(UUID bookingId);
}
