package com.seinfo.protoA.component.file.service.impl;

import com.seinfo.protoA.component.file.mapper.TusMapper;
import com.seinfo.protoA.component.file.service.TusService;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import com.seinfo.protoA.util.token.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadInfo;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TusServiceImpl implements TusService {

    private final TusFileUploadService tusFileUploadService;
    private final TokenUtils tokenUtil;
    private final ResponseEntityUtil responseEntityUtil;
    private final TusMapper tusMapper;

    @Value("${server.servlet.context-path}")
    private String serverContextPath;

    @Value("${server.port}")
    private String serverPort;

    @Value("${tus.server.data.directory}")
    private String savePath;

    /**
     * 파일 업로드
     * @throws Exception
     */

    @Transactional
    public String tusUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {

        //업로드
        tusFileUploadService.process(request, response);

        // 현재 업로드 정보
        UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(request.getRequestURI());

        // 완료 된 경우 파일 저장
        if (uploadInfo != null && !uploadInfo.isUploadInProgress()) {
            // original filename
            String originalFilename = uploadInfo.getFileName();
            String[] split = originalFilename.split("\\.");

            // 파일 정보(메타 정보)
            Map<String, String> metaData = uploadInfo.getMetadata();
            checkUploadFile(uploadInfo, metaData);

            String fileId = metaData.get("fileId");
            // 파일 이름 저장 양식 : systemid + '-' + uuid + '-' + 아이디.확장자
            String saveFileName = fileId + "." + split[split.length - 1];

            // 파일 업로드 임시 저장 경로
            String filePath = new SimpleDateFormat("yyMMdd").format(new Date());
            String fileWork = metaData.get("fileWork")+"/";
            String uploadedPath = savePath + "/save/" + fileWork + filePath;

            // 사용자 ID
            String id = tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID) == null ? "unknownuser"
                    : tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID).toString();

            File file = new File(uploadedPath, saveFileName);
            FileUtils.copyInputStreamToFile(tusFileUploadService.getUploadedBytes(request.getRequestURI()), file);

            // DB에 정보 저장
            Map<String, Object> mfileInfo = new HashMap<>();
            mfileInfo.put("mfileId", fileId);
            mfileInfo.put("mfileNm", originalFilename);
            mfileInfo.put("mfilePath", fileWork + filePath + "/" + saveFileName);
            mfileInfo.put("mfileSize", metaData.get("filesize"));
            mfileInfo.put("mfileType", metaData.get("filetype"));
            mfileInfo.put("mfileGrpId", metaData.get("fileGrpId"));
            mfileInfo.put("mfileInfoId", metaData.get("fileInfoId"));
            mfileInfo.put("hostname", InetAddress.getLocalHost().getCanonicalHostName() + ":" + serverPort + serverContextPath);
            mfileInfo.put("regUserId", id);
            mfileInfo.put("mfileStatus", "01") ;// 01: 임시저장, 02: 복호처리전 단계, 03:완료

            if(tusMapper.selectMfileGroupCnt(mfileInfo) == 0){
                tusMapper.insertMfileGroup(mfileInfo);
            }
            tusMapper.insertMfileList(mfileInfo);

            tusFileUploadService.deleteUpload(request.getRequestURI());

            return "success";
        }
        return null;
    }

    @Override
    public ResponseEntity<?> getMfileInfo(Map<String, Object> data) throws Exception {
        if(data.get("mfileInfoId") == null) {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "E001", "E001");
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", tusMapper.selectMfileInfo(data));
    }

    @Override
    public ResponseEntity<?> getMfileGroupInfo(Map<String, Object> data) throws Exception {
        if(data.get("mfileGrpId") == null) {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "E001", "E001");
        }
        Map<String, Object> mFileGroup = tusMapper.selectMfileGroup(data);

        Map<String, Object> retVal = new HashMap<>();
        retVal.put("mFileGroup", tusMapper.selectMfileInfo(mFileGroup));
        retVal.put("fileList", tusMapper.selectMfileListClient(data));
        retVal.put("mfileInfoId", mFileGroup != null? mFileGroup.get("mfileInfoId") : null);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", retVal);
    }

    /**
     * 파일 업로드 체크
     *
     * @param uploadInfo
     * @param metaData
     * @throw Exception
     */
    private void checkUploadFile(UploadInfo uploadInfo, Map<String, String> metaData) throws Exception {
        if(uploadInfo.getOffset() <= (1024 * 1024 * 5)) {
            metaData.put("mfileGrpId", metaData.get("fileGrpId"));
            Map<String, Object> data = tusMapper.selectMfileInfoByFileGroupId(metaData);
            if(data != null){
                log.info(metaData.toString());
                // 클라이언트 수신값
                String fileName = metaData.get("filename");
                String fileSize = metaData.get("filesize");

                // 서버 설정값
                String fileExt = data.get("mfileExt").toString();
                String fileMaxSize = data.get("fileMaxSize").toString();

                String[] fileExts = fileExt.split(",");
                String[] file = fileName.split("\\.");
                boolean checkExt = false;

                if(file.length > 0) {
                    for (String tmp : fileExts) {
                        if (tmp.equals(file[file.length - 1].toLowerCase())) {
                            checkExt = true;
                            break;
                        }
                    }
                }
                if(!checkExt) throw new RuntimeException("filename extends invalid");

                if(!(Long.parseLong(fileSize) <= Long.parseLong(fileMaxSize))) throw new RuntimeException("filesize invalid");
            }
        }
    }

    /* 파일 다운 허용 */
    @Override
    public ResponseEntity<?> getMfileDownInfo(Map<String, Object> data) throws Exception {
        Map<String, Object> errorResult = new HashMap<>();
        if(data.get("mfileGrpId") == null) {
            errorResult.put("status", "error");
            errorResult.put("message", "key null error : (mfileGrpId)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        String filePath = savePath.replaceAll("\\\\", "/") + "/save/";
        List<Map<String, Object>> fileList = tusMapper.selectMfileList(data);
        Map<String, Object> tmpDat = new HashMap<>();

        if("zip".equals(data.get("type"))){
            String mfileGrpId = data.get("mfileGrpId").toString();
            if (!mfileGrpId.isEmpty()) {
                if(tusMapper.selectMfileListCnt(Map.of("mfileGrpId", mfileGrpId)) == 0) {
                    errorResult.put("status", "error");
                    errorResult.put("message", "there are no files to download : (mfileGrpId)");
                    return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
                }
            }
            tmpDat.put("mfileNm", "filedown.zip");
            tmpDat.put("mfileGroupId", data.get("mfileGrpId"));
            tmpDat.put("mfileDownType", "z");
        }else {
            if(data.get("mfileId") == null) {
                errorResult.put("status", "error");
                errorResult.put("message", "key null error : (mfileId)");
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }
            if(fileList == null || fileList.isEmpty()) {
                errorResult.put("status", "error");
                errorResult.put("message", "there are no files to download : (mfileId)");
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            } else {
                Map<String, Object> file = fileList.get(0);
                tmpDat.put("mfileNm", file.get("mfileNm"));
                tmpDat.put("mfileGroupId", file.get("mfileGroupId"));
                tmpDat.put("mfileId", file.get("mfileId"));
                tmpDat.put("mfileDownType", "n");
                tmpDat.put("mfileDownPath", filePath + file.get("mfilePath"));
            }
        }

        // 사용자 ID
        String usrId = tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID) == null ? "unknownuser"
                : tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID).toString();
        String mfileDownId = "filedown-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-"
                + UUID.randomUUID().toString();

        tmpDat.put("regUserId", usrId);
        tmpDat.put("mfileDownId", mfileDownId);
        tusMapper.insertMfileDown(tmpDat);

        Map<String, Object> retVal = new HashMap<>();
        retVal.put("mfileDownId", mfileDownId);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", retVal);
    }

    @Override
    @Transactional
    public ResponseEntity<?> getFileDown(HttpServletResponse response, Map<String, Object> param) throws Exception {
        // 사용자 ID
        String usrId = tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID) == null ? "unknownuser"
                : tokenUtil.getClaimsByValue(TokenUtils.TOKEN_USER_ID).toString();
        param.put("regUserId", usrId);
        Map<String, Object> tmp = tusMapper.selectMfileDown(param);

        if(tmp != null) {
            tusMapper.updateMfileDown(param);
            String originalFilename = tmp.get("mfileNm").toString();

            // 일반 다운로드인 경우
            if("n".equals(tmp.get("mfileDownType"))){
                String fileFullPath = tmp.get("mfileDownPath").toString();
                File file = new File(fileFullPath);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + URLEncoder.encode(originalFilename, "UTF-8").replaceAll("\\+", "%20"))
                        .contentLength(file.length()).body(new InputStreamResource(new FileInputStream(fileFullPath)));
            }else if("z".equals(tmp.get("mfileDownType"))){

                List<Map<String, Object>> tmpList = tusMapper.selectMfileListPath(param);

                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename="+originalFilename);
                String filePath = savePath.replaceAll("\\\\", "/") + "/save/";

                try  (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
                    for(Map<String, Object> tmp1 : tmpList) {
                        Path file = Paths.get(filePath + tmp1.get("mfilePath"));
                        try (InputStream inputStream = Files.newInputStream(file)) {
                            zipOutputStream.putNextEntry(new ZipEntry(tmp1.get("mfileNm").toString()));
                            StreamUtils.copy(inputStream, zipOutputStream);
                            zipOutputStream.flush();
                            zipOutputStream.closeEntry();
                        }
                    }
                }
            }
            return null;
        }
        throw new RuntimeException("You have no permission to download");
    }

    @Transactional
    @Override
    public ResponseEntity<?> deleteFile(List<Map<String, Object>> data) throws Exception {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "error");

        if (data == null || data.isEmpty()) {
            errorResult.put("message", "DELETE FAIL : deleteArray null");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        try {
            // fileGrpId로 그룹핑
            Map<String, List<Map<String, Object>>> groupMap = data.stream()
                    .filter(m -> m.get("fileGrpId") != null)
                    .collect(Collectors.groupingBy(m -> (String) m.get("fileGrpId")));

            for (Map.Entry<String, List<Map<String, Object>>> entry : groupMap.entrySet()) {
                String fileGrpId = entry.getKey();
                List<Map<String, Object>> fileList = entry.getValue();

                for (Map<String, Object> fileMap : fileList) {
                    String mfilePath = (String) fileMap.get("mfilePath");
                    String fileId = (String) fileMap.get("fileId");

                    if (fileId == null || fileId.isEmpty()) {
                        errorResult.put("message", "DELETE FAIL : fileId is null");
                        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
                    }

                    if (mfilePath == null || mfilePath.isEmpty()) {
                        Map<String, Object> dbResult = tusMapper.selectMfilePath(fileMap);
                        if (dbResult == null || dbResult.get("mfilePath") == null) {
                            errorResult.put("message", "DELETE FAIL : mfilePath is null from DB");
                            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
                        }
                        mfilePath = dbResult.get("mfilePath").toString();
                    }

                    if (mfilePath != null && !mfilePath.isEmpty()) {
                        int iNum = mfilePath.lastIndexOf("/");
                        String str = mfilePath.substring(0, iNum);
                        String uploadedPath = savePath + "/save/" + str;
                        String realFileName = mfilePath.substring(iNum);

                        File targetFile = new File(uploadedPath, realFileName);
                        targetFile.delete();

                        tusMapper.deleteMfileList(Map.of("mfileId", fileId));
                    } else {
                        errorResult.put("message", "DELETE FAIL : mfilePath is null");
                        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
                    }
                }

                if (fileGrpId != null && !fileGrpId.isBlank()) {
                    if (tusMapper.selectMfileListCnt(Map.of("mfileGrpId", fileGrpId)) == 0) {
                        tusMapper.deleteMfileGroup(Map.of("mfileGrpId", fileGrpId));
                    }
                }
            }
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", Map.of("message", "DELETE SUCCESS"));

        } catch (Exception e) {
            errorResult.put("message", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

}
