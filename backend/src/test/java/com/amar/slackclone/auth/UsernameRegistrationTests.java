package com.amar.slackclone.auth;

import com.amar.slackclone.auth.dto.RegisterRequest;
import com.amar.slackclone.security.JwtService;
import com.amar.slackclone.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsernameRegistrationTests {
    @Test void registrationNormalizesAndRejectsCaseInsensitiveDuplicateUsername(){
        UserRepository users=mock(UserRepository.class);PasswordEncoder passwords=mock(PasswordEncoder.class);AuthService service=new AuthService(users,passwords,mock(JwtService.class));
        when(users.existsByUsernameIgnoreCase("amar.dev")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->service.register(new RegisterRequest("new@example.com","password1","Amar","Amar.Dev")));
        when(users.existsByUsernameIgnoreCase("amar.dev")).thenReturn(false);when(passwords.encode(any())).thenReturn("hash");when(users.save(any())).thenAnswer(i->i.getArgument(0));
        var response=service.register(new RegisterRequest("new@example.com","password1","Amar","Amar.Dev"));
        assertEquals("amar.dev",response.username());
    }
}
