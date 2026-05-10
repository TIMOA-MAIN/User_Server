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
@Table(name = "chat_messages")
public class Chat_messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "channel_id", nullable = false, insertable = false, updatable = false)
    private Long channelId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    @ToString.Exclude
    private Chat_channels chatChannel;


    @Column(name = "sender_id", columnDefinition = "nvarchar(50)", length = 50, insertable = false, updatable = false)
    private String senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @ToString.Exclude
    private Users user;

    @Column(name = "message_type",nullable = false, length = 255)
    private String messageType;

    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "image_size")
    private Long imageSize;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;


}
