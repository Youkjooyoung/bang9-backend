package kr.bang9.product.dao;

import kr.bang9.product.dto.CategoryFlat;
import kr.bang9.product.dto.ProductDetailRow;
import kr.bang9.product.dto.ProductOption;
import kr.bang9.product.dto.ProductSearchRequest;
import kr.bang9.product.dto.ProductSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductDao {

    List<ProductSummary> search(
        @Param("q") ProductSearchRequest request,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countSearch(@Param("q") ProductSearchRequest request);

    Optional<ProductDetailRow> findDetail(@Param("productId") Long productId);

    List<String> findImageUrls(@Param("productId") Long productId);

    List<ProductOption> findOptions(@Param("productId") Long productId);

    List<CategoryFlat> findAllCategories();

    Optional<Integer> lockProductStock(@Param("productId") Long productId);

    Optional<Integer> lockOptionStock(@Param("productOptionId") Long productOptionId);

    int decreaseProductStock(
        @Param("productId") Long productId,
        @Param("quantity") Integer quantity
    );

    int decreaseOptionStock(
        @Param("productOptionId") Long productOptionId,
        @Param("quantity") Integer quantity
    );

    int increaseProductStock(
        @Param("productId") Long productId,
        @Param("quantity") Integer quantity
    );

    int increaseOptionStock(
        @Param("productOptionId") Long productOptionId,
        @Param("quantity") Integer quantity
    );
}
