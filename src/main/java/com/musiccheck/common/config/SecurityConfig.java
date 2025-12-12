package com.musiccheck.common.config;

import com.musiccheck.common.jwt.JwtAuthenticationFilter;
import com.musiccheck.common.oauth.CustomOAuth2UserService;
import com.musiccheck.common.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // JWT 기반이므로 세션 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 사용하지 않는 보안 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
<<<<<<< HEAD
                        .requestMatchers("/oauth2/**", "/login/**", "/").permitAll() // 카카오/구글/네이버 OAuth
                        .requestMatchers("/api/spotify/callback").permitAll()       // Spotify 콜백은 인증 안함
                        .requestMatchers("/api/spotify/connect").authenticated()     // Spotify 연결은 로그인 후만 가능
                        .requestMatchers("/api/user/me", "/api/likes/**").authenticated()
=======
                        .requestMatchers("/oauth2/**", "/login/**", "/").permitAll()  // OAuth 관련만 허용
                        .requestMatchers("/api/spotify/callback").permitAll()  // 스포티파이 콜백은 인증 불필요 (state로 사용자 식별)
                        .requestMatchers("/api/user/me", "/api/user", "/api/likes", "/api/likes/disliked", "/api/likes/**", "/api/search/books/*/select", "/api/books/*/playlist", "/api/spotify/connect").authenticated() // 인증 필요한 API
                        .requestMatchers("/private/**").authenticated()
>>>>>>> 049777e96192a8bcd0d98c40670bdd35d4772940
                        .anyRequest().permitAll()
                )

                // OAuth2 로그인 설정 (카카오/네이버/구글)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
