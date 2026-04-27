package kr.bang9.product.dto;

import java.util.ArrayList;
import java.util.List;

public record CategoryNode(
    Integer categoryId,
    Integer parentId,
    String name,
    Integer depth,
    Integer sortOrder,
    List<CategoryNode> children
) {
    public static CategoryNode of(Integer id, Integer parentId, String name, Integer depth, Integer sortOrder) {
        return new CategoryNode(id, parentId, name, depth, sortOrder, new ArrayList<>());
    }
}
