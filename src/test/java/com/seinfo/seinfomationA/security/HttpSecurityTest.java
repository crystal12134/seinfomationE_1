package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.config.WebSecurityConfig;
import com.seinfo.seinfomationA.util.token.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringJUnitConfig(HttpSecurityTest.Config.class) @WebAppConfiguration
class HttpSecurityTest {
 @Configuration @EnableWebSecurity @EnableWebMvc @Import(WebSecurityConfig.class)
 static class Config {
  @Bean UserDetailsService users(){return new InMemoryUserDetailsManager();}
  @Bean JwtAuthenticationFilter authFilter() throws Exception {
   JwtAuthenticationFilter f=mock(JwtAuthenticationFilter.class);
   doAnswer(i->{((FilterChain)i.getArgument(2)).doFilter(i.getArgument(0),i.getArgument(1));return null;}).when(f).doFilter(any(),any(),any());return f;
  }
  @Bean JwtRefreshFilter refresh(){return mock(JwtRefreshFilter.class);}
  @Bean Endpoints endpoints(){return new Endpoints();}
 }
 @RestController static class Endpoints {
  @GetMapping("/") String home(){return "home";}
  @PostMapping("/board/freeBoardList/deletePost") String delete(){return "ok";}
  @PostMapping("/admin/save") String admin(){return "ok";}
  @PostMapping("/member/save") String work(){return "ok";}
  @PostMapping("/anyone/noticeList/deleteNotice") String notice(){return "ok";}
 }
 @org.springframework.beans.factory.annotation.Autowired WebApplicationContext context;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test void publicPageRemainsPublicAndIssuesCsrfCookie() throws Exception {mvc.perform(get("/")).andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"));}
 @Test void writeNeedsLogin() throws Exception {mvc.perform(post("/board/freeBoardList/deletePost").with(csrf())).andExpect(status().isUnauthorized());}
 @Test void loginAloneCannotBypassCsrf() throws Exception {mvc.perform(post("/board/freeBoardList/deletePost").with(user("alice"))).andExpect(status().isForbidden());}
 @Test void writeAllowsLoginWithCsrf() throws Exception {mvc.perform(post("/board/freeBoardList/deletePost").with(user("alice")).with(csrf())).andExpect(status().isOk());}
 @Test void adminUrlRejectsOrdinaryUser() throws Exception {mvc.perform(post("/admin/save").with(user("alice")).with(csrf())).andExpect(status().isForbidden());}
 @Test void noticeDeleteRequiresAdminEvenOnPublicPrefix() throws Exception {mvc.perform(post("/anyone/noticeList/deleteNotice").with(user("alice")).with(csrf())).andExpect(status().isForbidden());}
 @Test void employeeApiRejectsOrdinaryUser() throws Exception {mvc.perform(post("/member/save").with(user("alice")).with(csrf())).andExpect(status().isForbidden());}
 @Test void employeeAllowedWithCsrf() throws Exception {mvc.perform(post("/member/save").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isOk());}
}
