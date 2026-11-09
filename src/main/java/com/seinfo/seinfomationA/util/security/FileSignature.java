package com.seinfo.seinfomationA.util.security;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.zip.ZipFile;
public final class FileSignature {
    private FileSignature() {}
    public static void check(Path file,String extension) throws IOException {
        byte[] head;try(InputStream in=Files.newInputStream(file)){head=in.readNBytes(1024);}
        boolean valid=switch(extension) {
            case "pdf" -> new String(head,java.nio.charset.StandardCharsets.ISO_8859_1).contains("%PDF-");
            case "ppt" -> head.length>=8 && Arrays.equals(Arrays.copyOf(head,8),new byte[]{(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1});
            case "pptx","zip" -> zip(file,extension.equals("pptx"));
            default -> false;
        };
        if(!valid)throw new IOException("확장자와 파일 형식이 일치하지 않습니다.");
    }
    private static boolean zip(Path file,boolean presentation) {
        try(ZipFile zip=new ZipFile(file.toFile())) {
            if(zip.size()>10000)return false;
            return !presentation || (zip.getEntry("[Content_Types].xml")!=null && zip.getEntry("ppt/presentation.xml")!=null);
        } catch(IOException e){return false;}
    }
}
