package com.seinfo.seinfomationA.util.security;
import org.springframework.stereotype.Component;
@Component("safeHtml")
public class SafeHtml { public String clean(String value){return SafeContent.html(value);} }
