package TIMOA.userserver.DTO;

import TIMOA.userserver.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class server_members_image_dto {
    private String userId;

    private String userName;

    private String email;

    private String nickName;

    private String profileImage;

    private LocalDateTime createdAt;

    private String status;

    public static server_members_image_dto fromEntity(Users u, String profile_image) {
        return new server_members_image_dto(
                u.getUserId(),
                u.getUserName(),
                u.getEmail(),
                u.getNickName(),
                profile_image,
                u.getCreatedAt(),
                u.getStatus().toString());
    }
}
