package com.cartup.search.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
	
	@Value("${REQUEST_TIMEOUT}")
	private long requestTimeout;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
				.setConnectTimeout(Duration.ofMillis(requestTimeout))
				.setReadTimeout(Duration.ofMillis(requestTimeout))
				.build();
	}
	
	@Bean
	public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory){
		RedisTemplate<String,String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new GenericToStringSerializer<>(String.class));
		redisTemplate.setValueSerializer(new GenericToStringSerializer<>(String.class));
		return redisTemplate;
	}

}
