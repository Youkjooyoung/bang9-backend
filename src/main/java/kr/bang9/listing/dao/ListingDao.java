package kr.bang9.listing.dao;

import kr.bang9.listing.domain.Listing;
import kr.bang9.listing.dto.ListingBoundsRequest;
import kr.bang9.listing.dto.ListingClusterRequest;
import kr.bang9.listing.dto.ListingClusterResponse;
import kr.bang9.listing.dto.ListingDetail;
import kr.bang9.listing.dto.ListingSearchRequest;
import kr.bang9.listing.dto.ListingSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ListingDao {

    void insertListing(Listing listing);

    void insertListingImages(
        @Param("listingId") Long listingId,
        @Param("imageUrls") List<String> imageUrls
    );

    void insertListingOptions(
        @Param("listingId") Long listingId,
        @Param("optionCodes") List<String> optionCodes
    );

    List<ListingSummary> search(
        @Param("q") ListingSearchRequest request,
        @Param("offset") int offset,
        @Param("limit") int limit,
        @Param("viewerUserId") Long viewerUserId
    );

    long countSearch(@Param("q") ListingSearchRequest request);

    List<ListingSummary> findByBounds(
        @Param("q") ListingSearchRequest request,
        @Param("viewerUserId") Long viewerUserId
    );

    List<ListingSummary> findByHost(@Param("hostUserId") Long hostUserId);

    Optional<ListingDetail> findDetail(@Param("listingId") Long listingId);

    Optional<Long> findHostUserId(@Param("listingId") Long listingId);

    List<String> findImageUrls(@Param("listingId") Long listingId);

    List<String> findOptionCodes(@Param("listingId") Long listingId);

    Optional<String> findAiSummary(@Param("listingId") Long listingId);

    void updateListing(Listing listing);

    void updateStatus(
        @Param("listingId") Long listingId,
        @Param("status") String status
    );

    void increaseViewCount(@Param("listingId") Long listingId);

    void deleteImagesByListing(@Param("listingId") Long listingId);

    void deleteOptionsByListing(@Param("listingId") Long listingId);

    void softDelete(
        @Param("listingId") Long listingId,
        @Param("hostUserId") Long hostUserId
    );

    List<ListingClusterResponse> findClustersByGu(@Param("q") ListingClusterRequest request);

    List<ListingClusterResponse> findClustersByDong(@Param("q") ListingClusterRequest request);

    List<ListingSummary> findInBounds(
        @Param("q") ListingBoundsRequest request,
        @Param("viewerUserId") Long viewerUserId,
        @Param("limit") int limit
    );
}
