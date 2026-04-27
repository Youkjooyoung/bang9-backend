package kr.bang9.product.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.listing.dto.PageResponse;
import kr.bang9.product.dao.ProductDao;
import kr.bang9.product.dto.CategoryFlat;
import kr.bang9.product.dto.CategoryNode;
import kr.bang9.product.dto.ProductDetail;
import kr.bang9.product.dto.ProductDetailRow;
import kr.bang9.product.dto.ProductOption;
import kr.bang9.product.dto.ProductSearchRequest;
import kr.bang9.product.dto.ProductSummary;
import kr.bang9.product.review.dao.ProductReviewDao;
import kr.bang9.product.review.dto.ReviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductDao productDao;
    private final ProductReviewDao productReviewDao;

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> search(ProductSearchRequest request) {
        int page = request.safePage();
        int size = request.safeSize();
        int offset = page * size;
        List<ProductSummary> content = productDao.search(request, offset, size);
        long total = productDao.countSearch(request);
        return PageResponse.of(content, page, size, total);
    }

    @Transactional(readOnly = true)
    public ProductDetail getDetail(Long productId) {
        ProductDetailRow base = productDao.findDetail(productId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        List<String> images = productDao.findImageUrls(productId);
        List<ProductOption> options = productDao.findOptions(productId);
        ReviewSummary summary = productReviewDao.findSummaryByProduct(productId);
        Double averageRating = summary == null ? 0.0 : summary.averageRating();
        Integer reviewCount = summary == null ? 0 : summary.reviewCount();
        return new ProductDetail(
            base.productId(), base.categoryId(), base.categoryName(),
            base.name(), base.brand(), base.price(), base.salePrice(),
            base.stock(), base.status(),
            base.widthCm(), base.depthCm(), base.heightCm(),
            base.weightKg(), base.shippingFee(), base.descriptionHtml(),
            images, options, averageRating, reviewCount
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryNode> getCategoryTree() {
        List<CategoryFlat> flats = productDao.findAllCategories();
        return buildTree(flats);
    }

    private List<CategoryNode> buildTree(List<CategoryFlat> flats) {
        Map<Integer, CategoryNode> map = new HashMap<>();
        List<CategoryNode> roots = new ArrayList<>();
        for (CategoryFlat flat : flats) {
            CategoryNode node = CategoryNode.of(
                flat.categoryId(), flat.parentId(),
                flat.name(), flat.depth(), flat.sortOrder()
            );
            map.put(flat.categoryId(), node);
        }
        for (CategoryFlat flat : flats) {
            CategoryNode node = map.get(flat.categoryId());
            if (flat.parentId() == null) {
                roots.add(node);
            } else {
                CategoryNode parent = map.get(flat.parentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }
}
