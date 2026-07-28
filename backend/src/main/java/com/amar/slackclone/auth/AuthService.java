package com.amar.slackclone.auth;

import com.amar.slackclone.auth.dto.RegisterRequest;
import com.amar.slackclone.auth.dto.UserResponse;
import com.amar.slackclone.user.User;
import com.amar.slackclone.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Instant now = Instant.now();

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                now,
                now
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                savedUser.getCreatedAt()
        );
    }
}