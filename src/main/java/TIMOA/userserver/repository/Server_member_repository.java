package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Server_members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Server_member_repository extends JpaRepository<Server_members, Long> {

    /*
     * 특정 사용자 특정 서버의 활성 멤버인지 확인
     */
    Optional<Server_members> findByUserIdAndServerId(
            @Param("userId") String userId,
            @Param("serverId") Long serverId
    );

    List<Server_members> findByUserId(String userId);

    Long countByServerId(Long serverId);

    List<Server_members> findByServerId(Long serverId);

    List<Server_members> findByServerIdAndUserIdNot(Long serverId, String userId);
}