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
@Table(name = "board_sections")
public class Dashboard_sections {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "channel_id", nullable = false, insertable = false, updatable = false)
    private Long channelId;

    @Column(name = "section_name", nullable = false)
    private String sectionName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "position_order", nullable = false)
    private Integer positionOrder;

    // 외래키 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Dashboard_channel dashboardChannel;

    // 양방향 관계
    @OneToMany(mappedBy = "boardSections", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dashboard_rows> sectionRows = new ArrayList<>();

    // 기본 생성자
    public Dashboard_sections() {}

    // 생성자
    public Dashboard_sections(Long sectionId, Long channelId, String sectionName, LocalDate startDate, LocalDate endDate, Integer positionOrder) {
        this.sectionId = sectionId;
        this.channelId = channelId;
        this.sectionName = sectionName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.positionOrder = positionOrder;
    }

}
