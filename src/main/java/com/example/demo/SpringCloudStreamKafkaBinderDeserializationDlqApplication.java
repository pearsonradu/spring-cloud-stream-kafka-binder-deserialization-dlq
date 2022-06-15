package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.function.Consumer;

@SpringBootApplication
public class SpringCloudStreamKafkaBinderDeserializationDlqApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaBinderDeserializationDlqApplication.class, args);
	}

	@Bean
	public Consumer<Double> functionName() {
		return s -> {
			System.out.println(s);
		};
	}

	@Bean
	public DeadLetterPublishingRecoverer publisher(KafkaOperations template) {
		return new DeadLetterPublishingRecoverer(template);
	}

	@Bean
	public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
		return new DefaultErrorHandler(deadLetterPublishingRecoverer);
	}

	@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(DefaultErrorHandler errorHandler) {
		return (container, dest, group) -> {
			container.setCommonErrorHandler(errorHandler);
		};
	}
}
