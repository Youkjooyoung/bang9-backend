package kr.bang9.external.publicdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicRealEstate {
    private Long dealId;
    private String regionCode;
    private String dealType;
    private BigDecimal areaM2;
    private Integer deposit;
    private Integer monthlyRent;
    private String dealYearMonth;
    private String buildingName;
    private Short floor;
    private LocalDateTime createdAt;
}
