package kr.bang9.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bang9.cart.dto.CartItemView;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.order.dto.AddressSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderSnapshotSerializer {

    private final ObjectMapper objectMapper;

    public String address(AddressSnapshot address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, "주소 직렬화에 실패했습니다.");
        }
    }

    public String product(CartItemView item) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("productId", item.productId());
            snapshot.put("productName", item.productName());
            snapshot.put("brand", item.brand());
            snapshot.put("salePrice", item.salePrice());
            snapshot.put("additionalPrice", item.additionalPrice());
            snapshot.put("optionType", item.optionType());
            snapshot.put("optionValue", item.optionValue());
            snapshot.put("coverImageUrl", item.coverImageUrl());
            snapshot.put("sourceListingId", item.sourceListingId());
            snapshot.put("sourceListingTitle", item.sourceListingTitle());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, "상품 스냅샷 저장에 실패했습니다.");
        }
    }
}
