package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.*;

/** Stored separately so ordinary user lookups do not load image bytes. */
@Entity
@Table(name = "account_avatars")
public class AccountAvatar {
    @Id
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Lob
    @Column(name = "image_data", nullable = false, length = 1048576)
    private byte[] image;

    protected AccountAvatar() {}
    public AccountAvatar(User user) { this.user = user; }
    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }
}
