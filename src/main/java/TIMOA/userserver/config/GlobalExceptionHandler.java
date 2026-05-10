package TIMOA.userserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ResponseEntity<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
//        log.error("파일 크기 제한 초과: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
//                .body("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.");
//    }

}
