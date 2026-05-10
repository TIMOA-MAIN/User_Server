package TIMOA.userserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;


@Entity
@Getter
@Setter
@ToString
@Table(name = "chat_channels")
public class Chat_channels {

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


    @Column(name = "channel_name", columnDefinition = "nvarchar(100)", nullable = false, length = 100)
    private String channelName;


    @OneToMany(mappedBy = "chatChannel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Chat_messages> messages = new ArrayList<>();


    // 기본 생성자
    public Chat_channels() {}

    // 생성자
    public Chat_channels(Long serverId, String channelName) {
        this.serverId = serverId;
        this.channelName = channelName;
    }
}