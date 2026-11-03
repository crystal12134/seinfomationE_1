package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.security.*;
import com.seinfo.seinfomationA.component.file.service.impl.TusServiceImpl;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import me.desair.tus.server.TusFileUploadService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FileSecurityTest {
 @TempDir Path dir;
 @Test void preventsTraversalAndAbsolutePaths(){for(String p:List.of("../secret","/etc/passwd","C:/Windows/file","..\\secret"))assertThrows(IOException.class,()->SafeFiles.within(dir,p));}
 @Test void preventsSymlinkEscape() throws Exception {Path outside=Files.createDirectory(dir.resolve("outside"));Path base=Files.createDirectory(dir.resolve("save"));Files.createSymbolicLink(base.resolve("link"),outside);assertThrows(IOException.class,()->SafeFiles.within(base,"link/file"));}
 @Test void rejectsHtmlNamedPdf() throws Exception {Path p=dir.resolve("fake.pdf");Files.writeString(p,"<script>alert(1)</script>");assertThrows(IOException.class,()->FileSignature.check(p,"pdf"));}
 @Test void acceptsPdfSignature() throws Exception {Path p=dir.resolve("test.pdf");Files.writeString(p,"%PDF-1.7\n");assertDoesNotThrow(()->FileSignature.check(p,"pdf"));}
 @Test void deletionIgnoresUntrustedClientPath() throws Exception {
  TusMapper mapper=mock(TusMapper.class);ResourceAccess access=mock(ResourceAccess.class);when(access.email()).thenReturn("alice");
  TusServiceImpl service=new TusServiceImpl(mock(TusFileUploadService.class),mapper,access,mock(ResponseEntityUtil.class));ReflectionTestUtils.setField(service,"storage",dir.toString());
  Path saved=dir.resolve("save/owned/file.pdf");Files.createDirectories(saved.getParent());Files.writeString(saved,"%PDF-");Path unrelated=dir.resolve("secret.txt");Files.writeString(unrelated,"keep");
  when(mapper.selectMfilePath(anyMap())).thenReturn(Map.of("mfilePath","owned/file.pdf"));
  service.deleteFile(List.of(Map.of("fileGrpId","grp","fileId","file1","mfilePath","../secret.txt")));
  assertFalse(Files.exists(saved));assertTrue(Files.exists(unrelated));verify(access).fileGroup("grp",true,false);
 }
 @Test void rejectsOversizeUploadBeforeWritingAnyBytes() throws Exception {
  TusMapper mapper=mock(TusMapper.class);ResourceAccess access=mock(ResourceAccess.class);TusFileUploadService tus=mock(TusFileUploadService.class);when(access.email()).thenReturn("alice");
  TusServiceImpl service=new TusServiceImpl(tus,mapper,access,mock(ResponseEntityUtil.class));ReflectionTestUtils.setField(service,"maxBytes",10000000L);
  when(mapper.selectMfileInfo(anyMap())).thenReturn(Map.of("mfileMaxSize",10000000,"mfileMaxCnt",3,"mfileExt","pdf"));
  MockHttpServletRequest req=new MockHttpServletRequest("POST","/upload");req.addHeader("Upload-Length","20000000");
  Map<String,String> m=Map.of("fileGrpId","grp","fileId","file1","fileInfoId","SEINFO001","fileWork","pps","filename","file.pdf");
  req.addHeader("Upload-Metadata",m.entrySet().stream().map(e->e.getKey()+" "+Base64.getEncoder().encodeToString(e.getValue().getBytes())).collect(java.util.stream.Collectors.joining(",")));
  assertThrows(IllegalArgumentException.class,()->service.tusUpload(req,new MockHttpServletResponse()));verify(tus,never()).process(any(),any());
 }
}
