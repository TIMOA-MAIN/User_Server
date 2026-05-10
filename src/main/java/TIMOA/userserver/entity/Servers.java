package TIMOA.userserver.entity;

import TIMOA.userserver.entity.Dashboard.Dashboard_channel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@ToString
@Table(name = "servers")
public class Servers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "server_name", columnDefinition = "nvarchar(100)", nullable = false, length = 100)
    private String serverName;

    @Column(name = "description", columnDefinition = "nvarchar(500)", length = 500)
    private String description;

    @Column(name = "owner_id", columnDefinition = "nvarchar(50)", length = 50, nullable = false)// insertable = false, updatable = false
    private String ownerId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false) //X insertable = false, updatable = false
    @ToString.Exclude
    private Users user;


    @Column(name = "invite_code", columnDefinition = "nvarchar(500)", unique = true, length = 50)
    private String inviteCode;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 서버에 속한 채팅 채널들과의 관계 설정
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Chat_channels> channels = new ArrayList<>();

    // 서버에 속한 대시보드 채널들과의 관계 설정
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Dashboard_channel> dashboardChannels = new ArrayList<>();

    // 서버에 속한 노션 채널들과의 관계 설정
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<NotionChannel> notionChannels = new ArrayList<>();

    // 서버의 멤버들과의 관계 설정
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Server_members> members = new ArrayList<>();


    // 기본 생성자
    public Servers() {}

    // 생성자
    public Servers(String serverName, String description, String ownerId, Integer maxMembers) {
        this.serverName = serverName;
        this.description = description;
        this.ownerId = ownerId;
        this.maxMembers = maxMembers != null ? maxMembers : 100;
    }
}