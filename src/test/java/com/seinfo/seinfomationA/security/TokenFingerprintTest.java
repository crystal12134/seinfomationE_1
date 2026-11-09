package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.security.TokenFingerprint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TokenFingerprintTest {
 @Test void longTokensFitExistingDatabaseColumnsWithoutStoringCredentials(){String token="eyJ"+"a".repeat(2000)+".payload.signature";String stored=TokenFingerprint.of(token);assertEquals(64,stored.length());assertNotEquals(token,stored);assertEquals(stored,TokenFingerprint.of(stored));}
}
