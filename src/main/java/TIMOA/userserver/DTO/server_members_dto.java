package TIMOA.userserver.DTO;

import TIMOA.userserver.entity.Server_members;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class server_members_dto {
    private Long memberId;

    private Long serverId;

    private String userId;

    private String role;

    private LocalDateTime joinedAt;

    private Boolean isActive;

    public static server_members_dto fromEntity(Server_members s) {
        return new server_members_dto(
                s.getMemberId(),
                s.getServer().getServerId(),
                s.getUserId(),
                s.getRole(),
                s.getJoinedAt(),
                s.getIsActive()
        );
    }
}
