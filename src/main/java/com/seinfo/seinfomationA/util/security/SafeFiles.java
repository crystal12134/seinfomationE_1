package com.seinfo.seinfomationA.util.security;
import java.io.IOException;
import java.nio.file.*;
public final class SafeFiles {
    private SafeFiles() {}
    public static Path within(Path base, String relative) throws IOException {
        Path root=base.toAbsolutePath().normalize();
        String name=relative.replace('\\','/');
        if(name.isBlank() || name.startsWith("/") || name.contains(":")) throw new IOException("잘못된 파일 경로입니다.");
        Path result=root.resolve(name).normalize();
        if(result.equals(root) || !result.startsWith(root)) throw new IOException("저장 영역 밖의 경로입니다.");
        Path cursor=root;
        if(Files.isSymbolicLink(root)) throw new IOException("심볼릭 링크 경로는 허용하지 않습니다.");
        for(Path part:root.relativize(result)) {
            cursor=cursor.resolve(part);
            if(Files.isSymbolicLink(cursor)) throw new IOException("심볼릭 링크 경로는 허용하지 않습니다.");
        }
        return result;
    }
}
