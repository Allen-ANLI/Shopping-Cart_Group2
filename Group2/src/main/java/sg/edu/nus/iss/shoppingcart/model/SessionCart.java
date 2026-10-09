package sg.edu.nus.iss.shoppingcart.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session 购物车。条目只保存商品 ID 和数量，不保存价格或 JPA 实体。
 * 所有读写由 CartService 在 session 锁内完成。
 * @author Letian Xie
 */
public final class SessionCart implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Long ownerUserId;
    private final Map<Long, Integer> quantities = new LinkedHashMap<>();
    private long revision;

    public SessionCart(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getOwnerUserId() { return ownerUserId; }
    public long getRevision() { return revision; }
    public Map<Long, Integer> getQuantities() { return Map.copyOf(quantities); }

    public void put(Long productId, int quantity) {
        quantities.put(productId, quantity);
        revision++;
    }

    public void remove(Long productId) {
        if (quantities.remove(productId) != null) { revision++; }
    }

    public void clear() {
        if (!quantities.isEmpty()) { quantities.clear(); revision++; }
    }
}
