package com.example.demo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class SpringCloudStreamKafkaBinderDeserializationDlqApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaBinderDeserializationDlqApplication.class, args);
	}

	@Bean
	Function<Message<List<Event>>, List<Message<Event>>> functionName() {
		return str -> {
			System.out.println("Start");

			str.getPayload().forEach(s -> {
				if (s.message().contains("w")) {
					throw new RuntimeException();
				}

				System.out.println(s);
			});

			System.out.println("End");

			return str.getPayload()
					.stream()
					.map(event -> MessageBuilder.withPayload(event).build())
					.toList();
		};
	}

	@Bean
	ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(
			BindingServiceProperties bindingServiceProperties,
			KafkaTemplate<?, ?> kafkaTemplate) {
		return (container, destinationName, group) -> {
			// Retrieve DLQ destination from config defined binding
			var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
					(consumerRecord, e) -> new TopicPartition("dlq-topic", 0));

			// Programmatically define a backoff to avoid a bug where Spring Cloud Stream
			// retries even when a non-code config is defined
			var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(0, 1));

			container.setCommonErrorHandler(errorHandler);
			container.setBatchInterceptor(new NullValueFilterBatchInterceptor<>());
		};
	}

	static class NullValueFilterBatchInterceptor<K, V> implements BatchInterceptor<K, V> {

		@Override
		@Nullable
		public ConsumerRecords<K, V> intercept(ConsumerRecords<K, V> records,
				org.apache.kafka.clients.consumer.Consumer<K, V> consumer) {
			return records.partitions().stream()
					.map(partition -> StreamSupport.stream(records.spliterator(), false)
							.filter(consumerRecord -> consumerRecord.partition() == partition.partition())
							.filter(consumerRecord -> {
								var hasValue = consumerRecord.value() != null;

								if(!hasValue) {
									System.out.println("Dropping");
								}

								return hasValue;
							})
							.collect(Collectors.collectingAndThen(
									Collectors.toList(),
									list -> Map.entry(partition, list))))
					.collect(Collectors.collectingAndThen(
							Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
							ConsumerRecords::new));
		}

	}
}
