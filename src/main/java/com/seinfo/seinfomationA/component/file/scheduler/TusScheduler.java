package com.seinfo.seinfomationA.component.file.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.desair.tus.server.TusFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class TusScheduler {

    private final TusFileUploadService tus;

    @Value("${tus.server.data.directory}")
    private String UPLOAD_FOLDER;

    @PostConstruct
    public void init() {
        autoFolder(UPLOAD_FOLDER);
    }

    // 폴더 자동 생성 기능
    public void autoFolder(String folderName) {
        File folder = new File(folderName);
        if (!folder.exists()) {
            try {
                boolean created = folder.mkdirs(); // 상위 폴더까지 생성
                if (created) {
                    log.info("폴더가 생성 완료되었습니다. 폴더명: {}", folderName);
                } else {
                    log.info("폴더 생성에 실패했습니다.");
                }
            } catch (Exception e) {
                log.error("폴더 생성 중 오류가 발생하였습니다. >>> {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 360 * 1000)
    public void cleanup() throws Exception {
        tus.cleanup();
    }
}
