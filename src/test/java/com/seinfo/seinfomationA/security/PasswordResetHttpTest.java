package com.seinfo.seinfomationA.security;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.containsString;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("securitytest")
class PasswordResetHttpTest {
 @Autowired MockMvc mvc;
 @Test void anonymousCanOpenResetFormAndReceivesCsrfCookie()throws Exception{
  mvc.perform(get("/login/passwordReset")).andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"))
   .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Referrer-Policy","no-referrer"))
   .andExpect(content().string(containsString("id=\"resetForm\""))).andExpect(content().string(containsString("password-reset.js")));
 }
 @Test void resetCompletionStillRequiresCsrf()throws Exception{
  mvc.perform(post("/login/passwordReset/complete").contentType("application/json").content("{\"token\":\"invalid\",\"newPassword\":\"Changed9!pass\"}")).andExpect(status().isForbidden());
 }
 @Test void invalidTokenWithValidCsrfIsRejectedByResetService()throws Exception{
  mvc.perform(post("/login/passwordReset/complete").with(csrf()).contentType("application/json").content("{\"token\":\""+"A".repeat(43)+"\",\"newPassword\":\"Changed9!pass\"}")).andExpect(status().isBadRequest());
 }
}
