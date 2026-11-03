package com.seinfo.seinfomationA.component.file.service.impl;

import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.util.response.*;
import com.seinfo.seinfomationA.util.security.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadInfo;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

@Service
@RequiredArgsConstructor
public class TusServiceImpl implements TusService {
    private final TusFileUploadService tus;
    private final TusMapper mapper;
    private final ResourceAccess access;
    private final ResponseEntityUtil responses;
    @Value("${tus.server.data.directory}") private String storage;
    @Value("${seinfo.security.max-upload-bytes:10000000}") private long maxBytes;
    private Path root() throws IOException { Path p=Paths.get(storage,"save"); Files.createDirectories(p); return p; }
    private ResponseEntity<?> ok(Object value) { return responses.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001",value); }
    private static String required(Map<String,String> m,String key) { return ResourceAccess.id(m.get(key)); }
    private static String extension(String name) {
        if(name==null || name.length()>200 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(c->c<32)) throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        int dot=name.lastIndexOf('.');
        if(dot<=0) throw new IllegalArgumentException("확장자가 필요합니다.");
        return name.substring(dot+1).toLowerCase(Locale.ROOT);
    }
    private Map<String,Object> settings(Map<String,String> m,long size) throws Exception {
        String group=required(m,"fileGrpId"); required(m,"fileId");
        String info=required(m,"fileInfoId");
        if(!Set.of("pps","not","worklog").contains(m.get("fileWork"))) throw new IllegalArgumentException("허용되지 않은 파일 업무입니다.");
        if("worklog".equals(m.get("fileWork"))) access.requireStaff();
        Map<String,Object> config=mapper.selectMfileInfo(Map.of("mfileInfoId",info));
        if(config==null) throw new IllegalArgumentException("첨부 설정이 없습니다.");
        access.fileGroup(group,true,true);
        Map<String,Object> previous=mapper.selectMfileGroup(Map.of("mfileGrpId",group));
        if(previous!=null && !info.equals(previous.get("mfileInfoId"))) throw new IllegalArgumentException("첨부 설정이 일치하지 않습니다.");
        long limit=Math.min(maxBytes,Long.parseLong(config.get("mfileMaxSize").toString()));
        if(size<=0 || size>limit) throw new IllegalArgumentException("파일 용량 제한을 초과했습니다.");
        String ext=extension(m.get("filename"));
        boolean allowed=Arrays.stream(config.get("mfileExt").toString().split(",")).map(String::trim).anyMatch(ext::equalsIgnoreCase);
        if(!allowed || !Set.of("pdf","ppt","pptx","zip").contains(ext)) throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
        if(mapper.selectMfileListCnt(Map.of("mfileGrpId",group))>=Integer.parseInt(config.get("mfileMaxCnt").toString())) throw new IllegalArgumentException("첨부 개수 제한을 초과했습니다.");
        if(!mapper.selectMfileList(Map.of("mfileId",required(m,"fileId"))).isEmpty()) throw new IllegalArgumentException("중복 파일 번호입니다.");
        return config;
    }
    private Map<String,String> metadata(String header) {
        Map<String,String> result=new HashMap<>();
        if(header==null || header.length()>8192) throw new IllegalArgumentException("파일 정보가 없습니다.");
        for(String part:header.split(",")) {
            String[] pair=part.trim().split(" ",2);
            if(pair.length!=2 || !pair[0].matches("[A-Za-z_][A-Za-z0-9_]{0,40}") || result.put(pair[0],new String(Base64.getDecoder().decode(pair[1]),StandardCharsets.UTF_8))!=null) throw new IllegalArgumentException("파일 정보 형식 오류입니다.");
        }
        return result;
    }
    @Override @Transactional
    public synchronized String tusUpload(HttpServletRequest request,HttpServletResponse response) throws Exception {
        String owner=access.email();
        if("GET".equals(request.getMethod())) throw new org.springframework.security.access.AccessDeniedException("직접 다운로드는 허용하지 않습니다.");
        HttpServletRequest actual=request;
        if("POST".equals(request.getMethod())) {
            Map<String,String> m=metadata(request.getHeader("Upload-Metadata"));
            long length=Long.parseLong(Objects.toString(request.getHeader("Upload-Length"),"0"));
            settings(m,length);
            m.put("_owner",owner);
            String header=m.entrySet().stream().map(e->e.getKey()+" "+Base64.getEncoder().encodeToString(e.getValue().getBytes(StandardCharsets.UTF_8))).collect(java.util.stream.Collectors.joining(","));
            actual=new HttpServletRequestWrapper(request) {
                @Override public String getHeader(String n) {return n.equalsIgnoreCase("Upload-Metadata")?header:super.getHeader(n);}
                @Override public Enumeration<String> getHeaders(String n) {return n.equalsIgnoreCase("Upload-Metadata")?Collections.enumeration(List.of(header)):super.getHeaders(n);}
            };
        } else {
            UploadInfo info=tus.getUploadInfo(request.getRequestURI());
            if(info==null || !owner.equals(info.getMetadata().get("_owner"))) throw new org.springframework.security.access.AccessDeniedException("본인이 시작한 업로드만 사용할 수 있습니다.");
        }
        tus.process(actual,response);
        if(response.getStatus()>=400 || "DELETE".equals(request.getMethod())) return null;
        UploadInfo upload=tus.getUploadInfo(request.getRequestURI());
        if(upload==null || upload.isUploadInProgress()) return null;
        Map<String,String> m=upload.getMetadata();
        settings(m,upload.getOffset());
        String group=required(m,"fileGrpId"), fileId=required(m,"fileId");
        String relative=m.get("fileWork")+"/"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"/"+fileId+"."+extension(m.get("filename"));
        Path file=SafeFiles.within(root(),relative);
        Files.createDirectories(file.getParent());
        boolean success=false, created=false;
        try {
            try(OutputStream out=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)) {
                created=true;
                try(InputStream in=tus.getUploadedBytes(request.getRequestURI())) {
                byte[] chunk=new byte[8192];long total=0;int n;
                while((n=in.read(chunk))!=-1) {total+=n;if(total>maxBytes)throw new IOException("파일 용량 제한을 초과했습니다.");out.write(chunk,0,n);}
                if(total!=upload.getOffset())throw new IOException("파일 크기가 일치하지 않습니다.");
                }
            }
            FileSignature.check(file,extension(m.get("filename")));
            Map<String,Object> row=new HashMap<>();
            row.put("mfileId",fileId);row.put("mfileNm",m.get("filename"));row.put("mfilePath",relative);
            row.put("mfileSize",Files.size(file));row.put("mfileType","application/octet-stream");row.put("mfileGrpId",group);
            row.put("mfileInfoId",required(m,"fileInfoId"));row.put("regUserId",owner);row.put("mfileStatus","01");
            if(mapper.selectMfileGroupCnt(row)==0)mapper.insertMfileGroup(row);
            mapper.insertMfileList(row);success=true;
        } finally {if(created && !success)Files.deleteIfExists(file);tus.deleteUpload(request.getRequestURI());}
        return "success";
    }
    @Override public ResponseEntity<?> getMfileInfo(Map<String,Object> data) throws Exception {
        return ok(mapper.selectMfileInfo(Map.of("mfileInfoId",ResourceAccess.id(data.get("mfileInfoId")))));
    }
    @Override public ResponseEntity<?> getMfileGroupInfo(Map<String,Object> data) throws Exception {
        String group=ResourceAccess.id(data.get("mfileGrpId"));access.fileGroup(group,false,false);
        Map<String,Object> row=mapper.selectMfileGroup(Map.of("mfileGrpId",group));
        Map<String,Object> result=new HashMap<>();result.put("mFileGroup",mapper.selectMfileInfo(row));
        result.put("fileList",mapper.selectMfileListClient(Map.of("mfileGrpId",group)));result.put("mfileInfoId",row.get("mfileInfoId"));return ok(result);
    }
    @Override @Transactional public ResponseEntity<?> getMfileDownInfo(Map<String,Object> data) throws Exception {
        String group=ResourceAccess.id(data.get("mfileGrpId"));access.fileGroup(group,false,false);
        Map<String,Object> query=new HashMap<>();query.put("mfileGrpId",group);
        boolean zip="zip".equals(data.get("type"));
        if(!zip)query.put("mfileId",ResourceAccess.id(data.get("mfileId")));
        List<Map<String,Object>> files=mapper.selectMfileList(query);
        if(files.isEmpty())throw new IllegalArgumentException("첨부파일이 없습니다.");
        Map<String,Object> row=new HashMap<>();String down="filedown-"+UUID.randomUUID();
        row.put("mfileDownId",down);row.put("mfileGroupId",group);row.put("mfileDownType",zip?"z":"n");
        row.put("mfileNm",zip?"files.zip":files.get(0).get("mfileNm"));
        row.put("regUserId",downloadOwner());
        if(!zip){row.put("mfileId",files.get(0).get("mfileId"));row.put("mfileDownPath",SafeFiles.within(root(),files.get(0).get("mfilePath").toString()).toString());}
        mapper.insertMfileDown(row);return ok(Map.of("mfileDownId",down));
    }
    private String downloadOwner() {try{return access.email();}catch(org.springframework.security.access.AccessDeniedException e){return "public-download";}}
    @Override @Transactional public ResponseEntity<?> getFileDown(HttpServletResponse response,Map<String,Object> param) throws Exception {
        param.put("regUserId",downloadOwner());
        Map<String,Object> permit=mapper.selectMfileDown(param);
        if(permit==null)throw new org.springframework.security.access.AccessDeniedException("다운로드 권한이 없습니다.");
        access.fileGroup(permit.get("mfileGroupId"),false,false);
        List<Map<String,Object>> files=mapper.selectMfileList(Map.of("mfileGrpId",permit.get("mfileGroupId")));
        if("n".equals(permit.get("mfileDownType")))files=files.stream().filter(f->Objects.equals(f.get("mfileId"),permit.get("mfileId"))).toList();
        if(files.isEmpty())throw new IllegalArgumentException("첨부파일이 없습니다.");
        if(mapper.updateMfileDown(param)!=1)throw new org.springframework.security.access.AccessDeniedException("사용된 다운로드입니다.");
        if("n".equals(permit.get("mfileDownType"))) {
            Path file=SafeFiles.within(root(),files.get(0).get("mfilePath").toString());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(files.get(0).get("mfileNm").toString(),StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").body(new FileSystemResource(file));
        }
        response.setContentType("application/zip");response.setHeader("Content-Disposition","attachment; filename=files.zip");response.setHeader("Cache-Control","no-store");
        try(ZipOutputStream zip=new ZipOutputStream(response.getOutputStream())) {
            int index=0;for(Map<String,Object> f:files) {
                String name=Paths.get(f.get("mfileNm").toString().replace('\\','/')).getFileName().toString();
                zip.putNextEntry(new ZipEntry((++index)+"_"+name));Files.copy(SafeFiles.within(root(),f.get("mfilePath").toString()),zip);zip.closeEntry();
            }
        }
        return null;
    }
    @Override @Transactional public ResponseEntity<?> deleteFile(List<Map<String,Object>> data) throws Exception {
        access.email();if(data==null || data.isEmpty() || data.size()>100)throw new IllegalArgumentException("삭제 목록 오류입니다.");
        List<Map<String,Object>> checked=new ArrayList<>();
        for(Map<String,Object> item:data) {
            String group=ResourceAccess.id(item.get("fileGrpId"));access.fileGroup(group,true,false);
            String id=ResourceAccess.id(item.get("fileId"));Map<String,Object> key=Map.of("fileGrpId",group,"fileId",id);
            Map<String,Object> row=mapper.selectMfilePath(key);if(row==null)throw new IllegalArgumentException("첨부파일이 없습니다.");
            checked.add(Map.of("group",group,"id",id,"path",SafeFiles.within(root(),row.get("mfilePath").toString())));
        }
        for(Map<String,Object> row:checked) {
            mapper.deleteMfileList(Map.of("mfileId",row.get("id")));
            if(mapper.selectMfileListCnt(Map.of("mfileGrpId",row.get("group")))==0)mapper.deleteMfileGroup(Map.of("mfileGrpId",row.get("group")));
        }
        java.util.List<Path> paths=checked.stream().map(row->(Path)row.get("path")).toList();
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit(){for(Path path:paths)try{Files.deleteIfExists(path);}catch(IOException e){org.slf4j.LoggerFactory.getLogger(TusServiceImpl.class).warn("첨부파일 물리 삭제 재시도가 필요합니다.");}}
            });
        } else {for(Path path:paths)Files.deleteIfExists(path);}
        return ok(Map.of("message","삭제되었습니다."));
    }
}
