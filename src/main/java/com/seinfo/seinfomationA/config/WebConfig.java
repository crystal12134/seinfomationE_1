package com.seinfo.seinfomationA.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override public void postHandle(jakarta.servlet.http.HttpServletRequest req,jakarta.servlet.http.HttpServletResponse res,Object handler,org.springframework.web.servlet.ModelAndView view) {
                if(view!=null) {
                    var safe=(java.util.Map<String,Object>)com.seinfo.seinfomationA.util.security.ContentResponseAdvice.clean(view.getModel());
                    view.getModel().clear();view.getModel().putAll(safe);
                }
            }
        });
    }


	@Value("${tus.server.data.directory}")
	private String tusStoragePath;
	
	@Value("${tus.server.data.expiration}")
	private Long tusExpirationPeriod;
	
	@Value("${server.servlet.context-path}")
	private String contextPath;
	
	/**
	 * 다국어 메세지 위치 지정
	 * @return
	 */

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**/*.map")
				.addResourceLocations("classpath:/ignore/");;

		registry.addResourceHandler("/.well-known/**")
				.addResourceLocations("classpath:/ignore/");
	}
	
	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		String[] message = {"classpath:/messages/seinfo/seinfo"};
		messageSource.setBasenames(message);
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setCacheSeconds(600);
		return messageSource;
	}

	@Bean
	@Primary
	public TusFileUploadService tus() {
		if("/".equals(contextPath)) {
			contextPath = "";
		}
		return new TusFileUploadService().withStoragePath(tusStoragePath).withMaxUploadSize(10000000L)
				.withUploadExpirationPeriod(tusExpirationPeriod).withThreadLocalCache(true)
				.withUploadUri(contextPath + "/upload");
	}
}
