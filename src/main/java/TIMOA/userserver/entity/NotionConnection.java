package TIMOA.userserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "notion_connections")
@Getter
@Setter
public class NotionConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id")
    private Long connectionId;


    @Column(name = "user_id", columnDefinition = "nvarchar(50)", length = 50, nullable = false)
    private String userId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Users user;

    @Column(name = "access_token", nullable = false, length = 1000)
    private String accessToken;

    @Column(name = "workspace_id", length = 255)
    private String workspaceId;

    @Column(name = "workspace_name", length = 500)
    private String workspaceName;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    // 기본 생성자
    public NotionConnection() {
        this.connectedAt = LocalDateTime.now();
    }

    // 편의 생성자
    public NotionConnection(String userId, String accessToken, String workspaceId, String workspaceName) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.connectedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "NotionConnection{" +
                "connectionId=" + connectionId +
                ", userId='" + userId + '\'' +
                ", workspaceId='" + workspaceId + '\'' +
                ", workspaceName='" + workspaceName + '\'' +
                ", connectedAt=" + connectedAt +
                '}';
    }
}
