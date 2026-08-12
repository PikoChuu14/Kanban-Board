package com.company.kanban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> {})

                .formLogin(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        PathPatternRequestMatcher
                                                .withDefaults()
                                                .matcher("/api/auth/**")
                                ).permitAll()

                                // Temporary while building JWT
                                .requestMatchers(
                                        PathPatternRequestMatcher
                                                .withDefaults()
                                                .matcher("/api/**")
                                ).permitAll()

                                .requestMatchers(
                                        PathPatternRequestMatcher
                                                .withDefaults()
                                                .matcher("/error")
                                ).permitAll()

                                .anyRequest().authenticated()
                );

        return http.build();
    }
}