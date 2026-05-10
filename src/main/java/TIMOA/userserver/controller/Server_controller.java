package TIMOA.userserver.controller;

import TIMOA.userserver.DTO.Dashboard.dashboard_get_channel_request;
import TIMOA.userserver.DTO.server_members_image_dto;
import TIMOA.userserver.DTO.servers_dto;
import TIMOA.userserver.entity.Servers;
import TIMOA.userserver.service.Server_member_service;
import TIMOA.userserver.service.Server_service;
import TIMOA.userserver.service.Sse_service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/servers")
public class Server_controller {

    @Autowired
    private Server_service servers_service;

    @Autowired
    private Server_member_service server_member_service;

    @Autowired
    private Sse_service sse_service;

    /*
     * 서버 생성
     */
    @PostMapping("/create")
    public ResponseEntity<?> create_server(@RequestBody Servers server) {
        try {
            servers_dto create_server = servers_service.create_server(server);

            log.info("서버 생성 요청: {} by {}", create_server.getServerName(), create_server.getOwnerId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "서버가 성공적으로 생성되었습니다.",
                    "server", create_server
            ));
        } catch (Exception e) {
            log.error("서버 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 서버 수정
     */
    @PostMapping("/update")
    public ResponseEntity<?> update_server(@RequestBody Servers server,
                                           @RequestParam String user_id){
        try {
            servers_dto find_server = servers_service.update_server(server, user_id);

            sse_service.broadcastToServerMembers(find_server.getServerId(), "UPDATE_SERVER", find_server);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "server", find_server
            ));

        } catch (Exception e) {
            log.error("서버 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 서버 삭제 ( 서버 OWNER만 )
     */
    @DeleteMapping("/delete/{serverId}")
    public ResponseEntity<?> delete_server(@PathVariable Long serverId,
                                           @RequestParam String user_id) {
        try {
            List<server_members_image_dto> server_members = server_member_service.get_server_member(serverId);

            Servers delete_server = servers_service.get_delete_server_data(serverId, user_id);

            servers_service.delete_server(serverId, user_id);

            sse_service.broadcastToServerDelete(serverId, "DELETE_SERVER", servers_dto.fromEntity(delete_server), server_members);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "서버가 성공적으로 삭제되었습니다."
            ));

        } catch (Exception e) {
            log.error("서버 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 사용자 서버 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> get_user_servers(@PathVariable String userId) {
        try {
            List<servers_dto> servers = servers_service.get_user_servers(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "servers", servers
            ));

        } catch (Exception e) {
            log.error("사용자 서버 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 서버의 멤버 조회
     */
    @GetMapping("/members")
    public ResponseEntity<?> get_server_member(@RequestParam Long serverId){
        try {
            List<server_members_image_dto> serverMembers = server_member_service.get_server_member(serverId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "members", serverMembers
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 초대 코드 요청
     */
    @GetMapping("/get_invite_code/{serverId}")
    public ResponseEntity<?> get_invite_code(@PathVariable Long serverId){
        try {
            return servers_service.get_invite_code(serverId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 초대 코드로 서버 참가
     */
    @PostMapping("/join")
    public ResponseEntity<?> join_server(@RequestParam String invite_code,
                                         @RequestParam String user_id) {
        try{
            log.info("invite_code = '{}'", invite_code);
            if (user_id == null || invite_code == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "사용자 ID와 초대 코드가 필요합니다."));
            }

            return servers_service.join_server(user_id, invite_code);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 서버 탈퇴
     */
    @DeleteMapping("/leave")
    public ResponseEntity<?> leave_server(@RequestBody dashboard_get_channel_request request) {
        try{
            return servers_service.leave_server(request.getUserId(), request.getServerId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
     * 서버 강퇴
     */
    @DeleteMapping("/getout")
    public ResponseEntity<?> getout_server(@RequestParam String owner_id,
                                           @RequestBody dashboard_get_channel_request request) {
        try{
            return servers_service.getout_server(owner_id, request.getServerId(), request.getUserId());
        }catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}