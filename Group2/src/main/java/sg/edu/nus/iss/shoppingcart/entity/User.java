package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 用户实体。
 *
 * <p>本类由 <b>B 维护</b>。E 读取三个字段：{@code id} 用于订单归属校验，
 * {@code displayName} 用于页面问候，{@code role} 用于后台权限判断。</p>
 *
 * <p>保持 A 的 {@code app_users} 表和已有 ID/password_hash/display_name 映射，
 * 只增加 email 和 role。普通用户采用 E 已约定的 CUSTOMER，而不是另设 USER。
 * role 的数据库默认值用于兼容 A 已有用户，不重建用户或改动 D 的外键。</p>
 *
 * @author luopeiwen (B integration; preserves A app_users/passwordHash/displayName)
 * @author 蔡千一（Module E）补 role 字段，仅供后台权限判断
 */
@Entity
@Table(name = "app_users")
public class User {

    /**
 * 用户角色。 */
    public enum Role {
        /**
 * 普通用户：可浏览、加购、结账、查看自己的订单。 */
        CUSTOMER,
        /**
 * 管理员：额外可进入 /admin 后台管理商品。 */
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
 * 邮箱，个人资料管理加分项使用。 */
    @Column(length = 255)
    private String email;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(length = 30)
    private String phone;

    private LocalDate birthday;

    /**
     * 用户角色。
     *
     * <p>E 的后台守卫读这个字段。注意不能只信Session 里缓存的角色对象，
     * 每次受保护请求都应按 ID 从数据库重新读取，这样数据库里撤销管理员身份后
     * 下一次请求就立即生效。</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'CUSTOMER'")
    private Role role = Role.CUSTOMER;

    public User() {
    }

    public User(String username, String passwordHash, String displayName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.email = email;
        this.role = Role.CUSTOMER;
    }

    /**
 * 是否为管理员。 */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
}
