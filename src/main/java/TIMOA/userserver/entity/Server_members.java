package TIMOA.userserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Table(name = "server_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "user_id"}))
public class Server_members {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "server_id", nullable = false)  // insertable, updatable 제거
    private Long serverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", insertable = false, updatable = false)  // 읽기 전용으로 설정
    @ToString.Exclude
    private Servers server;

    @Column(name = "user_id", columnDefinition = "nvarchar(50)", length = 50, nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)  // 읽기 전용으로 설정
    @ToString.Exclude
    private Users user;

    @Column(name = "role", columnDefinition = "nvarchar(50)", length = 50)
    private String role = "MEMBER";

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // 기본 생성자
    public Server_members() {}

    // 생성자 수정
    public Server_members(Servers server, String userId) {
        this.server = server;
        this.serverId = server.getServerId();  // serverId도 직접 설정
        this.userId = userId;
        this.isActive = true;
    }

    // 편의 메서드 추가
    public void setServer(Servers server) {
        this.server = server;
        this.serverId = server != null ? server.getServerId() : null;
    }
}