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
                // 1. 세션 비활성화 (토큰 기반 인증)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. CORS 설정 (Lambda DSL 방식)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))

                // 3. CSRF, HTTP Basic, Form Login 비활성화 (Lambda DSL 방식)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 4. JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 5. 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**", "/").permitAll()  // OAuth 관련만 허용
                        .requestMatchers("/api/user/me", "/api/likes/**", "/api/search/books/*/select", "/api/books/**/playlist").authenticated() // 인증 필요한 API
                        .requestMatchers("/private/**").authenticated()
                        .anyRequest().permitAll()
                )

                // 6. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler) // 로그인 성공 시 핸들러 연결 (토큰 반환)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // 사용자 정보 처리 서비스 연결
                        )
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 모든 origin 허용 (프론트엔드 연결을 위해)
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}