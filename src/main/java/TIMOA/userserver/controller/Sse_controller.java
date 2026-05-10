package TIMOA.userserver.controller;

import TIMOA.userserver.service.Sse_service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/sse")
public class Sse_controller {

    @Autowired
    private Sse_service sseService;

    /*
     * 사용자별 SSE 연결
     */
    @GetMapping(value = "/user/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectUser(@PathVariable String userId) {
        log.info("SSE connection request for user: {}", userId);
        return sseService.createUserConnection(userId);
    }

    /*
     * SSE 연결 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        Map<String, Object> status = sseService.getActiveSubscriptions();
        return ResponseEntity.ok(status);
    }
}