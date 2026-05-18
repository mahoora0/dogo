package com.example.dogo.repository.user;

import com.example.dogo.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByPhone(String phone);
	Optional<User> findByLoginId(String loginId);
	boolean existsByNickname(String nickname);
	boolean existsByLoginId(String loginId);
	List<User> findAllByOrderByUserNoDesc();
	List<User> findByStatusAndWithdrawnAtBefore(String status, java.time.LocalDateTime dateTime);
}
