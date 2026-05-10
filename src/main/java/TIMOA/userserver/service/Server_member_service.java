package TIMOA.userserver.service;

import TIMOA.userserver.DTO.server_members_image_dto;
import TIMOA.userserver.DTO.users_dto;
import TIMOA.userserver.entity.Server_members;
import TIMOA.userserver.entity.Users;
import TIMOA.userserver.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class Server_member_service {

    @Autowired
    private Server_member_repository server_member_repository;

    @Autowired
    private Servers_repository servers_repository;

    @Autowired
    private User_repository user_repository;

    @Autowired
    private File_management_repository fmr;

    @Autowired
    private S3FileRepository S3;

    /*
     * 서버의 멤버 조회
     */
    public List<server_members_image_dto> get_server_member(Long server_id){
        try{
            log.info("서버 멤버 목록 조회: serverId={}", server_id);

            List<String> user_ids = server_member_repository.findByServerId(server_id).stream()
                    .map(Server_members::getUserId).toList();

            log.debug("서버 활성 멤버 ID 목록: serverId={}, userIds={}", server_id, user_ids);

            List<Users> find_users = user_repository.findByUserIdIn(user_ids);

            List<server_members_image_dto> members_image_dtos = new ArrayList<>();
            for(Users u : find_users){
                String image_file = S3.downloadFile(u.getProfileImageUrl());
                server_members_image_dto dto = server_members_image_dto.fromEntity(u, image_file);
                members_image_dtos.add(dto);
            }

            log.info("서버 멤버 목록 조회 성공: serverId={}, activeMemberCount={}, foundUserCount={}",
                    server_id, user_ids.size(), find_users.size());

            // 데이터 불일치 체크
            if(user_ids.size() != find_users.size()) {
                log.warn("서버 멤버 데이터 불일치 감지: serverId={}, memberRecordCount={}, userRecordCount={}",
                        server_id, user_ids.size(), find_users.size());
            }

            return members_image_dtos;
        } catch (Exception e) {
            log.error("서버 멤버 목록 조회 중 오류 발생: serverId={}, error={}", server_id, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}