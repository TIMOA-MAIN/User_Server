package TIMOA.userserver.service;

import TIMOA.userserver.DTO.login_request;
import TIMOA.userserver.DTO.users_dto;
import TIMOA.userserver.entity.Dashboard.Task_assignees;
import TIMOA.userserver.entity.Server_members;
import TIMOA.userserver.entity.Servers;
import TIMOA.userserver.entity.Users;
import TIMOA.userserver.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class User_service {

    private final String base_profile_image_url = "base_profile_image.jpg";

    @Autowired
    private User_repository user_repository;

    @Autowired
    private File_management_repository fmr;

    @Autowired
    private Servers_repository servers_repository;

    @Autowired
    private Server_member_repository server_member_repository;

    @Autowired
    private Chat_repository chat_repository;

    @Autowired
    private Dashboard_task_assignees_repository task_assignees_repository;

    @Autowired
    private S3FileRepository S3;

    @Value("${jwt.secret}")
    private String jwt_secret;// 충분히 긴 키
    Key key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(jwt_secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 키 초기화 완료");
    }

    /*
    회원 추가
    */
    public ResponseEntity<?> create_user(Users user){
        try{
            log.info("회원가입 시도: userId={}", user.getUserId());

            // 중복 사용자 체크
            if(user_repository.findById(user.getUserId()).isPresent()) {
                log.warn("회원가입 실패 - 중복 사용자: userId={}", user.getUserId());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미 존재하는 사용자 ID 입니다.",
                        "type", "id"
                ));
            }

            if(user_repository.findByEmail(user.getEmail()).isPresent()){
                log.warn("회원가입 실패 - 중복 이메일: userId={}", user.getUserId());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미 존재하는 사용자 Email 입니다.",
                        "type", "email"
                ));
            }
            user.setProfileImageUrl(base_profile_image_url);

            user.setNickName(user.getUserName());

            Users find_user = user_repository.save(user);
            log.info("회원가입 성공: userId={}, userName={}", find_user.getUserId(), find_user.getUserName());

            String image_file = S3.downloadFile(find_user.getProfileImageUrl());

            if(image_file==null || image_file.isEmpty())
                throw new Exception("프로필 파일을 찾을 수 없습니다.");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", users_dto.fromEntity(find_user),
                    "profile_image", image_file
            ));
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생: userId={}, error={}", user.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
    회원 삭제
     */
    public ResponseEntity<?> delete_user(login_request user){
        try{
            log.info("회원탈퇴 시도: userId={}", user.getUserId());

            Users find_user = user_repository.findById(user.getUserId())
                    .orElseThrow(() -> new Exception("없는 유저ID 입니다."));

            if(user_repository.findByUserIdAndPassword(find_user.getUserId(), find_user.getPassword()).isEmpty()) {
                log.warn("회원탈퇴 실패 - 비밀번호 불일치: userId={}", user.getUserId());
                throw new Exception("비밀번호가 일치하지 않습니다.");
            }

            List<Servers> owned_servers = servers_repository.findByOwnerId(find_user.getUserId());
            for(Servers server : owned_servers) {
                List<Server_members> other_members = server_member_repository
                        .findByServerIdAndUserIdNot(server.getServerId(), find_user.getUserId());

                if(!other_members.isEmpty()) {
                    Server_members new_owner = other_members.getFirst();
                    server.setOwnerId(new_owner.getUserId());
                    servers_repository.save(server);
                    log.info("서버 소유권 이전: serverId={}, 기존소유자={}, 새소유자={}",
                            server.getServerId(), find_user.getUserId(), new_owner.getUserId());
                } else {
                    // 혼자만 있으면 서버 삭제
                    servers_repository.delete(server);
                    log.info("빈 서버 삭제: serverId={}, serverName={}",
                            server.getServerId(), server.getServerName());
                }
            }

            int message_count = chat_repository.findBySenderId(find_user.getUserId()).size();

            chat_repository.anonymizeMessagesBySenderId(find_user.getUserId());
            log.info("사용자 메시지 익명화 완료: 총 {}개", message_count);

            List<Task_assignees> task_assignments = task_assignees_repository.findByUserId(find_user.getUserId());
            task_assignees_repository.deleteAll(task_assignments);
            log.info("작업 할당 정리 완료: 총 {}개", task_assignments.size());

            List<Server_members> memberships = server_member_repository.findByUserId(find_user.getUserId());
            server_member_repository.deleteAll(memberships);
            log.info("서버 멤버십 정리 완료: 총 {}개", memberships.size());

            if(!find_user.getProfileImageUrl().equals(base_profile_image_url)){
                S3.deleteFile(find_user.getProfileImageUrl());
                log.info("유저 이미지 파일 삭제 완료");
            }

            user_repository.deleteById(find_user.getUserId());
            log.info("회원탈퇴 성공: userId={}", find_user.getUserId());

            return ResponseEntity.ok(Map.of(
                    "success", true
            ));
        } catch (Exception e) {
            log.error("회원탈퇴 처리 중 오류 발생: userId={}, error={}", user.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
    회원 정보 수정
     */
    public ResponseEntity<?> update_user(String user_id, String user_name,
                                         String email, String nick_name, MultipartFile image_file){
        try {
            log.info("회원정보 수정 시도: userId={}, {}", user_id, image_file.getOriginalFilename());

            Users find_user = user_repository.findById(user_id)
                    .orElseThrow(() -> new IllegalArgumentException("없는 유저ID 입니다."));

            // 디버깅 로그 추가
            log.debug("현재 사용자 이메일: [{}]", find_user.getEmail());
            log.debug("입력받은 이메일: [{}]", email);

            if (email != null && !email.trim().isEmpty()) {
                String trimmedEmail = email.trim();

                // 현재 이메일과 다른 경우에만 중복 체크
                if (!trimmedEmail.equals(find_user.getEmail())) {
                    log.debug("이메일 변경 감지, 중복 체크 시작");
                    if (user_repository.existsByEmailAndUserIdNot(trimmedEmail, user_id)) {
                        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                    }
                    find_user.setEmail(trimmedEmail);
                } else {
                    log.debug("이메일 변경 없음, 중복 체크 스킵");
                }
            }
            log.info(image_file.getOriginalFilename());

            find_user.setUserName(user_name);
            find_user.setNickName(nick_name);

            String uniqueFileName = null;


            /*
             * 새로 들어온 파일의 이름이 db의 이미지와 같지 않으면 실행
             */
            if(!Objects.requireNonNull(image_file.getOriginalFilename()).equals(find_user.getProfileImageUrl())){
                /*
                 * db에 저장된 이미지의 이름이 기본 파일이면
                 * 기존 파일 삭제 없이 s3 파일 업로드
                 * 이후 해당 파일의 uniqueFileName을 db에 저장
                 * ---------------------------------------
                 * 기본 파일이 아니라면
                 * 기존 파일 삭제 후 새 이미지 업로드
                 * 이후 해당 파일의 uniqueFileName을 db에 저장
                 */
                if(find_user.getProfileImageUrl().equals(base_profile_image_url)){
                    uniqueFileName = S3.uploadFile(image_file);
                    find_user.setProfileImageUrl(uniqueFileName);
                }
                else{
                    S3.deleteFile(find_user.getProfileImageUrl());
                    uniqueFileName = S3.uploadFile(image_file);
                    find_user.setProfileImageUrl(uniqueFileName);
                }
            }

            find_user.setProfileImageUrl(uniqueFileName);

            Users save_user = user_repository.save(find_user);
            log.info("회원정보 수정 성공: userId={}", find_user.getUserId());

            String new_image_file = S3.downloadFile(uniqueFileName);

            if(new_image_file==null || new_image_file.isEmpty())
                throw new Exception("프로필 파일을 찾을 수 없습니다.");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", users_dto.fromEntity(save_user),
                    "profile_image", new_image_file
            ));
        } catch (Exception e) {
            log.error("회원정보 수정 중 오류 발생: userId={}, error={}", user_id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
    회원 전체 검색
     */
    public ResponseEntity<?> find_all_user(){
        try{
            log.info("전체 회원 조회 요청");

            List<users_dto> users = new ArrayList<>();
            List<Users> result = user_repository.findAll();

            log.info("전체 회원 조회 성공: 총 {}명", result.size());

            for(Users u : result){
                users.add(users_dto.fromEntity(u));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "users", users
            ));
        } catch (Exception e) {
            log.error("전체 회원 조회 중 오류 발생: error={}", e.getMessage());
            return null;
        }
    }

    /*
    회원 로그인
     */
    public ResponseEntity<?> join(login_request user) {
        try{
            String id = user.getUserId();
            log.info("로그인 시도: userId={}", id);

            Optional<Users> login_user_data = user_repository.findByUserIdAndPassword(id, user.getPassword());

            if (login_user_data.isPresent()) {
                String token = Jwts.builder()
                        .setSubject(id)
                        .signWith(key, SignatureAlgorithm.HS256)
                        .setExpiration(new Date(System.currentTimeMillis()+60*60*1000*24))    //24시간 마다 변경
                        .compact();

                update_login_state(login_user_data.get().getUserId(), true);
                log.info("로그인 성공: userId={}", id);
                log.info(login_user_data.get().getProfileImageUrl());
                String image_file = S3.downloadFile(login_user_data.get().getProfileImageUrl());

                log.info("프로필 이미지 호출 성공");

                if(image_file==null || image_file.isEmpty())
                    throw new Exception("프로필 파일을 찾을 수 없습니다.");

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "token", token,
                        "user", users_dto.fromEntity(login_user_data.get()),
                        "profile_image", image_file
                ));
            } else {
                log.warn("로그인 실패 - 인증 오류: userId={}", id);
                throw new Exception("아이디 또는 패스워드가 일치하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생: userId={}, error={}", user.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
    회원 로그아웃
     */
    public ResponseEntity<?> logout(Users user){
        try{
            log.info("로그아웃 시도: userId={}", user.getUserId());

            Optional<Users> find_user = user_repository.findById(user.getUserId());

            if(find_user.isPresent()){
                if(find_user.get().getStatus()== Users.user_status.OFFLINE) {
                    log.warn("로그아웃 실패 - 이미 오프라인 상태: userId={}", user.getUserId());
                    throw new Exception("로그 아웃 실패 : 이미 로그 아웃 유저");
                }

                update_login_state(find_user.get().getUserId(), false);
            }

            log.info("로그아웃 성공: userId={}", user.getUserId());
            return ResponseEntity.ok(Map.of(
                    "success", true
            ));
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생: userId={}, error={}", user.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /*
    회원 로그인 상태 변경
     */
    private void update_login_state(String user_id, boolean is_login) {
        try {
            Users.user_status state = is_login ? Users.user_status.ONLINE : Users.user_status.OFFLINE;
            Optional<Users> find_user = user_repository.findById(user_id);

            if (find_user.isPresent()) {
                Users user = find_user.get();
                user.setStatus(state);
                user_repository.save(user);
                log.debug("사용자 상태 변경: userId={}, status={}", user_id, state);
            } else {
                log.warn("상태 변경 실패 - 존재하지 않는 사용자: userId={}", user_id);
            }
        } catch (Exception e) {
            log.error("사용자 상태 변경 중 오류 발생: userId={}, error={}", user_id, e.getMessage());
        }
    }

//    @EventListener(ApplicationReadyEvent.class)
//    public void createSystemUsers() {
//        if (!user_repository.existsById("[삭제된 사용자]")) {
//            Users deletedUser = new Users();
//            deletedUser.setUserId("[삭제된 사용자]");
//            deletedUser.setUserName("삭제된 사용자");
//            deletedUser.setPassword("SYSTEM_USER"); // 실제 로그인 불가
//            deletedUser.setEmail("system@deleted.user");
//            deletedUser.setProfileImageUrl(base_profile_image_url);
//
//            user_repository.save(deletedUser);
//            log.info("시스템 사용자 '[삭제된 사용자]' 생성 완료");
//        }
//    }

    public ResponseEntity<?>    file_download_url(MultipartFile file) {
        try {
            log.info("다운로드 URL 생성 시도: fileName={}", file.getOriginalFilename());

            String downloadUrl = S3.file_download_url(file);

            log.info("다운로드 URL 생성 성공: fileName={}", file.getOriginalFilename());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "downloadUrl", downloadUrl
            ));
        } catch (Exception e) {
            log.error("다운로드 URL 생성 중 오류 발생: fileName={}, error={}",
                    file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
