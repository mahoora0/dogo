package com.example.dogo.repository.user;

import com.example.dogo.entity.user.UserSocialAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
