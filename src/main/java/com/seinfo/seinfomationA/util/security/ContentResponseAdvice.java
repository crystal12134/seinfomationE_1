package com.seinfo.seinfomationA.util.security;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import java.util.*;
@ControllerAdvice
public class ContentResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c){return true;}
    @Override public Object beforeBodyWrite(Object body,MethodParameter p,MediaType t,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest q,ServerHttpResponse r){
        if(body instanceof com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel model) model.setData(clean(model.getData()));
        else return clean(body);
        return body;
    }
    public static Object clean(Object value) {
        if(value instanceof Map<?,?> map){
            Map<Object,Object> safe=new LinkedHashMap<>();
            map.forEach((key,item)->{
                String k=String.valueOf(key).toLowerCase(Locale.ROOT);
                if(item instanceof String s && (k.endsWith("content") || k.endsWith("detail") || k.endsWith("answer") || k.endsWith("question"))) safe.put(key,SafeContent.html(s));
                else safe.put(key,clean(item));
            });return safe;
        }
        if(value instanceof List<?> list)return list.stream().map(ContentResponseAdvice::clean).toList();
        return value;
    }
}
