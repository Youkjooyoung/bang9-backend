package kr.bang9.order.dao;

import kr.bang9.order.domain.Order;
import kr.bang9.order.domain.OrderItem;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.order.dto.OrderListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderDao {

    void insertOrder(Order order);

    void insertOrderItem(OrderItem orderItem);

    Optional<Order> findDetailByCode(
        @Param("userId") Long userId,
        @Param("orderCode") String orderCode
    );

    Optional<Order> findByCode(@Param("orderCode") String orderCode);

    Optional<Long> lockOrderForUpdate(@Param("orderId") Long orderId);

    List<OrderItemView> findItemsByOrder(@Param("orderId") Long orderId);

    List<OrderListItem> findListByUser(
        @Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countByUser(@Param("userId") Long userId);

    void updateStatus(
        @Param("orderId") Long orderId,
        @Param("status") String status
    );
}
