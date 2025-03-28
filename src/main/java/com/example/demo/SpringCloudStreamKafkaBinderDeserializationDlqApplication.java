package com.example.demo;

import java.util.function.Consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;

@SpringBootApplication
public class SpringCloudStreamKafkaBinderDeserializationDlqApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaBinderDeserializationDlqApplication.class, args);
	}

	@Bean
	Consumer<Event> functionName() {
		return s -> {
			if (s.message().contains("w")) {
				throw new RuntimeException();
			}

			System.out.println(s);
		};
	}

	@Bean
	DefaultErrorHandler errorHandler() {
		return new DefaultErrorHandler((consumerRecord, exception) -> {
			System.out.println(consumerRecord.topic() + consumerRecord.partition() + consumerRecord.offset());
		});
	}

	@Bean
	ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(DefaultErrorHandler errorHandler) {
		return (container, dest, group) -> container.setCommonErrorHandler(errorHandler);
	}
}
