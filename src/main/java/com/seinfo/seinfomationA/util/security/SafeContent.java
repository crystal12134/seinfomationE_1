package com.seinfo.seinfomationA.util.security;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;
public final class SafeContent {
    private SafeContent() {}
    public static String html(String input) {
        if(input==null) return null;
        return Jsoup.clean(input,"",Safelist.relaxed().removeTags("img").addEnforcedAttribute("a","rel","noopener noreferrer"),new Document.OutputSettings().prettyPrint(false));
    }
    public static String text(String input) { return input==null?null:Jsoup.parseBodyFragment(input).text(); }
}
