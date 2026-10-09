package sg.edu.nus.iss.shoppingcart.dto;

import sg.edu.nus.iss.shoppingcart.entity.User;

/**
 * 页面/API 只展示这个对象，避免暴露 passwordHash 或整个 JPA 实体。
 * @author luopeiwen
 */
public record AuthenticatedUser(Long id, String username, String displayName,
                                String email, User.Role role) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), user.getRole());
    }
    public boolean isAdmin() { return role == User.Role.ADMIN; }
}
