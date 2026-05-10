package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Dashboard.Task_assignees;
import TIMOA.userserver.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Dashboard_task_assignees_repository extends JpaRepository<Task_assignees, Long> {
    List<Task_assignees> findByUserId(String userId);

    String user(Users user);
}
