package TIMOA.userserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("CORS 설정 적용 시작...");

        registry.addMapping("/**")  // 모든 경로에 대해
                .allowedOriginPatterns("*")  // 모든 도메인 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")  // 모든 HTTP 메서드 허용
                .allowedHeaders("*")  // 모든 헤더 허용
                .allowCredentials(false)  // credentials false (보안상 권장)
                .maxAge(3600);  // preflight 캐시 시간 (1시간)

        log.info("CORS 설정 완료: 모든 Origin 허용 | 허용 메서드: GET,POST,PUT,DELETE,OPTIONS,PATCH | Preflight 캐시: 1시간");
        log.debug("CORS 상세 설정 - 경로: /** | 헤더: 모든 헤더 허용 | Credentials: false");
    }
}
