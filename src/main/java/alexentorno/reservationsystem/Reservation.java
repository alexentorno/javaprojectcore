package alexentorno.reservationsystem;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.time.LocalDate;

public record Reservation (

        @Null
        Long id,

        @NotNull
        Long userId,

        @NotNull
        Long roomId,

        @FutureOrPresent
        @NotNull
        LocalDate start,

        @FutureOrPresent
        @NotNull
        LocalDate end,

        ReservationStatus status
) {}
