package com.capitan.chatapp.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.capitan.chatapp.dto.LoginResponseDto;
import com.capitan.chatapp.dto.LoginDto;
import com.capitan.chatapp.dto.RegisterDto;

import com.capitan.chatapp.models.Role;
import com.capitan.chatapp.models.UserEntity;
import com.capitan.chatapp.repository.RoleRepository;
import com.capitan.chatapp.repository.UserRepository;
import com.capitan.chatapp.security.JwtGenerator;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtGenerator jwtGenerator;
    private AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, JwtGenerator jwtGenerator, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtGenerator = jwtGenerator;
        this.authenticationManager = authenticationManager;
    }

    public ResponseEntity<?> login(@RequestBody LoginDto loginDto, HttpServletResponse response) {

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Optional<UserEntity> user = userRepository.findByUsername(loginDto.getUsername());
            if (user.isPresent()) {
                Cookie jwtCookie = jwtGenerator.generateCookie(authentication);
                response.addCookie(jwtCookie);
                // HttpHeaders headers = new HttpHeaders();
                // headers.add("Set-Cookie", jwtCookie.toString());
                UserEntity currentUser = user.get();
                LoginResponseDto loginResponseDto = new LoginResponseDto(currentUser.getNickname(),
                        currentUser.getProfileImg(), currentUser.isFirstLogin());
                return new ResponseEntity<>(loginResponseDto, HttpStatus.OK);
            } else
                return new ResponseEntity<>("User wasn't found", HttpStatus.NOT_FOUND);

        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    public ResponseEntity<String> register(RegisterDto registerDto) {
        try {
            if (userRepository.existsByUsername((registerDto.getUsername()))) {
                return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
            }

            if (registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
                UserEntity user = new UserEntity();
                user.setUsername(registerDto.getUsername());
                user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
                user.setProfileImg(
                        registerDto.getProfileImg());
                user.setNickname(user.generateEncodedNickname(registerDto.getNickname()));
                user.setFirstLogin(true);

                Role roles = roleRepository.findByName("USER").get();
                user.setRoles(Collections.singletonList(roles));
                userRepository.save(user);

                return new ResponseEntity<>("User registered successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Password and confirm password aren't equal", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<List<UserEntity>> searchUsersByNicknamePrefix(String prefix,
            HttpServletRequest request) {
        List<UserEntity> foundUsers = userRepository.findByNicknamePrefix(prefix);
        return new ResponseEntity<>(foundUsers, HttpStatus.OK);

    }

    public String getProfileImageByUsername(String username) {
        return userRepository.getProfileImageByUsername(username);
    }

    public Optional<UserEntity> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public String getNicknameByUsername(String username) {
        return userRepository.getNicknameByUsername(username);
    }

    public Boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    private String getJWTFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}