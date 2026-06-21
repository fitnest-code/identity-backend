package az.fitnest.identity.repository;

import az.fitnest.identity.model.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserDevice ud WHERE ud.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
