package TIMOA.userserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "notion_channel")
public class NotionChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Long channelId;

    // serverId 필드 제거 - server 객체를 통해 접근

    @Column(name = "page_title", length = 500)
    private String pageTitle;

    // Servers와의 다대일 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Servers server;

    // NotionBlock과의 일대다 관계 설정 (채널 삭제 시 블록들도 함께 삭제)
    @OneToMany(mappedBy = "notionChannel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotionBlock> notionBlocks = new ArrayList<>();

    // 기본 생성자
    public NotionChannel() {}

    // 매개변수 생성자
    public NotionChannel(Long channelId, String pageTitle) {
        this.channelId = channelId;
        this.pageTitle = pageTitle;
    }

    // 매개변수 생성자 (Server 객체 포함)
    public NotionChannel(Servers server, String pageTitle) {
        this.server = server;
        this.pageTitle = pageTitle;
    }

    @Override
    public String toString() {
        return "NotionChannel{" +
                "channelId=" + channelId +
                ", serverId=" + server.getServerId() +
                ", pageTitle='" + pageTitle + '\'' +
                ", blocksCount=" + (notionBlocks != null ? notionBlocks.size() : 0) +
                '}';
    }
}