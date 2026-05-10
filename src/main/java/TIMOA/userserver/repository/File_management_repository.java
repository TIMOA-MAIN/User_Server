package TIMOA.userserver.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@Repository
public class File_management_repository {
    private final String PROFILE_IMAGE_DIR = "user_profile_image/";
    private final String base_profile_image_url = "base_profile_image.jpg";

    /*
경로에 있는 이미지 데이터 불러오기
 */
    public String encodeImageToBase64(String imagePath) {
        try {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                log.warn("이미지 경로가 null 또는 빈 문자열입니다.");
                return "";
            }

            Path path = Paths.get(PROFILE_IMAGE_DIR+imagePath);

            if (!Files.exists(path)) {
                log.warn("이미지 파일이 존재하지 않습니다: {}", imagePath);
                return "";
            }

            if (!Files.isReadable(path)) {
                log.warn("이미지 파일을 읽을 수 없습니다: {}", imagePath);
                return "";
            }

            byte[] imageBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (IOException e) {
            log.error("이미지 파일 읽기 실패: path={}, error={}", imagePath, e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("이미지 인코딩 중 예상치 못한 오류: path={}, error={}", imagePath, e.getMessage());
            return "";
        }
    }


    /*
    이전 이미지 파일 삭제 및
    새로운 이미지 파일 생성
    새로운 url 반환
     */
    public String get_new_profile_image_url(String before_url, MultipartFile new_image_file){
        try{
            // 1. 파일이 비어있는지 확인
            if (new_image_file == null || new_image_file.isEmpty()) {
                log.info("업로드된 파일이 null이거나 비어있음으로 기존 url를 유지합니다.");
                return before_url;
            }

            // 2. 파일 크기 확인
            log.info("업로드된 파일 정보 - 이름: {}, 크기: {} bytes, 타입: {}",
                    new_image_file.getOriginalFilename(),
                    new_image_file.getSize(),
                    new_image_file.getContentType());

            File directory = new File(PROFILE_IMAGE_DIR);
            if(!directory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                directory.mkdirs();
            }

            // 4. 기존 파일 삭제 (기본 프로필 이미지가 아닌 경우)
            if (before_url != null && !before_url.equals(base_profile_image_url)) {
                Path beforeFilePath = Paths.get(PROFILE_IMAGE_DIR + before_url);
                if (Files.exists(beforeFilePath)) {
                    try {
                        Files.delete(beforeFilePath);
                        log.info("기존 파일 삭제 성공: {}", before_url);
                    } catch (IOException e) {
                        log.warn("기존 파일 삭제 실패: {}, error: {}", before_url, e.getMessage());
                    }
                }
            }

            // 5. 새 파일 저장
            String newFileName = new_image_file.getOriginalFilename();
            if (newFileName == null || newFileName.trim().isEmpty()) {
                // 파일명이 없으면 임시 이름 생성
                newFileName = "profile_" + System.currentTimeMillis() + ".jpg";
                log.info("파일명이 없어 임시 이름 생성: {}", newFileName);
            }

            Path newFilePath = Paths.get(PROFILE_IMAGE_DIR + newFileName);

            // 6. 파일명 중복 체크 및 처리
            if (Files.exists(newFilePath)) {
                // 중복된 경우 타임스탬프 추가
                String nameWithoutExt = newFileName.substring(0, newFileName.lastIndexOf('.'));
                String extension = newFileName.substring(newFileName.lastIndexOf('.'));
                newFileName = nameWithoutExt + "_" + System.currentTimeMillis() + extension;
                newFilePath = Paths.get(PROFILE_IMAGE_DIR + newFileName);
                log.info("파일명 중복으로 새 이름 생성: {}", newFileName);
            }

            // 7. 실제 파일 저장
            try {
                Files.write(newFilePath, new_image_file.getBytes());
                log.info("새 파일 저장 성공: {}, 크기: {} bytes", newFileName, new_image_file.getSize());

                // 8. 저장된 파일 존재 여부 재확인
                if (Files.exists(newFilePath)) {
                    log.info("파일 저장 검증 완료: {}", newFilePath.toString());
                } else {
                    log.error("파일 저장 후 존재하지 않음: {}", newFilePath.toString());
                    throw new IOException("파일 저장에 실패했습니다.");
                }

            } catch (IOException e) {
                log.error("파일 저장 중 오류 발생: {}, error: {}", newFileName, e.getMessage());
                throw new IOException("파일 저장에 실패했습니다: " + e.getMessage());
            }

            return newFileName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
