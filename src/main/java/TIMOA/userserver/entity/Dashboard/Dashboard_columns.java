package TIMOA.userserver.entity.Dashboard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(name = "board_columns")
public class Dashboard_columns {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "column_id")
    private Long columnId;

    @Column(name = "channel_id", nullable = false, insertable = false, updatable = false)
    private Long channelId;

    // 외래키 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Dashboard_channel dashboardChannel;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "position_order", nullable = false)
    private Integer positionOrder;

    @Column(name = "wip_limit", nullable = false)
    private Integer wipLimit;

    // 양방향 관계
    @OneToMany(mappedBy = "boardColumns", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dashboard_cards> boardCards = new ArrayList<>();

    // 기본 생성자
    public Dashboard_columns() {}

    // 생성자
    public Dashboard_columns(Long columnId, Long channelId, String columnName, Integer positionOrder, Integer wipLimit) {
        this.columnId = columnId;
        this.channelId = channelId;
        this.columnName = columnName;
        this.positionOrder = positionOrder;
        this.wipLimit = wipLimit;
    }
}
