package net.samitkumar.resource_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

@SpringBootApplication
public class ResourceServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceServerApplication.class, args);
	}

	@Bean
	JsonPlaceHolderClient jsonPlaceHolderClient(RestClient.Builder restClientBuilder) {
		HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
				.exchangeAdapter(RestClientAdapter.create(restClientBuilder.build()))
				.build();
		return proxyFactory.createClient(JsonPlaceHolderClient.class);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/user", config);
		return source;
	}

	@Bean
	RouterFunction<ServerResponse> routerFunction(JsonPlaceHolderClient jsonPlaceHolderClient) {
		return RouterFunctions
				.route()
				.GET("/api/user", request -> ServerResponse.ok().body(jsonPlaceHolderClient.allUser()))
				.build();
	}
}

record User(String id, String name, String username, String phone, String email) {}

@HttpExchange(url = "https://jsonplaceholder.typicode.com")
interface JsonPlaceHolderClient {
	@GetExchange("/users")
	List<User> allUser();
}