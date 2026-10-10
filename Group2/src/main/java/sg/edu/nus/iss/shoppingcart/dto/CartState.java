package sg.edu.nus.iss.shoppingcart.dto;

import java.util.Map;

/** Immutable snapshot of the current authenticated session's cart. */
public record CartState(String cartFormToken, Map<Long, Integer> quantities, int totalQuantity) {}
