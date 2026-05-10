package TIMOA.userserver.DTO.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class dashboard_get_channel_request {
    private Long serverId;
    private String userId;
}
