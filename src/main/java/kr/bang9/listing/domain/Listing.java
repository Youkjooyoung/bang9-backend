package kr.bang9.listing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    private Long listingId;
    private Long hostUserId;
    private String title;
    private String description;
    private String roomType;
    private String dealType;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer maintenanceFee;
    private BigDecimal areaM2;
    private Short floor;
    private Short totalFloor;
    private Byte roomCount;
    private Byte bathroomCount;
    private String addressRoad;
    private String addressDetail;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private String rejectReason;
    private String source;
    private String brokerName;
    private String brokerPhone;
    private String brokerOfficePhone;
    private String brokerOfficeName;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
