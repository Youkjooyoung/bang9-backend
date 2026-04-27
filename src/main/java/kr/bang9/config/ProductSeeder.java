package kr.bang9.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class ProductSeeder {

    private static final Logger log = LoggerFactory.getLogger(ProductSeeder.class);
    private static final int TARGET_COUNT = 800;

    @Bean
    public ApplicationRunner seedProducts(DataSource dataSource, JdbcTemplate jdbc) {
        return args -> {
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM PRODUCTS", Integer.class);
            if (existing != null && existing > 0) {
                log.info("[ProductSeeder] PRODUCTS 테이블에 이미 {}건 존재. 시드를 건너뜁니다.", existing);
                return;
            }

            List<CategorySpec> specs = buildCategorySpecs();
            SimpleJdbcInsert productInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("PRODUCTS")
                .usingColumns(
                    "CATEGORY_ID", "NAME", "BRAND", "PRICE", "SALE_PRICE", "STOCK",
                    "STATUS", "WIDTH_CM", "DEPTH_CM", "HEIGHT_CM", "SHIPPING_FEE",
                    "DESCRIPTION_HTML"
                )
                .usingGeneratedKeyColumns("PRODUCT_ID");
            SimpleJdbcInsert imageInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("PRODUCT_IMAGES")
                .usingColumns("PRODUCT_ID", "S3_URL", "SORT_ORDER", "IS_COVER")
                .usingGeneratedKeyColumns("PRODUCT_IMAGE_ID");

            Random random = new Random(42);
            int created = 0;
            while (created < TARGET_COUNT) {
                CategorySpec spec = specs.get(random.nextInt(specs.size()));
                String name = buildName(spec, random);
                String brand = spec.brands.get(random.nextInt(spec.brands.size()));
                int price = randomInRange(random, spec.priceMin, spec.priceMax);
                price = roundToThousand(price);
                int salePrice = (int) Math.round(price * (0.7 + random.nextDouble() * 0.3));
                salePrice = roundToThousand(salePrice);
                int stock = 10 + random.nextInt(200);
                int widthCm = randomInRange(random, spec.widthMin, spec.widthMax);
                int depthCm = randomInRange(random, spec.depthMin, spec.depthMax);
                int heightCm = randomInRange(random, spec.heightMin, spec.heightMax);
                int shipping = random.nextInt(4) == 0 ? 0 : (3000 + random.nextInt(10) * 500);

                Map<String, Object> row = new HashMap<>();
                row.put("CATEGORY_ID", spec.categoryId);
                row.put("NAME", name);
                row.put("BRAND", brand);
                row.put("PRICE", price);
                row.put("SALE_PRICE", salePrice);
                row.put("STOCK", stock);
                row.put("STATUS", "ACTIVE");
                row.put("WIDTH_CM", widthCm);
                row.put("DEPTH_CM", depthCm);
                row.put("HEIGHT_CM", heightCm);
                row.put("SHIPPING_FEE", shipping);
                row.put("DESCRIPTION_HTML",
                    "<p>" + brand + " " + spec.displayName + " 상품입니다. "
                        + "편안한 일상을 위한 " + spec.displayName + ".</p>");

                Number productId = productInsert.executeAndReturnKey(row);

                Map<String, Object> imageRow = new HashMap<>();
                imageRow.put("PRODUCT_ID", productId.longValue());
                imageRow.put("S3_URL", "https://picsum.photos/seed/bang9-" + productId + "/480/480");
                imageRow.put("SORT_ORDER", 0);
                imageRow.put("IS_COVER", true);
                imageInsert.execute(imageRow);

                created++;
            }

            log.info("[ProductSeeder] 상품 {}건 생성 완료.", created);
        };
    }

    private static String buildName(CategorySpec spec, Random random) {
        String adj = spec.adjectives.get(random.nextInt(spec.adjectives.size()));
        String material = spec.materials.get(random.nextInt(spec.materials.size()));
        return String.format("%s %s %s", adj, material, spec.displayName);
    }

    private static int randomInRange(Random random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private static int roundToThousand(int value) {
        return Math.max(1000, Math.round(value / 1000f) * 1000);
    }

    private static List<CategorySpec> buildCategorySpecs() {
        List<String> brandsFurniture = List.of("한샘", "일룸", "리바트", "까사미아", "시몬스", "에이스", "템퍼", "이케아");
        List<String> brandsAppliance = List.of("삼성", "LG", "위니아", "캐리어", "쿠쿠", "다이슨");
        List<String> brandsBedding = List.of("이브자리", "알레르망", "프레쉬룸", "지베르니", "포레스트");
        List<String> brandsLighting = List.of("오스람", "필립스", "레고아", "라이트온", "룸앤홈");
        List<String> brandsInterior = List.of("자라홈", "H&M홈", "모던하우스", "플라잉타이거", "이니스");

        List<String> adjPremium = List.of("모던", "클래식", "프리미엄", "북유럽풍", "미니멀", "내추럴");
        List<String> adjBasic = List.of("베이직", "심플", "컴팩트", "슬림", "와이드");
        List<String> adjAppliance = List.of("스마트", "에너지효율", "인버터", "프리미엄", "컴팩트");
        List<String> adjBedding = List.of("호텔식", "쿨", "사계절", "부드러운", "프리미엄");
        List<String> adjLighting = List.of("무드", "LED", "스마트", "감성", "따뜻한");

        List<String> materialsWood = List.of("원목", "MDF", "오크", "월넛", "자작", "화이트");
        List<String> materialsFabric = List.of("린넨", "패브릭", "극세사", "벨벳", "가죽");
        List<String> materialsAppliance = List.of("실버", "블랙", "화이트", "스테인리스", "베이지");
        List<String> materialsLighting = List.of("골드", "블랙", "우드", "아크릴", "유리");
        List<String> materialsTextile = List.of("면100", "린넨", "극세사", "벨벳", "데님");

        return List.of(
            new CategorySpec(10101, "싱글 침대", brandsFurniture, adjPremium, materialsWood, 120_000, 450_000, 100, 110, 195, 200, 30, 110),
            new CategorySpec(10102, "슈퍼싱글 침대", brandsFurniture, adjPremium, materialsWood, 150_000, 600_000, 110, 130, 200, 210, 30, 110),
            new CategorySpec(10103, "퀸 침대", brandsFurniture, adjPremium, materialsWood, 220_000, 900_000, 150, 170, 200, 215, 30, 130),
            new CategorySpec(10104, "킹 침대", brandsFurniture, adjPremium, materialsWood, 280_000, 1_200_000, 180, 200, 200, 215, 30, 130),
            new CategorySpec(10201, "1인 소파", brandsFurniture, adjBasic, materialsFabric, 180_000, 600_000, 80, 110, 80, 95, 70, 95),
            new CategorySpec(10202, "2인 소파", brandsFurniture, adjBasic, materialsFabric, 260_000, 1_000_000, 150, 180, 80, 100, 70, 100),
            new CategorySpec(10203, "3인 소파", brandsFurniture, adjBasic, materialsFabric, 380_000, 1_800_000, 200, 250, 85, 105, 75, 105),
            new CategorySpec(103, "식탁 세트", brandsFurniture, adjBasic, materialsWood, 200_000, 900_000, 100, 200, 60, 90, 70, 90),
            new CategorySpec(104, "책상/의자", brandsFurniture, adjBasic, materialsWood, 90_000, 500_000, 90, 160, 50, 70, 70, 130),
            new CategorySpec(105, "옷장/행거", brandsFurniture, adjBasic, materialsWood, 120_000, 800_000, 80, 200, 40, 60, 150, 220),
            new CategorySpec(201, "냉장고", brandsAppliance, adjAppliance, materialsAppliance, 600_000, 3_500_000, 55, 85, 60, 80, 150, 200),
            new CategorySpec(202, "세탁기", brandsAppliance, adjAppliance, materialsAppliance, 450_000, 2_200_000, 55, 65, 55, 65, 85, 115),
            new CategorySpec(203, "TV", brandsAppliance, adjAppliance, materialsAppliance, 350_000, 3_000_000, 90, 160, 10, 30, 55, 95),
            new CategorySpec(204, "전자레인지", brandsAppliance, adjAppliance, materialsAppliance, 80_000, 450_000, 40, 55, 35, 50, 28, 40),
            new CategorySpec(301, "매트리스", brandsBedding, adjBedding, materialsTextile, 180_000, 1_500_000, 100, 200, 190, 215, 15, 30),
            new CategorySpec(302, "이불/베개", brandsBedding, adjBedding, materialsTextile, 25_000, 250_000, 50, 220, 50, 240, 5, 20),
            new CategorySpec(401, "서랍장", brandsFurniture, adjBasic, materialsWood, 100_000, 450_000, 60, 120, 35, 50, 60, 130),
            new CategorySpec(402, "수납박스", brandsInterior, adjBasic, materialsWood, 15_000, 80_000, 30, 80, 30, 50, 25, 55),
            new CategorySpec(501, "스탠드 조명", brandsLighting, adjLighting, materialsLighting, 40_000, 280_000, 20, 45, 20, 45, 50, 160),
            new CategorySpec(502, "천장등", brandsLighting, adjLighting, materialsLighting, 60_000, 400_000, 40, 90, 40, 90, 10, 25),
            new CategorySpec(601, "커튼", brandsInterior, adjBedding, materialsTextile, 30_000, 180_000, 100, 300, 1, 5, 150, 260),
            new CategorySpec(602, "러그", brandsInterior, adjBedding, materialsTextile, 50_000, 350_000, 100, 240, 150, 300, 1, 3)
        );
    }

    private record CategorySpec(
        int categoryId,
        String displayName,
        List<String> brands,
        List<String> adjectives,
        List<String> materials,
        int priceMin,
        int priceMax,
        int widthMin,
        int widthMax,
        int depthMin,
        int depthMax,
        int heightMin,
        int heightMax
    ) {
    }
}
