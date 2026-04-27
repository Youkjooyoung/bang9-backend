package kr.bang9.external.publicdata.dao;

import java.util.List;
import kr.bang9.external.publicdata.PublicRealEstate;
import kr.bang9.external.publicdata.dto.MarketPriceStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PublicDataDao {

    int bulkInsert(@Param("rows") List<PublicRealEstate> rows);

    int countByRegionAndMonth(@Param("regionCode") String regionCode,
                              @Param("yearMonth") String yearMonth);

    int deleteByRegionAndMonth(@Param("regionCode") String regionCode,
                               @Param("yearMonth") String yearMonth);

    MarketPriceStats findStats(@Param("regionCode") String regionCode,
                               @Param("dealType") String dealType);

    List<PublicRealEstate> findRecent(@Param("regionCode") String regionCode,
                                      @Param("dealType") String dealType,
                                      @Param("size") int size);
}
