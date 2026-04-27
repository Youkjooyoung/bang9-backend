package kr.bang9.listing.favorite.dao;

import kr.bang9.listing.favorite.dto.FavoriteListingSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteDao {

    int insertFavorite(
        @Param("userId") long userId,
        @Param("listingId") long listingId
    );

    boolean existsFavorite(
        @Param("userId") long userId,
        @Param("listingId") long listingId
    );

    List<FavoriteListingSummary> findMyFavorites(
        @Param("userId") long userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countMyFavorites(@Param("userId") long userId);

    int deleteFavorite(
        @Param("userId") long userId,
        @Param("listingId") long listingId
    );
}
