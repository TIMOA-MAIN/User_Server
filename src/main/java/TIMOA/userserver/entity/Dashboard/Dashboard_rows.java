package TIMOA.userserver.entity.Dashboard;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(name = "section_rows")
public class Dashboard_rows {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "row_id")
    private Long rowId;

    @Column(name = "section_id", nullable = false, insertable = false, updatable = false)
    private Long sectionId;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "task_status")
    private Integer taskStatus;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "priority")
    private String priority;

    @Column(name = "position_order")
    private Integer positionOrder;

    // 외래키 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Dashboard_sections boardSections;

    // 양방향 관계
    @OneToMany(mappedBy = "dashboardRow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Task_assignees> taskAssignees = new ArrayList<>();

    // 기본 생성자
    public Dashboard_rows() {}

    // 생성자
    public Dashboard_rows(Long rowId, Long sectionId, String taskName, Integer taskStatus,
                          LocalDate startDate, LocalDate endDate, String priority, Integer positionOrder) {
        this.rowId = rowId;
        this.sectionId = sectionId;
        this.taskName = taskName;
        this.taskStatus = taskStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
        this.positionOrder = positionOrder;
    }
}
