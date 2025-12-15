package com.musiccheck.common.config;

import com.musiccheck.common.config.CustomAuthenticationEntryPoint;
import com.musiccheck.common.jwt.JwtAuthenticationFilter;
import com.musiccheck.common.oauth.CustomOAuth2AuthorizationRequestResolver;
import com.musiccheck.common.oauth.CustomOAuth2UserService;
import com.musiccheck.common.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
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
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

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

                // 인증 실패 시 JSON 응답 반환 (기본 HTML 로그인 페이지 대신)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**", "/").permitAll()  // OAuth 관련만 허용 (구글은 /login/oauth2/code/google로 처리)
                        .requestMatchers("/api/spotify/callback").permitAll()  // 스포티파이 콜백은 인증 불필요 (state로 사용자 식별)
                        .requestMatchers("/api/user/google-token").permitAll()  // 구글 로그인 임시 토큰 조회 (인증 불필요)
                        .requestMatchers("/api/admin/login", "/api/admin/users").permitAll()  // 관리자 로그인/사용자 조회 API (쿼리 파라미터로 인증)
                        .requestMatchers("/api/user/me", "/api/user", "/api/likes", "/api/likes/disliked", "/api/likes/**", "/api/search/books/*/select", "/api/search/recent", "/api/books/*/playlist", "/api/spotify/connect", "/api/spotify/create-playlist", "/api/admin/**").authenticated() // 인증 필요한 API
                        .requestMatchers("/private/**").authenticated()
                        .anyRequest().permitAll()
                )

                // OAuth2 로그인 설정 (카카오/네이버/구글)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")  // 명시적으로 baseUri 설정
                                .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver())
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")  // 콜백 URI 패턴 명시
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                );

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver() {
        // 기본 리졸버를 래핑하여 구글 로그인 시 prompt=select_account 추가
        OAuth2AuthorizationRequestResolver defaultResolver = 
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );
        return new CustomOAuth2AuthorizationRequestResolver(defaultResolver);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList("Content-Type", "Authorization", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
