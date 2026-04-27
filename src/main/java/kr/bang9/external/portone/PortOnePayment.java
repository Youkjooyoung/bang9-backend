package kr.bang9.external.portone;

public record PortOnePayment(
    String impUid,
    String merchantUid,
    String status,
    String method,
    int amount,
    long paidAtEpochSec,
    String rawJson
) {
    public boolean isPaid() {
        return "paid".equalsIgnoreCase(status);
    }
}
