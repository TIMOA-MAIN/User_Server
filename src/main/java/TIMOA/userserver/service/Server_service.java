package TIMOA.userserver.service;

import TIMOA.userserver.DTO.server_members_dto;
import TIMOA.userserver.DTO.servers_dto;
import TIMOA.userserver.entity.*;
import TIMOA.userserver.entity.Dashboard.Task_assignees;
import TIMOA.userserver.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class Server_service {

    @Autowired
    private Servers_repository servers_repository;

    @Autowired
    private Server_member_repository server_member_repository;

    @Autowired
    private Chat_channels_repository chat_channels_repository;

    @Autowired
    private User_repository user_repository;

    @Autowired
    private Chat_repository chat_repository;

    @Autowired
    private Dashboard_task_assignees_repository task_assignees_repository;

    /*
     * 서버 생성
     */
    public servers_dto create_server(Servers server) {
        try {
            String owner_id = server.getOwnerId();
            log.info("서버 생성 시도: serverName={}, ownerId={}", server.getServerName(), owner_id);

            if(user_repository.findById(owner_id).isEmpty()) {
                log.warn("서버 생성 실패 - 존재하지 않는 사용자: ownerId={}", owner_id);
                throw new Exception("없는 유저 정보입니다.");
            }

            String invite_code = generate_invite_code();
            server.setInviteCode(invite_code);

            Servers saved_server = servers_repository.save(server);
            log.info("서버 생성 성공: serverId={}, serverName={}, inviteCode={}",
                    saved_server.getServerId(), saved_server.getServerName(), invite_code);

            // 서버 소유자를 멤버로 추가
            Server_members member = new Server_members();
            member.setServer(saved_server);
            member.setUserId(saved_server.getOwnerId());
            server_member_repository.save(member);
            log.info("서버 소유자 멤버 추가: serverId={}, ownerId={}", saved_server.getServerId(), owner_id);

            // 기본 채널 생성
            Chat_channels general_channel = new Chat_channels();
            general_channel.setChannelName("채팅 채널");
            general_channel.setServer(saved_server);
            chat_channels_repository.save(general_channel);
            log.info("기본 채널 생성 완료: serverId={}, channelName={}",
                    saved_server.getServerId(), general_channel.getChannelName());

            return servers_dto.fromEntity(saved_server);

        } catch (Exception e) {
            log.error("서버 생성 중 오류 발생: serverName={}, ownerId={}, error={}",
                    server.getServerName(), server.getOwnerId(), e.getMessage());
            throw new RuntimeException("서버 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /*
     * 서버 수정 ( 서버 이름, 서버 설명, 서버 최대 인원 )
     */
    public servers_dto update_server(Servers server, String user_id){
        try {
            log.info("서버 수정 시도: serverId={}, userId={}", server.getServerId(), user_id);

            Servers find_server = servers_repository.findById(server.getServerId())
                    .orElseThrow(() -> new IllegalArgumentException("서버를 찾을 수 없습니다."));

            if(user_repository.findById(user_id).isEmpty()){
                log.warn("서버 수정 실패 - 존재하지 않는 사용자: userId={}", user_id);
                throw new Exception("사용자를 찾을 수 없습니다.");
            }

            if(server_member_repository.findByUserIdAndServerId(user_id, server.getServerId()).isEmpty()) {
                log.warn("서버 수정 실패 - 권한 없음: serverId={}, userId={}", server.getServerId(), user_id);
                throw new Exception("해당 서버의 멤버가 아닙니다.");
            }

            // 변경 내용 로깅
            log.info("서버 정보 변경: serverId={}, oldName={}, newName={}, oldDesc={}, newDesc={}, oldMaxMembers={}, newMaxMembers={}",
                    find_server.getServerId(), find_server.getServerName(), server.getServerName(),
                    find_server.getDescription(), server.getDescription(),
                    find_server.getMaxMembers(), server.getMaxMembers());

            find_server.setServerName(server.getServerName());
            find_server.setDescription(server.getDescription());
            find_server.setMaxMembers(server.getMaxMembers());

            Servers updated_server = servers_repository.save(find_server);
            log.info("서버 수정 성공: serverId={}, serverName={}", updated_server.getServerId(), updated_server.getServerName());

            return servers_dto.fromEntity(updated_server);

        } catch (Exception e) {
            log.error("서버 수정 중 오류 발생: serverId={}, userId={}, error={}",
                    server.getServerId(), user_id, e.getMessage());
            throw new RuntimeException("서버 수정에 실패했습니다: " + e.getMessage());
        }
    }

    /*
     * 서버 삭제
     */
    public void delete_server(Long server_id, String user_id){
        try {
            log.info("서버 삭제 시도: serverId={}, userId={}", server_id, user_id);

            Servers find_server = servers_repository.findById(server_id)
                    .orElseThrow(() -> new IllegalArgumentException("서버를 찾을 수 없습니다."));

            String server_name = find_server.getServerName();
            Long member_count = server_member_repository.countByServerId(server_id);

            servers_repository.delete(find_server);
            log.info("서버 삭제 완료: serverId={}, serverName={}, deletedMemberCount={}",
                    server_id, server_name, member_count);

        } catch (Exception e) {
            log.error("서버 삭제 중 오류 발생: serverId={}, userId={}, error={}",
                    server_id, user_id, e.getMessage());
            throw new RuntimeException("서버 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /*
     * 사용자 속한 서버 목록 조회
     */
    public List<servers_dto> get_user_servers(String user_id) {
        try {
            log.debug("사용자 서버 목록 조회: userId={}", user_id);

            List<Server_members> user_join_servers = server_member_repository.findByUserId(user_id);
            List<Servers> servers = new ArrayList<>();

            for(Server_members s : user_join_servers){
                if(servers_repository.findById(s.getServerId()).isPresent()) {
                    Servers server = servers_repository.findById(s.getServerId()).get();
                    servers.add(server);
                }
            }

            List<servers_dto> servers_dtos = new ArrayList<>();
            for(Servers s : servers){
                servers_dtos.add(servers_dto.fromEntity(s));
            }

            log.info("사용자 서버 목록 조회 성공: userId={}, serverCount={}", user_id, servers.size());
            return servers_dtos;

        } catch (Exception e) {
            log.error("사용자 서버 목록 조회 중 오류 발생: userId={}, error={}", user_id, e.getMessage());
            return List.of();
        }
    }

    /*
     * 초대 코드로 서버 참가
     */
    public ResponseEntity<?> join_server(String user_id, String invite_code){
        try{
            log.info("서버 참가 시도: userId={}, inviteCode={}", user_id, invite_code);

            Users user = user_repository.findById(user_id).orElseThrow(() -> new Exception("서버 참가 실패 - 존재하지 않는 사용자"));

            Servers find_server = servers_repository.findByInviteCode(invite_code)
                    .orElseThrow(() -> new IllegalArgumentException("서버 코드에 맞는 서버를 찾을 수 없습니다."));

            log.info("초대 코드로 서버 찾기 성공: serverId={}, serverName={}, inviteCode={}",
                    find_server.getServerId(), find_server.getServerName(), invite_code);

            if(server_member_repository.findByUserIdAndServerId(user_id, find_server.getServerId()).isPresent()) {
                log.warn("서버 참가 실패 - 이미 참가한 서버: userId={}, serverId={}", user_id, find_server.getServerId());
                throw new Exception("이미 참가한 서버입니다.");
            }

            Long current_member_count = server_member_repository.countByServerId(find_server.getServerId());
            if(current_member_count >= find_server.getMaxMembers()) {
                log.warn("서버 참가 실패 - 인원 초과: serverId={}, currentCount={}, maxMembers={}",
                        find_server.getServerId(), current_member_count, find_server.getMaxMembers());
                throw new Exception("서버 인원이 가득 찼습니다.");
            }

            Server_members server_members = new Server_members(find_server, user_id);
            server_members.setUser(user);

            Server_members save_server_member = server_member_repository.save(server_members);
            log.info("서버 참가 성공: userId={}, serverId={}, serverName={}, memberCount={}",
                    user_id, find_server.getServerId(), find_server.getServerName(), current_member_count + 1);

            server_members_dto send_member_data = server_members_dto.fromEntity(save_server_member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "서버에 성공적으로 참가했습니다.",
                    "server", send_member_data
            ));
        } catch (Exception e) {
            log.error("서버 참가 중 오류 발생: userId={}, inviteCode={}, error={}",
                    user_id, invite_code, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
     * 서버 탈퇴
     */
    public ResponseEntity<?> leave_server(String user_id, Long server_id){
        try{
            log.info("서버 탈퇴 시도: userId={}, serverId={}", user_id, server_id);

            user_repository.findById(user_id).orElseThrow(() -> new Exception("없는 유저ID 입니다."));

            Servers find_server = servers_repository.findById(server_id)
                    .orElseThrow(() -> new Exception("서버를 찾을 수 없습니다."));

            //서버 owner 탈퇴시 소유권 이전
            if(find_server.getOwnerId().equals(user_id)){
                List<Servers> owned_servers = servers_repository.findByOwnerId(user_id);
                for(Servers server : owned_servers) {
                    List<Server_members> other_members = server_member_repository
                            .findByServerIdAndUserIdNot(server.getServerId(), user_id);

                    if(!other_members.isEmpty()) {
                        Server_members new_owner = other_members.getFirst();
                        server.setOwnerId(new_owner.getUserId());
                        servers_repository.save(server);
                        log.info("서버 소유권 이전: serverId={}, 기존소유자={}, 새소유자={}",
                                server.getServerId(), user_id, new_owner.getUserId());
                    } else {
                        // 혼자만 있으면 서버 삭제
                        servers_repository.delete(server);
                        log.info("빈 서버 삭제: serverId={}, serverName={}",
                                server.getServerId(), server.getServerName());
                    }
                }
            }

            if(server_member_repository.findByUserIdAndServerId(user_id, server_id).isEmpty()) {
                log.warn("서버 탈퇴 실패 - 멤버가 아님: userId={}, serverId={}", user_id, server_id);
                throw new Exception("해당 서버의 멤버가 아닙니다.");
            }

            int message_count = chat_repository.findBySenderId(user_id).size();

            chat_repository.anonymizeMessagesBySenderId(user_id);
            log.info("사용자 메시지 익명화 완료: 총 {}개", message_count);

            List<Task_assignees> task_assignments = task_assignees_repository.findByUserId(user_id);
            task_assignees_repository.deleteAll(task_assignments);
            log.info("작업 할당 정리 완료: 총 {}개", task_assignments.size());

            Server_members server_member = server_member_repository.findByUserIdAndServerId(user_id, server_id).get();
            server_member_repository.delete(server_member);

            Long remaining_members = server_member_repository.countByServerId(server_id);
            log.info("서버 탈퇴 성공: userId={}, serverId={}, serverName={}, remainingMembers={}",
                    user_id, server_id, find_server.getServerName(), remaining_members);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "서버 탈퇴 성공."
            ));
        } catch (Exception e) {
            log.error("서버 탈퇴 중 오류 발생: userId={}, serverId={}, error={}",
                    user_id, server_id, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
     * 서버 강퇴
     */
    public ResponseEntity<?> getout_server(String owner_id, Long server_id, String user_id){
        try{
            log.info("서버 강퇴 시도: userId={}, serverId={}", user_id, server_id);

            user_repository.findById(user_id).orElseThrow(() -> new Exception("없는 유저ID 입니다."));

            Servers find_server = servers_repository.findById(server_id)
                    .orElseThrow(() -> new Exception("서버를 찾을 수 없습니다."));

            if(!find_server.getOwnerId().equals(owner_id)){
                throw new Exception("해당 서버의 Owner가 아닙니다.");
            }

            if(server_member_repository.findByUserIdAndServerId(user_id, server_id).isEmpty()) {
                log.warn("서버 강퇴 실패 - 멤버가 아님: userId={}, serverId={}", user_id, server_id);
                throw new Exception("해당 서버의 멤버가 아닙니다.");
            }

            Server_members server_member = server_member_repository.findByUserIdAndServerId(user_id, server_id).get();
            server_member_repository.delete(server_member);

            int message_count = chat_repository.findBySenderId(user_id).size();

            chat_repository.anonymizeMessagesBySenderId(user_id);
            log.info("사용자 메시지 익명화 완료: 총 {}개", message_count);

            List<Task_assignees> task_assignments = task_assignees_repository.findByUserId(user_id);
            task_assignees_repository.deleteAll(task_assignments);
            log.info("작업 할당 정리 완료: 총 {}개", task_assignments.size());

            Long remaining_members = server_member_repository.countByServerId(server_id);
            log.info("서버 강퇴 성공: userId={}, serverId={}, serverName={}, remainingMembers={}",
                    user_id, server_id, find_server.getServerName(), remaining_members);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "서버 탈퇴 성공."
            ));
        } catch (Exception e) {
            log.error("서버 강퇴 중 오류 발생: userId={}, serverId={}, error={}",
                    user_id, server_id, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
     * 삭제할 서버 데이터 호출
     */
    public Servers get_delete_server_data(Long server_id, String user_id){
        try{
            Servers server = servers_repository.findById(server_id)
                    .orElseThrow(() -> new IllegalArgumentException("서버를 찾을 수 없습니다."));

            if (!server.getOwnerId().equals(user_id)) {
                log.warn("서버 삭제 실패 - 권한 없음: serverId={}, userId={}, ownerId={}",
                        server_id, user_id, server.getOwnerId());
                throw new IllegalArgumentException("서버 소유자만 서버를 삭제할 수 있습니다.");
            }

            return server;
        } catch (Exception e) {
            log.error("서버 삭제 중 오류 발생: serverId={}, userId={}, error={}",
                    server_id, user_id, e.getMessage());
            throw new RuntimeException("서버 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /*
     * 초대 코드 호출
     */
    public ResponseEntity<?> get_invite_code(Long serverId){
        try{
            log.info("초대 코드 조회 시도: serverId={}", serverId);

            Servers server = servers_repository.findById(serverId)
                    .orElseThrow(() -> new Exception("해당 서버를 찾을 수 없습니다."));

            log.info("초대 코드 조회 성공: serverId={}", serverId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", server.getInviteCode()
            ));
        } catch (Exception e) {
            log.error("초대 코드 조회 중 오류 발생: serverId={}", serverId);
            throw new RuntimeException(e.getMessage());
        }
    }

    /*
     * 고유한 초대 코드 생성
     */
    private String generate_invite_code() {
        String server_invite_code;
        int attempt_count = 0;

        do{
            server_invite_code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            attempt_count++;

            if(attempt_count > 10) {
                log.warn("초대 코드 생성 시도 횟수 초과: attempts={}", attempt_count);
            }
        } while (servers_repository.findByInviteCode(server_invite_code).isPresent());

        log.debug("초대 코드 생성 완료: inviteCode={}, attempts={}", server_invite_code, attempt_count);
        return server_invite_code;
    }

}