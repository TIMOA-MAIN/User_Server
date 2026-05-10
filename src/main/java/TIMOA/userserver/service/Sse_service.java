package TIMOA.userserver.service;

import TIMOA.userserver.DTO.server_members_image_dto;
import TIMOA.userserver.entity.Server_members;
import TIMOA.userserver.entity.Users;
import TIMOA.userserver.repository.Server_member_repository;
import TIMOA.userserver.repository.User_repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class Sse_service {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Server_member_repository server_member_repository;

    @Autowired
    private User_repository user_repository;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userSubscribers = new ConcurrentHashMap<>();

    // 추가: 마지막 하트비트 시간 추적
    private final Map<String, Long> lastHeartbeats = new ConcurrentHashMap<>();

    // Keep-alive 및 하트비트 스케줄러
    private final ScheduledExecutorService keepAliveExecutor = Executors.newScheduledThreadPool(2);

    // 설정
    private static final long HEARTBEAT_INTERVAL = 30_000; // 30초마다 하트비트 전송
    private static final long HEARTBEAT_TIMEOUT = 90_000;  // 90초 동안 응답 없으면 오프라인
    private static final long TIMEOUT_CHECK_INTERVAL = 60_000; // 60초마다 타임아웃 체크

    public Sse_service() {
        //하트비트 전송 시작 30초
        keepAliveExecutor.scheduleAtFixedRate(
                this::sendHeartbeats,
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.MILLISECONDS
        );

        //타임아웃 체크 시작 60초
        keepAliveExecutor.scheduleAtFixedRate(
                this::checkHeartbeatTimeouts,
                TIMEOUT_CHECK_INTERVAL,
                TIMEOUT_CHECK_INTERVAL,
                TimeUnit.MILLISECONDS
        );
    }

    /*
     * 사용자별 SSE 연결 생성
     */
    public SseEmitter createUserConnection(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("userId가 null이거나 비어있습니다");
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        SseEmitter emitter = new SseEmitter(0L);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected for user " + userId));
        } catch (IOException e) {
            log.error("사용자 {}의 초기 SSE 메시지 전송 오류: {}", userId, e.getMessage());
            emitter.completeWithError(e);
            return emitter;
        }

        // 동기화 블록으로 원자성 보장
        synchronized (userSubscribers) {
            // 기존 연결들을 즉시 정리
            CopyOnWriteArrayList<SseEmitter> existing = userSubscribers.get(userId);
            if (existing != null && !existing.isEmpty()) {
                log.info("사용자 {}의 기존 연결 {}개를 정리합니다", userId, existing.size());

                // 기존 연결들을 완료시키고 즉시 제거
                List<SseEmitter> toRemove = new ArrayList<>(existing);
                for (SseEmitter old : toRemove) {
                    try {
                        old.complete();
                    } catch (Exception e) {
                        log.debug("기존 연결 정리 중 오류 (무시): {}", e.getMessage());
                    }
                }

                // 기존 리스트를 완전히 정리
                existing.clear();
                userSubscribers.remove(userId);
            }

            // 새로운 리스트 생성하고 새 연결 추가
            CopyOnWriteArrayList<SseEmitter> newList = new CopyOnWriteArrayList<>();
            newList.add(emitter);
            userSubscribers.put(userId, newList);


            // 마지막 하트비트 시간 초기화
            lastHeartbeats.put(userId, System.currentTimeMillis());

            log.info("사용자 {}의 새로운 SSE 연결 (전체 사용자 연결 수: {})",
                    userId, getTotalUserConnections());
        }

        setUserOnline(userId);

        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            removeUserEmitter(userId, emitter);
            log.info("사용자 {}의 SSE 연결이 완료되었습니다", userId);
            checkAndSetOffline(userId);
        });

        emitter.onTimeout(() -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 무시
            }
            removeUserEmitter(userId, emitter);
            log.info("사용자 {}의 SSE 연결이 타임아웃되었습니다", userId);
            checkAndSetOffline(userId);
        });

        emitter.onError((throwable) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 무시
            }
            removeUserEmitter(userId, emitter);

            // 정상적인 클라이언트 종료는 DEBUG 레벨로
            if (throwable instanceof IOException &&
                    (throwable.getMessage().contains("현재 연결은") ||
                            throwable.getMessage().contains("Connection reset") ||
                            throwable.getMessage().contains("Broken pipe") ||
                            throwable.getMessage().contains("호스트 시스템의 소프트웨어"))) {
                log.debug("사용자 {}의 SSE 연결이 클라이언트에 의해 종료됨: {}", userId, throwable.getMessage());
            } else {
                log.error("사용자 {}의 SSE 연결 오류: {}", userId, throwable.getMessage());
            }

            checkAndSetOffline(userId);
        });

        return emitter;
    }

    /*
     * 서버의 모든 멤버에게 이벤트 브로드캐스트
     */
    public void broadcastToServerMembers(Long serverId, String eventType, Object data) {
        try {
            // 서버 멤버 목록 조회
            List<String> serverMembers = server_member_repository.findByServerId(serverId)
                    .stream()
                    .map(Server_members::getUserId)
                    .toList();

            log.info("서버 {}의 멤버 목록: {}", serverId, serverMembers.toString());

            if (serverMembers.isEmpty()) {
                log.warn("서버 {}에 활성화된 멤버가 없습니다", serverId);
                return;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("type", eventType);
            message.put("serverId", serverId);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);

            // 각 서버 멤버에게 개별 전송
            int successCount = 0;
            int failCount = 0;
            for (String userId : serverMembers) {
                CopyOnWriteArrayList<SseEmitter> userEmitters = userSubscribers.get(userId);
                if (userEmitters != null && !userEmitters.isEmpty()) {
                    log.debug("서버 {}의 사용자 {}에게 {}개 연결로 브로드캐스트 중",
                            serverId, userId, userEmitters.size());

                    try {
                        sendToUserEmitters(userEmitters, eventType, jsonMessage, userId);
                        successCount++;
                    } catch (Exception userEx) {
                        failCount++;
                        log.warn("사용자 {}에게 브로드캐스트 실패: {}", userId, userEx.getMessage());
                    }
                } else {
                    log.debug("서버 {}의 사용자 {}에게 활성화된 SSE 연결이 없습니다", serverId, userId);
                    failCount++;
                }
            }

            log.info("서버 브로드캐스트 완료: {} -> 서버 {}의 {}명 사용자 성공, {}명 실패",
                    eventType, serverId, successCount, failCount);

        } catch (Exception e) {
            log.error("서버 {}에 브로드캐스트 오류: {} - {}",
                    serverId, e.getClass().getSimpleName(),
                    e.getMessage() != null ? e.getMessage() : "메시지 없음", e);
        }
    }

    /*
     * 서버의 모든 멤버에게 이벤트 브로드캐스트
     * 서버 삭제시에만 실행
     */
    public void broadcastToServerDelete(Long serverId, String eventType, Object data, List<server_members_image_dto> server_members){
        try {
            // 서버 멤버 목록 조회
            List<String> members = server_members.stream()
                    .map(server_members_image_dto::getUserId)
                    .toList();

            log.info("서버 {}의 멤버 목록: {}", serverId, members.toString());

            if (members.isEmpty()) {
                log.warn("서버 {}에 활성화된 멤버가 없습니다", serverId);
                return;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("type", eventType);
            message.put("serverId", serverId);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);

            // 각 서버 멤버에게 개별 전송
            int successCount = 0;
            int failCount = 0;
            for (String userId : members) {
                CopyOnWriteArrayList<SseEmitter> userEmitters = userSubscribers.get(userId);
                if (userEmitters != null && !userEmitters.isEmpty()) {
                    log.debug("서버 {}의 사용자 {}에게 {}개 연결로 브로드캐스트 중",
                            serverId, userId, userEmitters.size());

                    try {
                        sendToUserEmitters(userEmitters, eventType, jsonMessage, userId);
                        successCount++;
                    } catch (Exception userEx) {
                        failCount++;
                        log.warn("사용자 {}에게 브로드캐스트 실패: {}", userId, userEx.getMessage());
                    }
                } else {
                    log.debug("서버 {}의 사용자 {}에게 활성화된 SSE 연결이 없습니다", serverId, userId);
                    failCount++;
                }
            }

            log.info("서버 브로드캐스트 완료: {} -> 서버 {}의 {}명 사용자 성공, {}명 실패",
                    eventType, serverId, successCount, failCount);

        } catch (Exception e) {
            log.error("서버 {}에 브로드캐스트 오류: {} - {}",
                    serverId, e.getClass().getSimpleName(),
                    e.getMessage() != null ? e.getMessage() : "메시지 없음", e);
        }
    }

    // Keep-alive 시스템
    private void sendKeepAlive() {
        try {
            if (userSubscribers.isEmpty()) {
                log.info("활성화된 사용자 연결이 없습니다");
                return;
            }

            log.info("Keep-alive 시작: 총 {}명의 사용자 (60초 간격)", userSubscribers.size());

            for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userSubscribers.entrySet()) {
                if (entry == null) {
                    log.warn("userSubscribers entry가 null입니다");
                    continue;
                }

                String userId = entry.getKey();
                CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();

                if (userId == null) {
                    log.warn("userId가 null인 entry가 있습니다");
                    continue;
                }

                if (emitters == null) {
                    log.warn("사용자 {}의 emitters가 null입니다", userId);
                    continue;
                }

                try {
                    sendKeepAliveToUserEmitters(emitters, userId);
                } catch (Exception userException) {
                    log.error("사용자 {}의 Keep-alive 처리 중 오류: {} - {}",
                            userId,
                            userException.getClass().getSimpleName(),
                            userException.getMessage() != null ? userException.getMessage() : "메시지 없음",
                            userException);
                }
            }

            int totalConnections = getTotalUserConnections();
            if (totalConnections > 0) {
                log.info("{}개 사용자 연결에 Keep-alive 신호 전송 완료 (60초 간격)", totalConnections);
            }

        } catch (Exception e) {
            log.error("Keep-alive 전체 처리 중 오류: {} - {}",
                    e.getClass() != null ? e.getClass().getSimpleName() : "Unknown",
                    e.getMessage() != null ? e.getMessage() : "메시지 없음", e);
        }
    }

    private void sendKeepAliveToUserEmitters(CopyOnWriteArrayList<SseEmitter> emitters, String userId) {
        if (emitters == null) {
            log.warn("사용자 {}의 emitters가 null입니다", userId);
            return;
        }

        if (emitters.isEmpty()) {
            log.debug("사용자 {}의 emitters가 비어있습니다", userId);
            return;
        }

        log.debug("사용자 {}의 Keep-alive 시작: {}개 연결", userId, emitters.size());

        int activeCount = 0;
        int removedCount = 0;

        // 제거할 emitter들을 별도로 수집
        List<SseEmitter> toRemove = new ArrayList<>();

        // 스냅샷 기반 반복 (Iterator 사용하지 않음)
        for (SseEmitter emitter : emitters) {
            if (emitter == null) {
                log.warn("사용자 {}에서 null emitter 발견", userId);
                toRemove.add(emitter);
                continue;
            }

            try {
                // emitter 상태 확인 후 전송
                try {
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .data("keep-alive"));
                    activeCount++;
                } catch (IllegalStateException stateEx) {
                    // 이미 완료된 emitter는 제거 대상에 추가
                    log.debug("사용자 {}의 완료된 emitter 제거 예정: {}", userId, stateEx.getMessage());
                    toRemove.add(emitter);
                    continue;
                }

            } catch (Exception e) {
                toRemove.add(emitter);

                if (e instanceof IOException && e.getMessage() != null && (
                        e.getMessage().contains("Connection reset") ||
                                e.getMessage().contains("Broken pipe") ||
                                e.getMessage().contains("현재 연결은"))) {
                    log.debug("사용자 {}의 Keep-alive 연결이 클라이언트에 의해 종료됨: {}", userId, e.getMessage());
                } else {
                    log.warn("사용자 {}의 Keep-alive 중 예상치 못한 오류: {} - {}",
                            userId,
                            e.getClass() != null ? e.getClass().getSimpleName() : "Unknown",
                            e.getMessage() != null ? e.getMessage() : "메시지 없음");
                }
            }
        }

        // 제거할 emitter들을 일괄 제거
        if (!toRemove.isEmpty()) {
            try {
                emitters.removeAll(toRemove);
                removedCount = toRemove.size();
                log.debug("사용자 {}의 Keep-alive 완료: 활성={}개, 제거={}개", userId, activeCount, removedCount);
            } catch (Exception removeException) {
                log.debug("사용자 {}의 emitter 일괄 제거 중 예외: {}", userId,
                        removeException.getMessage() != null ? removeException.getMessage() : "메시지 없음");
            }
        }
    }

    private void sendToUserEmitters(CopyOnWriteArrayList<SseEmitter> emitters, String eventType, String data, String userId) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        // 제거할 emitter들을 별도로 수집
        List<SseEmitter> toRemove = new ArrayList<>();

        // 스냅샷 기반 반복 (Iterator 사용하지 않음)
        for (SseEmitter emitter : emitters) {
            if (emitter == null) {
                toRemove.add(emitter);
                failCount.incrementAndGet();
                continue;
            }

            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(data));
                successCount.incrementAndGet();
            } catch (IOException ioEx) {
                failCount.incrementAndGet();
                toRemove.add(emitter);

                if (ioEx.getMessage() != null && (
                        ioEx.getMessage().contains("Connection reset") ||
                                ioEx.getMessage().contains("Broken pipe") ||
                                ioEx.getMessage().contains("현재 연결은") ||
                                ioEx.getMessage().contains("호스트 시스템의 소프트웨어"))) {
                    log.debug("사용자 {}의 SSE 연결이 클라이언트에 의해 종료됨", userId);
                } else {
                    log.debug("사용자 {}의 SSE 연결 중단: {}", userId, ioEx.getMessage());
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
                toRemove.add(emitter);
                log.warn("사용자 {}에게 SSE 메시지 전송 중 예상치 못한 오류: {} - {}",
                        userId,
                        e.getClass() != null ? e.getClass().getSimpleName() : "Unknown",
                        e.getMessage() != null ? e.getMessage() : "메시지 없음");
            }
        }

        // 제거할 emitter들을 일괄 제거
        if (!toRemove.isEmpty()) {
            try {
                emitters.removeAll(toRemove);
            } catch (Exception removeException) {
                log.debug("사용자 {}의 emitter 일괄 제거 중 예외: {}", userId,
                        removeException.getMessage() != null ? removeException.getMessage() : "메시지 없음");
            }
        }

        if (successCount.get() > 0 || failCount.get() > 0) {
            log.debug("📤 사용자 {}에게 전송 (성공: {}개, 실패: {}개)", userId, successCount.get(), failCount.get());
        }
    }

    private void removeUserEmitter(String userId, SseEmitter emitter) {
        if (userId == null) return;

        synchronized (userSubscribers) {
            CopyOnWriteArrayList<SseEmitter> userEmitters = userSubscribers.get(userId);
            if (userEmitters != null) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) {
                    userSubscribers.remove(userId);
                    log.debug("사용자 {}의 모든 연결이 제거되었습니다", userId);
                }
            }
        }
    }

    private int getTotalUserConnections() {
        try {
            return userSubscribers.values().stream()
                    .filter(list -> list != null)
                    .mapToInt(List::size)
                    .sum();
        } catch (Exception e) {
            log.error("전체 사용자 연결 수 계산 중 오류: {} - {}",
                    e.getClass() != null ? e.getClass().getSimpleName() : "Unknown",
                    e.getMessage() != null ? e.getMessage() : "메시지 없음", e);
            return 0;
        }
    }

    /*
     * 연결 상태 조회
     */
    public Map<String, Object> getActiveSubscriptions() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalUsers", userSubscribers.size());
        status.put("totalConnections", getTotalUserConnections());

        Map<String, Integer> userConnections = new HashMap<>();
        userSubscribers.forEach((userId, emitters) ->
                userConnections.put(userId, emitters.size()));
        status.put("userConnections", userConnections);

        return status;
    }

    /*
     * 모든 연결된 사용자에게 하트비트 전송 (30초마다)
     */
    private void sendHeartbeats() {
        try {
            if (userSubscribers.isEmpty()) {
                return;
            }

            int totalSent = 0;
            int totalFailed = 0;

            // 모든 사용자에게 하트비트 전송
            for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userSubscribers.entrySet()) {
                String userId = entry.getKey();
                CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();

                if (emitters == null || emitters.isEmpty()) {
                    continue;
                }

                List<SseEmitter> toRemove = new ArrayList<>();

                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("heartbeat")
                                .data("ping"));

                        lastHeartbeats.put(userId, System.currentTimeMillis());
                        totalSent++;

                    } catch (IOException ioEx) {
                        // ✅ 정상적인 클라이언트 종료는 DEBUG 레벨로
                        if (ioEx.getMessage() != null && (
                                ioEx.getMessage().contains("Connection reset") ||
                                        ioEx.getMessage().contains("Broken pipe") ||
                                        ioEx.getMessage().contains("현재 연결은") ||
                                        ioEx.getMessage().contains("호스트 시스템의 소프트웨어"))) {
                            log.debug("사용자 {}의 하트비트 전송 실패 (클라이언트 종료): {}", userId, ioEx.getMessage());

                            try {
                                emitter.complete();
                            } catch (Exception e) {
                                // 무시
                            }

                        } else {
                            // 예상치 못한 오류만 WARN 레벨로
                            log.warn("사용자 {}의 하트비트 전송 실패: {}", userId, ioEx.getMessage());
                        }
                        toRemove.add(emitter);
                        totalFailed++;

                    } catch (Exception e) {
                        log.warn("사용자 {}의 하트비트 전송 중 예상치 못한 오류: {}", userId, e.getMessage());
                        toRemove.add(emitter);
                        totalFailed++;
                    }
                }

                // 실패한 emitter 제거
                if (!toRemove.isEmpty()) {
                    emitters.removeAll(toRemove);
                    if (emitters.isEmpty()) {
                        userSubscribers.remove(userId);
                        lastHeartbeats.remove(userId);
                        log.info("사용자 {}의 모든 연결이 제거됨", userId);
                        // 모든 연결 제거 시 오프라인 처리
                        setUserOffline(userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("하트비트 전송 중 오류: {}", e.getMessage(), e);
        }
    }

    /*
     * 하트비트 타임아웃 체크 (60초마다)
     * 90초 동안 하트비트가 없으면 자동 오프라인 처리
     */
    private void checkHeartbeatTimeouts() {
        try {
            long now = System.currentTimeMillis();
            List<String> timeoutUsers = new ArrayList<>();

            // 타임아웃된 사용자 찾기
            for (Map.Entry<String, Long> entry : lastHeartbeats.entrySet()) {
                String userId = entry.getKey();
                Long lastHeartbeat = entry.getValue();

                if (now - lastHeartbeat > HEARTBEAT_TIMEOUT) {
                    log.warn("사용자 {} 하트비트 타임아웃 ({}초 경과)",
                            userId, (now - lastHeartbeat) / 1000);
                    timeoutUsers.add(userId);
                }
            }

            // 타임아웃된 사용자들 일괄 오프라인 처리
            if (!timeoutUsers.isEmpty()) {
                log.info("{}명의 사용자를 타임아웃으로 일괄 오프라인 처리", timeoutUsers.size());

                // 배치로 DB 업데이트 (성능 최적화!)
                setUsersOfflineBatch(timeoutUsers);

                // 연결 정리
                for (String userId : timeoutUsers) {
                    userSubscribers.remove(userId);
                    lastHeartbeats.remove(userId);
                }
            }

        } catch (Exception e) {
            log.error("하트비트 타임아웃 체크 중 오류: {}", e.getMessage(), e);
        }
    }

    /*
     * 사용자를 온라인 상태로 설정
     */
    private void setUserOnline(String userId) {
        try {
            Optional<Users> userOptional = user_repository.findById(userId);

            if (userOptional.isPresent()) {
                Users user = userOptional.get();

                // 이미 온라인이면 스킵 (성능 최적화)
                if (Users.user_status.ONLINE.equals(user.getStatus())) {
                    log.debug("사용자 {}는 이미 온라인 상태", userId);
                    return;
                }

                user.setStatus(Users.user_status.ONLINE);
                user_repository.save(user);

                log.info("사용자 {} 온라인 설정 완료", userId);
            } else {
                log.warn("사용자 {}를 찾을 수 없음", userId);
            }
        } catch (Exception e) {
            log.error("사용자 {} 온라인 설정 실패: {}", userId, e.getMessage(), e);
        }
    }

    /*
     * 사용자를 오프라인 상태로 설정
     */
    private void setUserOffline(String userId) {
        try {
            Optional<Users> userOptional = user_repository.findById(userId);

            if (userOptional.isPresent()) {
                Users user = userOptional.get();

                // 이미 오프라인이면 스킵 (성능 최적화)
                if (Users.user_status.OFFLINE.equals(user.getStatus())) {
                    log.debug("사용자 {}는 이미 오프라인 상태", userId);
                    return;
                }

                user.setStatus(Users.user_status.OFFLINE);
                user_repository.save(user);

                log.info("사용자 {} 오프라인 설정 완료", userId);
            } else {
                log.warn("사용자 {}를 찾을 수 없음", userId);
            }
        } catch (Exception e) {
            log.error("사용자 {} 오프라인 설정 실패: {}", userId, e.getMessage(), e);
        }
    }

    /*
     * 여러 사용자를 일괄 오프라인 처리 (배치 성능 최적화)
     */
    private void setUsersOfflineBatch(List<String> userIds) {
        try {
            if (userIds.isEmpty()) {
                return;
            }

            log.info("{}명 사용자 배치 오프라인 처리 시작", userIds.size());

            // 단일 쿼리로 배치 업데이트
            List<Users> users = user_repository.findByUserIdIn(userIds);

            for (Users user : users) {
                if (Users.user_status.OFFLINE.equals(user.getStatus())) {
                    user.setStatus(Users.user_status.OFFLINE);
                }
            }

            user_repository.saveAll(users);

            log.info("{}명 사용자 배치 오프라인 처리 완료", users.size());

        } catch (Exception e) {
            log.error("배치 오프라인 처리 실패: {}", e.getMessage(), e);
        }
    }

    /*
     * 사용자에게 남은 연결이 있는지 확인 후 오프라인 처리
     */
    private void checkAndSetOffline(String userId) {
        CopyOnWriteArrayList<SseEmitter> emitters = userSubscribers.get(userId);

        // 남은 연결이 없으면 오프라인 처리
        if (emitters == null || emitters.isEmpty()) {
            lastHeartbeats.remove(userId);
            setUserOffline(userId);
        }
    }


    public void shutdown() {
        if (!keepAliveExecutor.isShutdown()) {
            keepAliveExecutor.shutdown();
            try {
                if (!keepAliveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    keepAliveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                keepAliveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("SSE 서비스 종료 완료");
    }
}