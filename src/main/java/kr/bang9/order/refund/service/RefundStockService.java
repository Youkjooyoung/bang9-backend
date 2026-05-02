package kr.bang9.order.refund.service;

import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.product.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundStockService {

    private final OrderDao orderDao;
    private final ProductDao productDao;

    public void restore(Long orderId) {
        List<OrderItemView> items = orderDao.findItemsByOrder(orderId);
        for (OrderItemView item : items) {
            if (item.productOptionId() != null) {
                productDao.increaseOptionStock(item.productOptionId(), item.quantity());
            } else {
                productDao.increaseProductStock(item.productId(), item.quantity());
            }
        }
    }
}
