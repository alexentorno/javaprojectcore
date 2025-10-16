package alexentorno.reservationsystem;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository repository) {
        this.reservationRepository = repository;
    }

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Reservation with id " + id + " not found."
                ));
        return toDomainReservation(reservationEntity);
    }

    public List<Reservation> getAllReservations() {
        List<ReservationEntity> reservations = reservationRepository.findAll();
        return reservations.stream().map(this::toDomainReservation).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("Reservation status should be empty.");
        }
        if (!reservationToCreate.end().isAfter(reservationToCreate.start())) {
            log.info("End date must be after start date for at least one day.");
            throw new IllegalArgumentException("End date must be after start date for at least one day.");
        }
        ReservationEntity newReservation = new ReservationEntity(
            null,
            reservationToCreate.userId(),
            reservationToCreate.roomId(),
            reservationToCreate.start(),
            reservationToCreate.end(),
            ReservationStatus.PENDING
        );
        var savedEntity = reservationRepository.save(newReservation); // .save() returns entity with generated id
        return toDomainReservation(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        ReservationEntity existingReservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation with id " + id + " not found."));
        if (existingReservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Reservation cannot be modified. Status must be PENDING, but found " + reservationToUpdate.status()
            );
        }
        if (existingReservation.endDate.isBefore(existingReservation.startDate)) {
            throw new IllegalArgumentException("End date must be after start date for at least one day.");
        }
        ReservationEntity reservationToSave = new ReservationEntity(
            existingReservation.getId(),
            reservationToUpdate.userId(),
            reservationToUpdate.roomId(),
            reservationToUpdate.start(),
            reservationToUpdate.end(),
            ReservationStatus.PENDING
        );
        ReservationEntity updatedEntity = reservationRepository.save(reservationToSave);
        return toDomainReservation(updatedEntity);
    }

    @Transactional
    public void cancelReservation(Long id) {
        var reservation = reservationRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Reservation with id " + id + " not found."));
        if (reservation.getStatus() == ReservationStatus.APPROVED) {
            throw new IllegalStateException("Reservation with id " + id + " cannot be cancelled as it is already approved.");
        }
        if (reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new IllegalStateException("Reservation with id " + id + " is already cancelled.");
        }
        reservationRepository.setStatus(id, ReservationStatus.CANCELLED);
        log.info("Reservation with id " + id + " successfully cancelled.");
    }

    public Reservation approveReservation(Long id) {
        ReservationEntity reservationToApprove = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Reservation with id " + id + " not found."
                ));
        if (reservationToApprove.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Reservation cannot be approved. Status must be PENDING, but found "
                            + reservationToApprove.getStatus().name()
            );
        }
        if (reservationHasConflictsWithOtherReservations(reservationToApprove)) {
            throw new IllegalStateException(
                    "Reservation cannot be approved. It has conflicts with other reservations."
            );
        }
        reservationToApprove.setStatus(ReservationStatus.APPROVED);
        reservationRepository.save(reservationToApprove);
        return toDomainReservation(reservationToApprove);
    }

    private boolean reservationHasConflictsWithOtherReservations(
            ReservationEntity reservation
    ) {
        return reservationRepository.findAll().stream()
                .filter(r -> !r.equals(reservation))
                .anyMatch(found -> found.getEndDate().isAfter(reservation.getStartDate())
                        && found.getEndDate().isBefore(reservation.getEndDate())
                        || found.getStartDate().isAfter(reservation.getStartDate())
                        && found.getStartDate().isBefore(reservation.getEndDate())
                        || found.getStartDate().isBefore(reservation.getStartDate())
                        && found.getEndDate().isAfter(reservation.getEndDate())
                        || found.getStartDate().isEqual(reservation.getStartDate())
                        || found.getEndDate().isEqual(reservation.getEndDate())
                );
    }

    private Reservation toDomainReservation(ReservationEntity entity) {
        return new Reservation(
            entity.getId(),
            entity.getUserId(),
            entity.getRoomId(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getStatus()
        );
    }
}
