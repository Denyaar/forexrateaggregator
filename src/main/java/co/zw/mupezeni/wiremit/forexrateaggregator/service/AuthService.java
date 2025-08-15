/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 8:02 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.service;

import co.zw.mupezeni.wiremit.forexrateaggregator.dto.AuthDTOs;
import co.zw.mupezeni.wiremit.forexrateaggregator.enums.Role;
import co.zw.mupezeni.wiremit.forexrateaggregator.model.User;
import co.zw.mupezeni.wiremit.forexrateaggregator.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register new user
     */
    public AuthDTOs.AuthResponse signup(AuthDTOs.SignupRequest request) {
        log.info("Attempting to register new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {}", savedUser.getUsername());

        // Generate JWT token
        String token = jwtService.generateToken(savedUser);

        return AuthDTOs.AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userInfo(mapToUserInfo(savedUser))
                .build();
    }

    /**
     * Authenticate user login
     */
    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));

            // Update last login
            userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());

            // Generate JWT token
            String token = jwtService.generateToken(userDetails);

            log.info("Successfully authenticated user: {}", user.getUsername());

            return AuthDTOs.AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .userInfo(mapToUserInfo(user))
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }


    private AuthDTOs.UserInfo mapToUserInfo(User user) {
        return AuthDTOs.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}