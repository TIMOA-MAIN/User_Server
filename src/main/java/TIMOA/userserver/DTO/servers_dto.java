package TIMOA.userserver.DTO;

import TIMOA.userserver.entity.Servers;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class servers_dto {
    private Long serverId;

    private String serverName;

    private String description;

    private String ownerId;

    private String inviteCode;

    private Integer maxMembers;

    private LocalDateTime createdAt;

    public static servers_dto fromEntity(Servers s) {
        return new servers_dto(
                s.getServerId(),
                s.getServerName(),
                s.getDescription(),
                s.getOwnerId(),
                s.getInviteCode(),
                s.getMaxMembers(),
                s.getCreatedAt());
    }
}
