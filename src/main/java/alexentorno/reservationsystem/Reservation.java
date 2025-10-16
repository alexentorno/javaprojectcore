package alexentorno.reservationsystem;

import java.time.LocalDate;

public record Reservation (
        Long id,
        Long userId,
        Long roomId,
        LocalDate start,
        LocalDate end,
        ReservationStatus status
) {}
