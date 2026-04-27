package kr.bang9.ai.dao;

import kr.bang9.ai.domain.ListingAiSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListingAiSummaryDao {

    ListingAiSummary findByListingId(@Param("listingId") Long listingId);

    List<Long> findListingIdsWithoutSummary(@Param("limit") int limit);

    int upsert(ListingAiSummary summary);

    int deleteByListingId(@Param("listingId") Long listingId);
}
