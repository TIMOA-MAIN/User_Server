package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Servers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Servers_repository extends JpaRepository<Servers, Long> {
    Optional<Servers> findByInviteCode(String inviteCode);

    List<Servers> findByOwnerId(String ownerId);
}