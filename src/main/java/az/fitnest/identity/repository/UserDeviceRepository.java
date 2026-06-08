package az.fitnest.identity.repository;

import az.fitnest.identity.model.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);
}
