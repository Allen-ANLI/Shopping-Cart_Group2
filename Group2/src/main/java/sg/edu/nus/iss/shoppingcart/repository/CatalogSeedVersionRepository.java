package sg.edu.nus.iss.shoppingcart.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.CatalogSeedVersion;
public interface CatalogSeedVersionRepository extends JpaRepository<CatalogSeedVersion, String> {}
