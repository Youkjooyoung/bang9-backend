package kr.bang9.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class ListingSeeder {

    private static final Logger log = LoggerFactory.getLogger(ListingSeeder.class);
    private static final int TARGET_COUNT = 100;

    private static final double CENTER_LAT = 37.4842;
    private static final double CENTER_LNG = 126.8983;
    private static final double LAT_SPREAD = 0.010;
    private static final double LNG_SPREAD = 0.012;

    private static final String BASE_ROAD = "서울 구로구 디지털로31길";

    @Bean
    @Order(10)
    public ApplicationRunner seedListings(DataSource dataSource, JdbcTemplate jdbc) {
        return args -> {
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM LISTINGS", Integer.class);
            int existingCount = existing == null ? 0 : existing;
            if (existingCount >= TARGET_COUNT) {
                log.info("[ListingSeeder] LISTINGS 테이블에 이미 {}건 존재. 시드를 건너뜁니다.", existingCount);
                return;
            }

            Long hostUserId;
            try {
                hostUserId = jdbc.queryForObject(
                    "SELECT USER_ID FROM USERS WHERE EMAIL = ?",
                    Long.class,
                    "admin@bang9.local"
                );
            } catch (Exception e) {
                log.warn("[ListingSeeder] admin 호스트 사용자 없음. 시드 건너뜀: {}", e.getMessage());
                return;
            }
            if (hostUserId == null) {
                log.warn("[ListingSeeder] admin 호스트 사용자 없음. 시드 건너뜀.");
                return;
            }

            SimpleJdbcInsert imageInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("LISTING_IMAGES")
                .usingColumns("LISTING_ID", "S3_URL", "SORT_ORDER", "IS_COVER")
                .usingGeneratedKeyColumns("LISTING_IMAGE_ID");

            List<RoomSpec> roomSpecs = buildRoomSpecs();
            Random random = new Random(77);
            int toCreate = TARGET_COUNT - existingCount;
            int created = 0;

            while (created < toCreate) {
                RoomSpec spec = pickByWeight(roomSpecs, random);
                String dealType = pickDealType(random, spec);
                int deposit = dealDeposit(random, spec, dealType);
                int monthlyRent = dealMonthly(random, spec, dealType);
                int maintenance = dealType.equals("SALE") ? 0 : randomIn(random, 30_000, 180_000);
                double area = spec.areaMin + random.nextDouble() * (spec.areaMax - spec.areaMin);
                area = Math.round(area * 10.0) / 10.0;
                short floor = (short) (random.nextInt(15) + 1);
                short totalFloor = (short) Math.max(floor, (short) (floor + random.nextInt(8)));

                double lat = CENTER_LAT + (random.nextDouble() - 0.5) * 2 * LAT_SPREAD;
                double lng = CENTER_LNG + (random.nextDouble() - 0.5) * 2 * LNG_SPREAD;
                int building = 1 + random.nextInt(200);
                String addressRoad = BASE_ROAD + " " + building;
                String addressDetail = (random.nextInt(20) + 1) + "동 " + (random.nextInt(20) + 1) + "0"
                    + random.nextInt(10) + "호";

                String title = buildTitle(spec, dealType, random);
                String description = buildDescription(spec, dealType, area);

                String insertSql = "INSERT INTO LISTINGS ("
                    + "HOST_USER_ID, TITLE, DESCRIPTION, ROOM_TYPE, DEAL_TYPE, DEPOSIT, MONTHLY_RENT, MAINTENANCE_FEE, "
                    + "AREA_M2, FLOOR, TOTAL_FLOOR, ADDRESS_ROAD, ADDRESS_DETAIL, LATITUDE, LONGITUDE, GEO_POINT, STATUS, SOURCE, "
                    + "BROKER_NAME, BROKER_PHONE, BROKER_OFFICE_PHONE, BROKER_OFFICE_NAME"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                    + "ST_SRID(ST_GeomFromText(?), 4326), ?, ?, ?, ?, ?, ?)";

                String wkt = "POINT(" + lng + " " + lat + ")";

                String brokerName = pickBrokerName(random);
                String brokerPhone = buildPhone(random, true);
                String brokerOfficePhone = buildPhone(random, false);
                String brokerOfficeName = pickOfficeName(random);

                long newId = insertListing(jdbc, insertSql,
                    hostUserId, title, description, spec.roomType, dealType, deposit, monthlyRent, maintenance,
                    area, floor, totalFloor, addressRoad, addressDetail, lat, lng, wkt, "ACTIVE", "SEED",
                    brokerName, brokerPhone, brokerOfficePhone, brokerOfficeName);

                int imageCount = 3 + random.nextInt(3);
                for (int i = 0; i < imageCount; i++) {
                    Map<String, Object> imgRow = new HashMap<>();
                    imgRow.put("LISTING_ID", newId);
                    imgRow.put("S3_URL", "https://picsum.photos/seed/bang9-listing-" + newId + "-" + i + "/960/640");
                    imgRow.put("SORT_ORDER", i);
                    imgRow.put("IS_COVER", i == 0);
                    imageInsert.execute(imgRow);
                }

                created++;
            }

            log.info("[ListingSeeder] 매물 {}건 생성 완료 (중심 {}, {}).", created, CENTER_LAT, CENTER_LNG);
        };
    }

    private static long insertListing(JdbcTemplate jdbc, String sql, Object... params) {
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("LISTINGS insert returned no key");
        return key.longValue();
    }

    private static RoomSpec pickByWeight(List<RoomSpec> specs, Random random) {
        int totalWeight = specs.stream().mapToInt(RoomSpec::weight).sum();
        int pick = random.nextInt(totalWeight);
        int acc = 0;
        for (RoomSpec spec : specs) {
            acc += spec.weight;
            if (pick < acc) return spec;
        }
        return specs.get(0);
    }

    private static String pickDealType(Random random, RoomSpec spec) {
        int r = random.nextInt(100);
        if (spec.roomType.equals("APARTMENT") || spec.roomType.equals("HOUSE")) {
            if (r < 25) return "SALE";
            if (r < 65) return "JEONSE";
            return "MONTHLY";
        }
        if (spec.roomType.equals("OFFICETEL")) {
            if (r < 10) return "SALE";
            if (r < 45) return "JEONSE";
            return "MONTHLY";
        }
        if (r < 30) return "JEONSE";
        return "MONTHLY";
    }

    private static int dealDeposit(Random random, RoomSpec spec, String dealType) {
        if (dealType.equals("SALE")) return roundToMan(randomIn(random, spec.salePriceMin, spec.salePriceMax));
        if (dealType.equals("JEONSE")) return roundToMan(randomIn(random, spec.jeonseMin, spec.jeonseMax));
        return roundToMan(randomIn(random, spec.depositMin, spec.depositMax));
    }

    private static int dealMonthly(Random random, RoomSpec spec, String dealType) {
        if (dealType.equals("SALE") || dealType.equals("JEONSE")) return 0;
        return roundToMan(randomIn(random, spec.rentMin, spec.rentMax));
    }

    private static int randomIn(Random random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private static int roundToMan(int value) {
        return Math.max(10_000, Math.round(value / 10_000f) * 10_000);
    }

    private static String buildTitle(RoomSpec spec, String dealType, Random random) {
        String[] adjectives = {"깨끗한", "채광좋은", "신축", "풀옵션", "역세권", "남향", "리모델링", "조용한"};
        String dealLabel = dealType.equals("SALE") ? "매매" : dealType.equals("JEONSE") ? "전세" : "월세";
        String adj = adjectives[random.nextInt(adjectives.length)];
        return "[" + dealLabel + "] " + adj + " " + spec.displayName + " (디지털단지)";
    }

    private static String buildDescription(RoomSpec spec, String dealType, double area) {
        String dealLabel = dealType.equals("SALE") ? "매매" : dealType.equals("JEONSE") ? "전세" : "월세";
        return "구로 디지털단지 " + spec.displayName + " 매물입니다. 면적 " + area + "㎡, 거래유형: " + dealLabel
            + ". 가산/구로 디지털단지 지하철역 도보권, 주변 편의시설 풍부.";
    }

    private static List<RoomSpec> buildRoomSpecs() {
        return List.of(
            new RoomSpec("ONE_ROOM", "원룸", 40,
                15.0, 26.0,
                5_000_000, 30_000_000,
                300_000, 700_000,
                30_000_000, 100_000_000,
                100_000_000, 250_000_000),
            new RoomSpec("TWO_ROOM", "투룸", 20,
                26.0, 45.0,
                10_000_000, 70_000_000,
                500_000, 1_200_000,
                100_000_000, 250_000_000,
                250_000_000, 550_000_000),
            new RoomSpec("OFFICETEL", "오피스텔", 20,
                20.0, 40.0,
                5_000_000, 50_000_000,
                400_000, 900_000,
                80_000_000, 200_000_000,
                180_000_000, 450_000_000),
            new RoomSpec("APARTMENT", "아파트", 15,
                55.0, 120.0,
                30_000_000, 200_000_000,
                1_000_000, 3_000_000,
                200_000_000, 600_000_000,
                550_000_000, 1_500_000_000),
            new RoomSpec("HOUSE", "주택", 5,
                60.0, 150.0,
                50_000_000, 300_000_000,
                1_500_000, 4_000_000,
                250_000_000, 800_000_000,
                700_000_000, 1_800_000_000)
        );
    }

    private static String pickBrokerName(Random random) {
        String[] surnames = {"김", "이", "박", "최", "정", "장", "임", "한", "신", "오"};
        String[] givenNames = {"민수", "지훈", "영호", "수연", "지영", "현우", "유진", "태민", "서연", "동현"};
        return surnames[random.nextInt(surnames.length)] + givenNames[random.nextInt(givenNames.length)];
    }

    private static String pickOfficeName(Random random) {
        String[] prefixes = {"디지털", "구로", "행복", "신도림", "가산", "베스트", "한솔", "미래", "으뜸", "프라임"};
        return prefixes[random.nextInt(prefixes.length)] + " 공인중개사사무소";
    }

    private static String buildPhone(Random random, boolean mobile) {
        if (mobile) {
            int mid = 1000 + random.nextInt(9000);
            int tail = 1000 + random.nextInt(9000);
            return "010-" + mid + "-" + tail;
        }
        int mid = 100 + random.nextInt(900);
        int tail = 1000 + random.nextInt(9000);
        return "02-" + mid + "-" + tail;
    }

    private record RoomSpec(
        String roomType,
        String displayName,
        int weight,
        double areaMin,
        double areaMax,
        int depositMin,
        int depositMax,
        int rentMin,
        int rentMax,
        int jeonseMin,
        int jeonseMax,
        int salePriceMin,
        int salePriceMax
    ) {
    }
}
