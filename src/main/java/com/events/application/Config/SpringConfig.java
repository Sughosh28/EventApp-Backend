package com.events.application.Config;

import com.events.application.Jwt.EventUserDetailService;
import com.events.application.Jwt.JwtAuthenticationFilter;
import com.events.application.Model.UserEntity;
import com.events.application.Repository.UserRepository;
import com.events.application.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;


@Configuration
public class SpringConfig implements WebMvcConfigurer {
    @Autowired
    private EventUserDetailService eventUserDetailService;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "*")
                .allowCredentials(true);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http

                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(registry->{
                    registry.requestMatchers("/","/register","/v3/api-docs/**","/swagger-ui/**", "/login", "/loginUser" ).permitAll();
                    registry.requestMatchers("/api/events/**").hasRole("ORGANIZER");
                    registry.requestMatchers("/api/bookEvents/**").hasRole("USER");

                    registry.requestMatchers("/api/users/**").hasRole("USER");
                    registry.anyRequest().authenticated();
                      })
                .oauth2Login(oauth ->
                        oauth
                                .loginPage("/login")
                                .successHandler((request, response, authentication) -> response.sendRedirect("/profile"))
                                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService()))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
          }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return userRequest -> {
            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");

            UserEntity user = userRepository.findByEmail(email);
            if (user == null) {
                user = new UserEntity();
                user.setEmail(email);
                user.setUsername(name);
                user.setRole("USER");

                System.out.println("Saving user: " + user);
                userRepository.save(user);
            } else {
                System.out.println("User already exists: " + user);
            }
            return oAuth2User;
        };
    }


    @Bean
    public UserDetailsService userDetailsService(){
        return  eventUserDetailService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(eventUserDetailService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
