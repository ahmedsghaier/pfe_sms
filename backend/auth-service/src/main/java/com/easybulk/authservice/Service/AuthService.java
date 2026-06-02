package com.easybulk.authservice.Service;

import com.easybulk.authservice.DTO.LoginRequest;
import com.easybulk.authservice.DTO.LoginResponse;
import com.easybulk.authservice.model.Organization;
import com.easybulk.authservice.model.User;
import com.easybulk.authservice.model.UserOrganizationRole;
import com.easybulk.authservice.Repository.OrganizationRepository;
import com.easybulk.authservice.Repository.UserOrganizationRoleRepository;
import com.easybulk.authservice.Repository.UserRepository;
import com.easybulk.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@SpringBootApplication(scanBasePackages = "com.easybulk")
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Générer les tokens
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Récupérer les organisations de l'utilisateur
        List<UserOrganizationRole> userRoles = userOrgRoleRepository.findByUserId(user.getId());

        List<LoginResponse.OrganizationInfo> organizations = userRoles.stream()
                .map(role -> {
                    Organization org = organizationRepository.findById(role.getOrganizationId())
                            .orElse(null);

                    return LoginResponse.OrganizationInfo.builder()
                            .id(role.getOrganizationId())
                            .name(org != null ? org.getName() : "Unknown")
                            .role(role.getRole().name())
                            .groupId(role.getGroupId())
                            .build();
                })
                .collect(Collectors.toList());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organizations(organizations)
                .build();

        log.info("Login successful for user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }
}