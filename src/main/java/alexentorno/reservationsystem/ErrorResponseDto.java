package alexentorno.reservationsystem;

import java.time.LocalDateTime;

public record ErrorResponseDto (
        String message,
        String detailedMessage,
        LocalDateTime errorTime
) {
}
