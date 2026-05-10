package TIMOA.userserver.controller;


import TIMOA.userserver.DTO.login_request;
import TIMOA.userserver.entity.Users;
import TIMOA.userserver.service.User_service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class User_controller {

    @Autowired
    private User_service user_service;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Users user) {
        log.info("API 호출: POST /api/users/logout, userId={}", user.getUserId());

        try {
            ResponseEntity<?> response = user_service.logout(user);

            log.info("API 응답: POST /api/users/logout, userId={}, status={}",
                    user.getUserId(), response.getStatusCode());

            return response;
        } catch (Exception e) {
            log.error("API 오류: POST /api/users/logout, userId={}, error={}",
                    user.getUserId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody login_request user) {
        log.info("API 호출: POST /api/users/login, userId={}", user.getUserId());

        try {
            ResponseEntity<?> response = user_service.join(user);

            log.info("API 응답: POST /api/users/login, userId={}, status={}, result=SUCCESS",
                    user.getUserId(), response.getStatusCode());

            return response;
        } catch (Exception e) {
            log.error("API 오류: POST /api/users/login, userId={}, error={}",
                    user.getUserId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/create/user")
    public ResponseEntity<?> create(@RequestBody Users user){
        log.info("API 호출: POST /api/users/create/user, userId={}, userName={}",
                user.getUserId(), user.getUserName());

        try {
            ResponseEntity<?> response = user_service.create_user(user);
            log.info("API 응답: POST /api/users/create/user, userId={}, status={}",
                    user.getUserId(), response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("API 오류: POST /api/users/create/user, userId={}, error={}",
                    user.getUserId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/update/user")
    public ResponseEntity<?> update(@RequestParam("userId") String userId,
                                    @RequestParam("userName") String userName,
                                    @RequestParam("email") String email,
                                    @RequestParam("nickName") String nickName,
                                    @RequestParam(value = "profileImage", required = false) MultipartFile file){
        log.info("API 호출: POST /api/users/update/user, userId={}", userId);

        try {
            ResponseEntity<?> response = user_service.update_user(userId, userName, email, nickName, file);
            log.info("API 응답: POST /api/users/update/user, userId={}, status={}",
                    userId, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("API 오류: POST /api/users/update/user, userId={}, error={}",
                    userId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/delete/user")
    public ResponseEntity<?> delete(@RequestBody login_request user) {
        log.info("API 호출: DELETE /api/users/delete/user, userId={}", user.getUserId());

        try {
            ResponseEntity<?> response = user_service.delete_user(user);
            log.info("API 응답: DELETE /api/users/delete/user, userId={}, status={}",
                    user.getUserId(), response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("API 오류: DELETE /api/users/delete/user, userId={}, error={}",
                    user.getUserId(), e.getMessage());
            throw e;
        }
    }

//    @GetMapping
//    public ResponseEntity<?> find_all_user() {
//        log.info("API 호출: GET /api/users");
//
//        try {
//            ResponseEntity<?> response = user_service.find_all_user();
//            log.info("API 응답: GET /api/users, status={}",
//                    response != null ? response.getStatusCode() : "NULL");
//            return response;
//        } catch (Exception e) {
//            log.error("API 오류: GET /api/users, error={}", e.getMessage());
//            throw e;
//        }
//    }

    @PostMapping("/download-url")
    public ResponseEntity<?> download_url (@RequestParam(value = "file", required = false) MultipartFile file){
        try {
            ResponseEntity<?> response = user_service.file_download_url(file);
            log.info("API 응답: POST /api/users/download, userId={}, status={}",
                    file.getOriginalFilename(), response.getStatusCode());

            return response;
        } catch (Exception e) {
            log.error("API 오류: POST /api/users/download, userId={}, error={}",
                    file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }

}