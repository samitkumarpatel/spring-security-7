package net.samitkumar.auth_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableWebSecurity
public class AuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServerApplication.class, args);
	}
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	RouterFunction<ServerResponse> routerFunction(JdbcUserDetailsManager jdbcUserDetailsManager) {
		return RouterFunctions
				.route()
				.GET("/ping", request -> ServerResponse.ok().body(Map.of("message", "pong")))
				.GET("/secure", request -> ServerResponse.ok().body(Map.of("message", "Hello world")))
				.POST("/register", request -> {
					var u = request.body(User.class);
					UserDetails user = org.springframework.security.core.userdetails.User.builder()
							.username(u.username())
							.password(passwordEncoder().encode(u.password()))
							.roles("USER")
							.build();
					jdbcUserDetailsManager.createUser(user);
					return ServerResponse.ok().build();
				})
				.build();
	}

	@Bean
	JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
		return new JdbcUserDetailsManager(dataSource);
	}

	//curl -i -u samit:samit123 http://localhost:9000/ping

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.oauth2AuthorizationServer(authorizationServer -> {
					http.securityMatcher(authorizationServer.getEndpointsMatcher());
					authorizationServer.oidc(Customizer.withDefaults());
				})
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint("/login"),
								new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
						)
				)
				.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain web(HttpSecurity http) throws Exception {
		return http
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/ping", "/register").permitAll()
						.anyRequest().authenticated()
				)
				.formLogin(Customizer.withDefaults())
				.oauth2Login(Customizer.withDefaults())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/register"))
				.oneTimeTokenLogin(ott -> ott
						.tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
							String ottUrl = "/login/ott?token=" + oneTimeToken.getTokenValue();
							IO.println("### OTT link: http://localhost:" + request.getServerPort() + ottUrl);
							response.sendRedirect(ottUrl);
						})
				)
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		var config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}

record User(String username, String password) {}