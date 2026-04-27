package kr.bang9.cart.dao;

import kr.bang9.cart.domain.CartItem;
import kr.bang9.cart.dto.CartItemView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CartDao {

    void insertCart(@Param("userId") Long userId);

    void insertCartItem(CartItem cartItem);

    Optional<Long> findCartIdByUser(@Param("userId") Long userId);

    Optional<CartItem> findExistingItem(
        @Param("cartId") Long cartId,
        @Param("productId") Long productId,
        @Param("productOptionId") Long productOptionId
    );

    Optional<Long> findCartIdByItem(@Param("cartItemId") Long cartItemId);

    List<CartItemView> findItemsByCart(@Param("cartId") Long cartId);

    int countItemsByCart(@Param("cartId") Long cartId);

    void updateItemQuantity(
        @Param("cartItemId") Long cartItemId,
        @Param("quantity") Integer quantity
    );

    void deleteItem(@Param("cartItemId") Long cartItemId);

    void deleteItemsByCart(@Param("cartId") Long cartId);
}
