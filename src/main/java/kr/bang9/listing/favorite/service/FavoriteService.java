package kr.bang9.listing.favorite.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.listing.favorite.dao.FavoriteDao;
import kr.bang9.listing.favorite.dto.FavoriteListingSummary;
import kr.bang9.listing.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteDao favoriteDao;

    @Transactional
    public void addFavorite(long userId, long listingId) {
        try {
            int inserted = favoriteDao.insertFavorite(userId, listingId);
            if (inserted == 0) {
                throw new CustomException(ErrorCode.ERR_FAVORITE_ALREADY);
            }
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.ERR_FAVORITE_ALREADY);
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(long userId, long listingId) {
        return favoriteDao.existsFavorite(userId, listingId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriteListingSummary> getMyFavorites(long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, 50);
        int offset = safePage * safeSize;
        List<FavoriteListingSummary> content = favoriteDao.findMyFavorites(userId, offset, safeSize);
        long total = favoriteDao.countMyFavorites(userId);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    @Transactional
    public void removeFavorite(long userId, long listingId) {
        int deleted = favoriteDao.deleteFavorite(userId, listingId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.ERR_FAVORITE_NOT_FOUND);
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
