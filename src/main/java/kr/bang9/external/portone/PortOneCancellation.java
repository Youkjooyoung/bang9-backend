package kr.bang9.external.portone;

public record PortOneCancellation(
    String impUid,
    String merchantUid,
    String status,
    int cancelAmount,
    long cancelledAtEpochSec,
    String pgTid,
    String rawJson
) {
    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }
}
