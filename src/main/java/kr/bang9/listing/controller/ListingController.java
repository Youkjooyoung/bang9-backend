package kr.bang9.listing.controller;

import jakarta.validation.Valid;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.listing.dto.ListingBoundsRequest;
import kr.bang9.listing.dto.ListingClusterRequest;
import kr.bang9.listing.dto.ListingClusterResponse;
import kr.bang9.listing.dto.ListingCreateResponse;
import kr.bang9.listing.dto.ListingDetailResponse;
import kr.bang9.listing.dto.ListingSaveRequest;
import kr.bang9.listing.dto.ListingSearchRequest;
import kr.bang9.listing.dto.ListingStatusUpdateRequest;
import kr.bang9.listing.dto.ListingSummary;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.listing.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ListingCreateResponse> create(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody ListingSaveRequest request
    ) {
        return ResponseEntity.ok(listingService.create(principal.userId(), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ListingSummary>> search(
        @AuthenticationPrincipal AuthPrincipal principal,
        @ModelAttribute ListingSearchRequest request
    ) {
        Long viewerUserId = principal == null ? null : principal.userId();
        return ResponseEntity.ok(listingService.search(request, viewerUserId));
    }

    @GetMapping("/map")
    public ResponseEntity<List<ListingSummary>> mapBounds(
        @AuthenticationPrincipal AuthPrincipal principal,
        @ModelAttribute ListingSearchRequest request
    ) {
        Long viewerUserId = principal == null ? null : principal.userId();
        return ResponseEntity.ok(listingService.findByBounds(request, viewerUserId));
    }

    @GetMapping("/cluster")
    public ResponseEntity<List<ListingClusterResponse>> cluster(
        @ModelAttribute ListingClusterRequest request
    ) {
        return ResponseEntity.ok(listingService.findClusters(request));
    }

    @GetMapping("/in-bounds")
    public ResponseEntity<List<ListingSummary>> inBounds(
        @AuthenticationPrincipal AuthPrincipal principal,
        @ModelAttribute ListingBoundsRequest request
    ) {
        Long viewerUserId = principal == null ? null : principal.userId();
        return ResponseEntity.ok(listingService.findInBounds(request, viewerUserId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<List<ListingSummary>> myListings(
        @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(listingService.findMyListings(principal.userId()));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDetailResponse> detail(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId
    ) {
        Long viewerUserId = principal == null ? null : principal.userId();
        ListingDetailResponse response = listingService.getDetail(listingId, viewerUserId);
        try {
            listingService.recordView(listingId);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{listingId}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<Void> update(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId,
        @Valid @RequestBody ListingSaveRequest request
    ) {
        listingService.update(principal.userId(), listingId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{listingId}/status")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<Void> updateStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId,
        @Valid @RequestBody ListingStatusUpdateRequest request
    ) {
        listingService.updateStatusByHost(principal.userId(), listingId, request.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{listingId}")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId
    ) {
        listingService.delete(principal.userId(), listingId);
        return ResponseEntity.noContent().build();
    }
}
