package kr.bang9.listing.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.util.ProfanityFilter;
import kr.bang9.listing.dao.ListingDao;
import kr.bang9.listing.domain.Listing;
import kr.bang9.listing.dto.ListingBoundsRequest;
import kr.bang9.listing.dto.ListingClusterRequest;
import kr.bang9.listing.dto.ListingClusterResponse;
import kr.bang9.listing.dto.ListingCreateResponse;
import kr.bang9.listing.dto.ListingDetail;
import kr.bang9.listing.dto.ListingDetailResponse;
import kr.bang9.listing.dto.ListingSaveRequest;
import kr.bang9.listing.dto.ListingSearchRequest;
import kr.bang9.listing.dto.ListingSummary;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.listing.favorite.dao.FavoriteDao;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String SOURCE_HOST = "HOST";

    private final ListingDao listingDao;
    private final FavoriteDao favoriteDao;
    private final ProfanityFilter profanityFilter;

    @Transactional
    public ListingCreateResponse create(long hostUserId, ListingSaveRequest request) {
        validateProfanity(request.title(), request.description());
        Listing listing = buildListing(null, hostUserId, request);
        listingDao.insertListing(listing);
        saveImagesAndOptions(listing.getListingId(), request.imageUrls(), request.optionCodes());
        return new ListingCreateResponse(listing.getListingId(), listing.getStatus());
    }

    public PageResponse<ListingSummary> search(ListingSearchRequest request, Long viewerUserId) {
        int page = request.safePage();
        int size = request.safeSize();
        int offset = page * size;
        List<ListingSummary> content = listingDao.search(request, offset, size, viewerUserId);
        long total = listingDao.countSearch(request);
        return PageResponse.of(content, page, size, total);
    }

    public List<ListingSummary> findByBounds(ListingSearchRequest request, Long viewerUserId) {
        return listingDao.findByBounds(request, viewerUserId);
    }

    public List<ListingClusterResponse> findClusters(ListingClusterRequest request) {
        if (!request.hasBounds()) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        if (request.isGuLevel()) {
            return listingDao.findClustersByGu(request).stream()
                .map(this::overrideGuCentroid)
                .toList();
        }
        if (request.isDongLevel()) {
            return listingDao.findClustersByDong(request);
        }
        return Collections.emptyList();
    }

    private ListingClusterResponse overrideGuCentroid(ListingClusterResponse c) {
        GuCentroids.Coord centroid = GuCentroids.lookup(c.regionCode());
        if (centroid == null) return c;
        return new ListingClusterResponse(
            c.regionCode(),
            c.regionName(),
            c.regionType(),
            centroid.lat(),
            centroid.lng(),
            c.count()
        );
    }

    public List<ListingSummary> findInBounds(ListingBoundsRequest request, Long viewerUserId) {
        if (!request.hasBounds()) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        return listingDao.findInBounds(request, viewerUserId, request.safeLimit());
    }

    public List<ListingSummary> findMyListings(long hostUserId) {
        return listingDao.findByHost(hostUserId);
    }

    @Transactional(readOnly = true)
    public ListingDetailResponse getDetail(Long listingId, Long viewerUserId) {
        ListingDetail base = listingDao.findDetail(listingId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        List<String> images = listingDao.findImageUrls(listingId);
        List<String> options = listingDao.findOptionCodes(listingId);
        String aiSummary = listingDao.findAiSummary(listingId).orElse(null);
        boolean favorite = viewerUserId != null
            && favoriteDao.existsFavorite(viewerUserId, listingId);
        return ListingDetailResponse.from(base, images, options, aiSummary, favorite, viewerUserId != null);
    }

    @Transactional
    public void recordView(Long listingId) {
        listingDao.increaseViewCount(listingId);
    }

    @Transactional
    public void update(long hostUserId, Long listingId, ListingSaveRequest request) {
        validateProfanity(request.title(), request.description());
        verifyOwner(listingId, hostUserId);
        Listing listing = buildListing(listingId, hostUserId, request);
        listingDao.updateListing(listing);
        listingDao.deleteImagesByListing(listingId);
        listingDao.deleteOptionsByListing(listingId);
        saveImagesAndOptions(listingId, request.imageUrls(), request.optionCodes());
    }

    @Transactional
    public void updateStatusByHost(long hostUserId, Long listingId, String newStatus) {
        verifyOwner(listingId, hostUserId);
        listingDao.updateStatus(listingId, newStatus);
    }

    @Transactional
    public void delete(long hostUserId, Long listingId) {
        verifyOwner(listingId, hostUserId);
        listingDao.softDelete(listingId, hostUserId);
    }

    private Listing buildListing(Long listingId, long hostUserId, ListingSaveRequest request) {
        return Listing.builder()
            .listingId(listingId)
            .hostUserId(hostUserId)
            .title(request.title())
            .description(request.description())
            .roomType(request.roomType())
            .dealType(request.dealType())
            .deposit(request.deposit())
            .monthlyRent(request.monthlyRent() == null ? 0 : request.monthlyRent())
            .maintenanceFee(request.maintenanceFee() == null ? 0 : request.maintenanceFee())
            .areaM2(request.areaM2())
            .floor(request.floor())
            .totalFloor(request.totalFloor())
            .roomCount(request.roomCount() == null ? 1 : request.roomCount())
            .bathroomCount(request.bathroomCount() == null ? 1 : request.bathroomCount())
            .addressRoad(request.addressRoad())
            .addressDetail(request.addressDetail())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .status(STATUS_PENDING)
            .source(SOURCE_HOST)
            .brokerName(request.brokerName())
            .brokerPhone(request.brokerPhone())
            .brokerOfficePhone(request.brokerOfficePhone())
            .brokerOfficeName(request.brokerOfficeName())
            .build();
    }

    private void saveImagesAndOptions(Long listingId, List<String> imageUrls, List<String> optionCodes) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            listingDao.insertListingImages(listingId, imageUrls);
        }
        if (optionCodes != null && !optionCodes.isEmpty()) {
            listingDao.insertListingOptions(listingId, optionCodes);
        }
    }

    private void validateProfanity(String... texts) {
        for (String text : texts) {
            if (profanityFilter.containsProfanity(text)) {
                throw new CustomException(ErrorCode.ERR_PROFANITY);
            }
        }
    }

    private void verifyOwner(Long listingId, long hostUserId) {
        long ownerId = listingDao.findHostUserId(listingId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (ownerId != hostUserId) {
            throw new CustomException(ErrorCode.ERR_LISTING_NOT_OWNED);
        }
    }
}
