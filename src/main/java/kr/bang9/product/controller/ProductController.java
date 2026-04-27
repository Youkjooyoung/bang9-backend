package kr.bang9.product.controller;

import kr.bang9.listing.dto.PageResponse;
import kr.bang9.product.dto.CategoryNode;
import kr.bang9.product.dto.ProductDetail;
import kr.bang9.product.dto.ProductSearchRequest;
import kr.bang9.product.dto.ProductSummary;
import kr.bang9.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummary>> search(@ModelAttribute ProductSearchRequest request) {
        return ResponseEntity.ok(productService.search(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryNode>> categories() {
        return ResponseEntity.ok(productService.getCategoryTree());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetail> detail(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(productService.getDetail(productId));
    }
}
