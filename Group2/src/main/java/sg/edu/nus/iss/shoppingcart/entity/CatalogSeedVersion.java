package sg.edu.nus.iss.shoppingcart.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Completed migrations survive product deletion, so restart never recreates removed products. */
@Entity @Table(name = "catalog_seed_versions")
public class CatalogSeedVersion {
    @Id @Column(length = 60) private String version;
    @Column(nullable = false) private LocalDateTime appliedAt;
    protected CatalogSeedVersion() {}
    public CatalogSeedVersion(String version) { this.version = version; this.appliedAt = LocalDateTime.now(); }
    public String getVersion() { return version; }
}
