package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.security.SafeContent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ContentSecurityTest {
 @Test void removesScriptsAndEventHandlers(){String s=SafeContent.html("<p>안녕</p><script>alert(1)</script><img src=x onerror=alert(1)>");assertFalse(s.contains("script"));assertFalse(s.contains("onerror"));assertTrue(s.contains("안녕"));}
 @Test void removesJavascriptUrls(){String s=SafeContent.html("<a href='javascript:alert(1)'>link</a>");assertFalse(s.contains("javascript:"));}
 @Test void keepsBasicEditorFormatting(){assertTrue(SafeContent.html("<p><strong>중요</strong></p>").contains("<strong>중요</strong>"));}
}
