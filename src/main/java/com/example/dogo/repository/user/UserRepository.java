package com.example.dogo.repository.user;

import com.example.dogo.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {

	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	Optional<User> findByLoginId(String loginId);
	boolean existsByNickname(String nickname);
	boolean existsByLoginId(String loginId);
	List<User> findAllByOrderByUserNoDesc();
	Page<User> findAllByOrderByUserNoDesc(Pageable pageable);
	List<User> findByStatusAndWithdrawnAtBefore(String status, java.time.LocalDateTime dateTime);
}
