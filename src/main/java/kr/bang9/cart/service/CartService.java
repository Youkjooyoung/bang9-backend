package kr.bang9.cart.service;

import kr.bang9.cart.dao.CartDao;
import kr.bang9.cart.domain.CartItem;
import kr.bang9.cart.dto.CartItemAddRequest;
import kr.bang9.cart.dto.CartItemUpdateRequest;
import kr.bang9.cart.dto.CartItemView;
import kr.bang9.cart.dto.CartResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartDao cartDao;

    @Transactional
    public CartItemView add(long userId, CartItemAddRequest request) {
        Long cartId = findOrCreateCart(userId);
        return cartDao.findExistingItem(cartId, request.productId(), request.productOptionId())
            .map(existing -> increaseQuantity(existing, request.quantity()))
            .orElseGet(() -> insertNewItem(cartId, request));
    }

    public CartResponse getCart(long userId) {
        Long cartId = cartDao.findCartIdByUser(userId).orElse(null);
        if (cartId == null) {
            return CartResponse.of(null, List.of());
        }
        List<CartItemView> items = cartDao.findItemsByCart(cartId);
        return CartResponse.of(cartId, items);
    }

    public int getItemCount(long userId) {
        return cartDao.findCartIdByUser(userId)
            .map(cartDao::countItemsByCart)
            .orElse(0);
    }

    @Transactional
    public void updateQuantity(long userId, Long cartItemId, CartItemUpdateRequest request) {
        verifyItemOwner(userId, cartItemId);
        cartDao.updateItemQuantity(cartItemId, request.quantity());
    }

    @Transactional
    public void removeItem(long userId, Long cartItemId) {
        verifyItemOwner(userId, cartItemId);
        cartDao.deleteItem(cartItemId);
    }

    @Transactional
    public void clear(long userId) {
        cartDao.findCartIdByUser(userId).ifPresent(cartDao::deleteItemsByCart);
    }

    private Long findOrCreateCart(long userId) {
        return cartDao.findCartIdByUser(userId).orElseGet(() -> {
            cartDao.insertCart(userId);
            return cartDao.findCartIdByUser(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
        });
    }

    private CartItemView increaseQuantity(CartItem existing, int addQuantity) {
        int nextQuantity = Math.min(999, existing.getQuantity() + addQuantity);
        cartDao.updateItemQuantity(existing.getCartItemId(), nextQuantity);
        return findViewById(existing.getCartItemId());
    }

    private CartItemView insertNewItem(Long cartId, CartItemAddRequest request) {
        CartItem item = CartItem.builder()
            .cartId(cartId)
            .productId(request.productId())
            .productOptionId(request.productOptionId())
            .quantity(request.quantity())
            .sourceListingId(request.sourceListingId())
            .build();
        cartDao.insertCartItem(item);
        return findViewById(item.getCartItemId());
    }

    private CartItemView findViewById(Long cartItemId) {
        Long cartId = cartDao.findCartIdByItem(cartItemId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        return cartDao.findItemsByCart(cartId).stream()
            .filter(view -> view.cartItemId().equals(cartItemId))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    private void verifyItemOwner(long userId, Long cartItemId) {
        Long cartId = cartDao.findCartIdByItem(cartItemId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        Long userCartId = cartDao.findCartIdByUser(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_FORBIDDEN));
        if (!cartId.equals(userCartId)) {
            throw new CustomException(ErrorCode.ERR_FORBIDDEN);
        }
    }
}
