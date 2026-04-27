package kr.bang9.listing.favorite.controller;

import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.listing.dto.PageResponse;
import kr.bang9.listing.favorite.dto.FavoriteListingSummary;
import kr.bang9.listing.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ResponseEntity<Void> add(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId
    ) {
        favoriteService.addFavorite(principal.userId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<FavoriteListingSummary>> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(principal.userId(), page, size));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> remove(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("listingId") Long listingId
    ) {
        favoriteService.removeFavorite(principal.userId(), listingId);
        return ResponseEntity.noContent().build();
    }
}
