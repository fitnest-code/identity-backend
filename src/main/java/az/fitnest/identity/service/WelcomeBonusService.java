package az.fitnest.identity.service;

import az.fitnest.identity.model.entity.User;

import java.util.List;

public interface WelcomeBonusService {

    void tryPublishWelcomeBonusEligible(User user);

    boolean isWelcomeBonusReceived(Long userId);

    void markWelcomeBonusReceived(Long userId);

    List<Long> findUserIdsPendingWelcomeBonus();
}
