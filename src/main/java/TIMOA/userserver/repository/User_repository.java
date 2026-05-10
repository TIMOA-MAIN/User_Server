package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface User_repository extends JpaRepository<Users, String> {
    @Query("SELECT u FROM Users u WHERE u.userId = :UserId AND u.password = :Password")
    Optional<Users> findByUserIdAndPassword(@Param("UserId") String UserId, @Param("Password") String Password);

    /*
     * 컬럼 아이디들에 맞는 모든 카드 조회
     */
    List<Users> findByUserIdIn(List<String> userIds);

    @Query("SELECT COUNT(u) > 0 FROM Users u WHERE u.email = :email AND u.userId != :userId")
    boolean existsByEmailAndUserIdNot(@Param("email") String email, @Param("userId") String userId);

    Optional<Users> findByEmail(@Param("email") String email);
}