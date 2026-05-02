package kr.bang9.order.service;

import kr.bang9.cart.dto.CartItemView;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.product.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderStockService {

    private final ProductDao productDao;

    public void validateStock(List<CartItemView> items) {
        for (CartItemView item : items) {
            int stock = item.productOptionId() != null
                ? productDao.lockOptionStock(item.productOptionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "상품 옵션이 존재하지 않습니다."))
                : productDao.lockProductStock(item.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "상품이 존재하지 않습니다."));
            if (stock < item.quantity()) {
                throw new CustomException(ErrorCode.ERR_STOCK_NOT_ENOUGH);
            }
        }
    }

    public void decreaseOrderedItems(List<OrderItemView> items) {
        for (OrderItemView item : items) {
            decreaseStock(item.productId(), item.productOptionId(), item.quantity());
        }
    }

    private void decreaseStock(Long productId, Long productOptionId, int quantity) {
        int affected = productOptionId != null
            ? productDao.decreaseOptionStock(productOptionId, quantity)
            : productDao.decreaseProductStock(productId, quantity);
        if (affected == 0) {
            throw new CustomException(ErrorCode.ERR_STOCK_NOT_ENOUGH);
        }
    }
}
