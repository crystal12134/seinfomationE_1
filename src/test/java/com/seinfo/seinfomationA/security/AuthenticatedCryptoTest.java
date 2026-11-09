package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.EncriptUtil;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthenticatedCryptoTest {
 static final String DATA=Base64.getEncoder().encodeToString(new byte[32]);
 static final String LOOKUP=Base64.getEncoder().encodeToString(new byte[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
 @BeforeEach void configure(){EncriptUtil.configure(DATA,LOOKUP);}
 @Test void companyCiphertextUsesRandomNonce() throws Exception {String a=EncriptUtil.encryptCompanyNo("1234567890"),b=EncriptUtil.encryptCompanyNo("1234567890");assertNotEquals(a,b);assertEquals("1234567890",EncriptUtil.decryptCompanyNo(a));}
 @Test void lookupPreservesJoinsWithoutDeterministicEncryption() throws Exception {assertEquals(EncriptUtil.lookupCompanyNo("123-45-67890"),EncriptUtil.lookupCompanyNo("1234567890"));assertEquals(67,EncriptUtil.lookupCompanyNo("1234567890").length());}
 @Test void rejectsChangedCiphertext() throws Exception {String a=EncriptUtil.encryptCompanyNo("1234567890");byte[] p=Base64.getUrlDecoder().decode(a.substring(3));p[p.length-1]^=1;String changed="v2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(p);assertThrows(Exception.class,()->EncriptUtil.decryptCompanyNo(changed));}
 @Test void rejectsCrossPurposeCiphertext() throws Exception {String a=EncriptUtil.encryptAes256("1234567890");assertThrows(Exception.class,()->EncriptUtil.decryptCompanyNo(a));}
 @Test void rejectsLegacyCiphertext(){assertThrows(Exception.class,()->EncriptUtil.decryptAes256("00112233445566778899aabbccddeeff"));}
 @Test void urlParametersRoundTripWithUnicode() throws Exception {String value="requestNo=REQ123&name=테스트";assertEquals(value,EncriptUtil.decryptAes256(EncriptUtil.encryptAes256(value)));}
 @Test void rejectsSameOrShortKeys(){assertThrows(IllegalArgumentException.class,()->EncriptUtil.configure(DATA,DATA));assertThrows(IllegalArgumentException.class,()->EncriptUtil.configure("YQ==",LOOKUP));}
}
