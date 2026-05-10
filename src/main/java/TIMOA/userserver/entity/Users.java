package TIMOA.userserver.entity;

import TIMOA.userserver.entity.Dashboard.Task_assignees;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(name = "users")
public class Users {

    @Id
    @Column(name = "user_id", columnDefinition = "nvarchar(50)", length = 50, nullable = false)
    private String userId;

    @Column(name = "user_name", columnDefinition = "nvarchar(100)", length = 100, nullable = false)
    private String userName;

    @Column(name = "email", columnDefinition = "nvarchar(255)", length = 255, unique = true)
    private String email;

    @Column(name = "password", columnDefinition = "nvarchar(255)", length = 255, nullable = false)
    private String password;

    @Column(name = "nick_name", columnDefinition = "nvarchar(100)", length = 100)
    private String nickName;

    @Column(name = "profile_image_url", columnDefinition = "nvarchar(500)", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @ColumnDefault("'OFFLINE'")
    private user_status status = user_status.OFFLINE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum user_status {
        ONLINE,
        OFFLINE
    }


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Servers> servers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Server_members> server_members = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Chat_messages> chat_messages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Task_assignees> task_assignees = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<NotionConnection> notionConnections = new ArrayList<>();


    public Users() {}

    public Users(String userId, String userName, String email, String password, String nickName, String profileImageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.email = email != null ? email : "";
        this.password = password;
        this.nickName = nickName != null ? nickName : "";
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
    }
}
