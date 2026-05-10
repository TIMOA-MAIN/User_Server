package TIMOA.userserver.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notion_block")
public class NotionBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    @JsonProperty("blockId")
    private Long blockId;

    // channelId 필드 제거 - notionChannel 객체를 통해 접근

    @Column(name = "type", length = 50, nullable = false)
    @JsonProperty("type")
    private String type = "text";

    @Column(name = "content", columnDefinition = "TEXT")
    @JsonProperty("content")
    private String content = "";

    // URL 컬럼을 TEXT 타입으로 변경 (Base64 이미지 데이터 저장을 위해)
    @Column(name = "url", columnDefinition = "TEXT")
    @JsonProperty("url")
    private String url;

    @Column(name = "position_order", nullable = false)
    @JsonProperty("positionOrder")
    private Integer positionOrder = 0;

    // NotionChannel과의 다대일 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private NotionChannel notionChannel;

    // 기본 생성자
    public NotionBlock() {}

    // 매개변수 생성자
    public NotionBlock(Long blockId, String type, String content,
                       String url, String alt, Integer positionOrder) {
        this.blockId = blockId;
        this.type = (type != null && !type.equals("null")) ? type : "text";
        this.content = (content != null) ? content : "";
        this.url = (url != null && !url.equals("null")) ? url : null;
        this.positionOrder = (positionOrder != null) ? positionOrder : 0;
    }

    // 매개변수 생성자 (NotionChannel 객체 포함)
    public NotionBlock(NotionChannel notionChannel, String type, String content,
                       String url, String alt, Integer positionOrder) {
        this.notionChannel = notionChannel;
        this.type = (type != null && !type.equals("null")) ? type : "text";
        this.content = (content != null) ? content : "";
        this.url = (url != null && !url.equals("null")) ? url : null;
        this.positionOrder = (positionOrder != null) ? positionOrder : 0;
    }

    // JPA Pre-persist/update 시 기본값 설정 및 검증
    @PrePersist
    @PreUpdate
    private void validateAndSetDefaults() {
        if (this.type == null || "null".equals(this.type) || this.type.trim().isEmpty()) {
            this.type = "text";
        }
        if (this.content == null) {
            this.content = "";
        }
        if (this.positionOrder == null) {
            this.positionOrder = 0;
        }
        // url과 alt에서 "null" 문자열 제거
        if ("null".equals(this.url)) {
            this.url = null;
        }
    }

    @Override
    public String toString() {
        return "NotionBlock{" +
                "blockId=" + blockId +
                ", channelId=" + notionChannel.getChannelId() +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", url='" + (url != null ? url.substring(0, Math.min(50, url.length())) + "..." : "null") + '\'' +
                ", positionOrder=" + positionOrder +
                '}';
    }
}