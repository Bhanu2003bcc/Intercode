package com.interview.platform.security;

import com.interview.platform.repository.UserRepository;
import com.interview.platform.enums.Role;
import com.interview.platform.models.User;
import com.interview.platform.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        return processOAuth2User(registrationId, oAuth2User);
    }

    private OAuth2User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.getOrDefault("name", email);
        String providerId = attributes.get("sub") != null
            ? attributes.get("sub").toString()
            : attributes.get("id").toString();
        String avatarUrl = (String) attributes.getOrDefault("picture", null);

        Provider provider = Provider.GOOGLE;

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setFullName(name);
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(Instant.now());
        } else {
            user = User.builder()
                .email(email)
                .fullName(name)
                .avatarUrl(avatarUrl)
                .role(Role.CANDIDATE)
                .provider(provider)
                .providerId(providerId)
                .isActive(true)
                .build();
        }

        userRepository.save(user);
        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
