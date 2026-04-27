package kr.bang9.listing.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GU 코드(시·군·구 5자리) → 행정구 실제 중심점(centroid).
 * 클러스터 응답의 중심 좌표는 매물의 AVG로 계산되는데,
 * 매물이 한 지역에 몰리면 모든 GU 마커가 겹치므로 실제 행정구 중심으로 보정한다.
 */
public final class GuCentroids {

    private GuCentroids() {
    }

    public record Coord(BigDecimal lat, BigDecimal lng) {
    }

    private static Coord c(double lat, double lng) {
        return new Coord(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }

    private static final Map<String, Coord> SEOUL = Map.ofEntries(
        Map.entry("11110", c(37.5735, 126.9788)),
        Map.entry("11140", c(37.5641, 126.9979)),
        Map.entry("11170", c(37.5326, 126.9905)),
        Map.entry("11200", c(37.5634, 127.0367)),
        Map.entry("11215", c(37.5384, 127.0823)),
        Map.entry("11230", c(37.5744, 127.0397)),
        Map.entry("11260", c(37.6065, 127.0928)),
        Map.entry("11290", c(37.5894, 127.0167)),
        Map.entry("11305", c(37.6396, 127.0257)),
        Map.entry("11320", c(37.6688, 127.0471)),
        Map.entry("11350", c(37.6542, 127.0568)),
        Map.entry("11380", c(37.6027, 126.9291)),
        Map.entry("11410", c(37.5791, 126.9368)),
        Map.entry("11440", c(37.5663, 126.9019)),
        Map.entry("11470", c(37.5170, 126.8665)),
        Map.entry("11500", c(37.5509, 126.8495)),
        Map.entry("11530", c(37.4954, 126.8874)),
        Map.entry("11545", c(37.4519, 126.9020)),
        Map.entry("11560", c(37.5263, 126.8966)),
        Map.entry("11590", c(37.5124, 126.9393)),
        Map.entry("11620", c(37.4784, 126.9516)),
        Map.entry("11650", c(37.4837, 127.0324)),
        Map.entry("11680", c(37.5172, 127.0473)),
        Map.entry("11710", c(37.5145, 127.1059)),
        Map.entry("11740", c(37.5301, 127.1238))
    );

    public static Coord lookup(String regionCode) {
        if (regionCode == null) return null;
        return SEOUL.get(regionCode);
    }
}
