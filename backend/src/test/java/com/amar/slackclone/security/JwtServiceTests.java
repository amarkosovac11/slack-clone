package com.amar.slackclone.security;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTests{
@Test void rejectsSigningKeysShorterThan256Bits(){String weak=java.util.Base64.getEncoder().encodeToString("too-short".getBytes());assertThrows(IllegalStateException.class,()->new JwtService(weak,60000));}
@Test void generatedTokenIsValidAndContainsSubject(){String key=java.util.Base64.getEncoder().encodeToString("a-production-key-needs-at-least-32-bytes".getBytes());JwtService service=new JwtService(key,60000);String token=service.generateToken(1L,"user@example.com");assertTrue(service.isTokenValid(token));assertEquals("user@example.com",service.extractEmail(token));}}
