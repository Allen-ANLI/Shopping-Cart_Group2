package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.ShippingAddress;
import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByUser_IdOrderByDefaultAddressDescIdAsc(Long userId);
    Optional<ShippingAddress> findByIdAndUser_Id(Long id, Long userId);
}
