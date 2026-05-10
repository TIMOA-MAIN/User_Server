package TIMOA.userserver.entity.Dashboard;

import TIMOA.userserver.entity.Servers;
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
@Table(name = "dashboard_channel")
public class Dashboard_channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "server_id", nullable = false, insertable = false, updatable = false)
    private Long serverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @ToString.Exclude
    private Servers server;

    @Column(name = "board_name", nullable = false)
    private String boardName;

    @Column(name = "description")
    private String description;

    // 양방향 관계
    @OneToMany(mappedBy = "dashboardChannel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dashboard_columns> boardColumns = new ArrayList<>();

    @OneToMany(mappedBy = "dashboardChannel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dashboard_sections> boardSections = new ArrayList<>();

    // 기본 생성자
    public Dashboard_channel() {}

    // 생성자
    public Dashboard_channel(Long channelId, Long serverId, String boardName, String description) {
        this.channelId = channelId;
        this.serverId = serverId;
        this.boardName = boardName;
        this.description = description;
    }
}
