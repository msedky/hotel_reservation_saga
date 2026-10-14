package com.travelbooking.flight.model.entity;
import com.travelbooking.flight.model.enums.FlightReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="flight_reservations", uniqueConstraints=@UniqueConstraint(name="uk_flight_reservation_booking",columnNames="booking_id"))
@Getter @Setter
public class FlightReservation {
    @Id @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;
    @Column(name="booking_id",nullable=false) private UUID bookingId;
    @Column(name="saga_id",nullable=false) private UUID sagaId;
    @Column(name="flight_offer_id",nullable=false) private String flightOfferId;
    @Column(name="provider_reference") private String providerReference;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private FlightReservationStatus status;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void prePersist(){createdAt=Instant.now();updatedAt=createdAt;}
    @PreUpdate void preUpdate(){updatedAt=Instant.now();}
}
