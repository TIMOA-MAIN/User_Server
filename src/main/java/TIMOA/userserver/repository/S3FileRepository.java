package TIMOA.userserver.repository;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class S3FileRepository {

    private final String bucketName = "timoa-profileimage";

    private final String bucketName_file = "timoa-filedownload";

    private final String fixedFileName = "TIMOA-Client.msi";

    private final String s3Region = "ap-northeast-2";

    private final S3Template s3Template;


    public String uploadFile(MultipartFile file) throws IOException {
        try {
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

            s3Template.upload(bucketName, uniqueFileName, file.getInputStream());

            log.info("S3 파일 업로드 성공: 원본={}, 저장={}",
                    file.getOriginalFilename(), uniqueFileName);

            return uniqueFileName;

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: 파일명={}, 오류={}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("S3 업로드 실패: " + e.getMessage());
        }
    }

    public String downloadFile(String fileName) throws IOException {
        try {
            Resource resource = s3Template.download(bucketName, fileName);

            if (resource == null || !resource.exists()) {
                throw new IOException("파일을 찾을 수 없습니다: " + fileName);
            }

            return resourceToBase64(resource);

        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패: 파일명={}, 오류={}", fileName, e.getMessage(), e);
            throw new IOException("S3 다운로드 실패: " + e.getMessage());
        }
    }

    // 파일 삭제
    public void deleteFile(String fileName) {
        try {
            s3Template.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            log.info("S3 파일 삭제 실패");
        }
    }

    /*
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = "unnamed_file";
        }

        // 확장자 추출
        String extension = getFileExtension(originalFileName);

        // UUID + 확장자로 고유 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString();

        if (!extension.isEmpty()) {
            uniqueFileName += "." + extension;
        }

        log.debug("고유 파일명 생성: 원본={}, 생성={}", originalFileName, uniqueFileName);

        return uniqueFileName;
    }

    /*
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }

        return "";
    }

    private String resourceToBase64(Resource resource) throws Exception {
        if (resource == null) {
            throw new Exception("리소스가 null입니다.");
        }

        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("리소스를 Base64로 변환 중 오류 발생: {}", e.getMessage());
            throw new Exception("이미지 변환 실패");
        }
    }

    /**
     * 배포 파일을 고정된 이름으로 업로드
     */
    public String file_download_url(MultipartFile file) throws IOException {

        s3Template.deleteObject(bucketName_file, fixedFileName);

        s3Template.upload(bucketName_file, fixedFileName, file.getInputStream());

        log.info("배포 파일 업로드 완료: fileName={}", fixedFileName);

        return getPublicUrl(fixedFileName);
    }

    /**
     * Public S3 URL 생성 (영구 유효)
     */
    public String getPublicUrl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName_file, s3Region, fileName);
    }
}
