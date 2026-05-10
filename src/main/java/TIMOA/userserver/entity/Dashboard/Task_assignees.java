package TIMOA.userserver.entity.Dashboard;

import TIMOA.userserver.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "task_assignees")
public class Task_assignees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "row_id", nullable = false, insertable = false, updatable = false)
    private Long rowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_id")
    @ToString.Exclude
    private Dashboard_rows dashboardRow;

    @Column(name = "user_id", columnDefinition = "nvarchar(50)", length = 50, nullable = false, insertable = false, updatable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private Users user;

    // 기본 생성자
    public Task_assignees() {}

    // 생성자
    public Task_assignees(Long rowId, String userId) {
        this.rowId = rowId;
        this.userId = userId;
    }
}