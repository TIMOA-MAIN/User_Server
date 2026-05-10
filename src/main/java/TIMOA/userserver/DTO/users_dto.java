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
public class users_dto {

    private String UserId;

    private String UserName;

    private String Email;

    private String NickName;

    private LocalDateTime CreatedAt;

    public static users_dto fromEntity(Users u) {
        return new users_dto(
                u.getUserId(),
                u.getUserName(),
                u.getEmail(),
                u.getNickName(),
                u.getCreatedAt());
    }
}
