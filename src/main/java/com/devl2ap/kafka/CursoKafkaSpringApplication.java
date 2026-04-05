package com.devl2ap.kafka;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@SpringBootApplication
public class CursoKafkaSpringApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(CursoKafkaSpringApplication.class);
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@KafkaListener(topics = "l2ap-topic", groupId = "l2ap-group", containerFactory = "kafkaListenerContainerFactory", properties = {"max.poll.interval.ms=4000","max.poll.records=10"})
	public void listen(List<ConsumerRecord<String, String>> messages) {
		logger.warn("Start reading BATCH");
		for(ConsumerRecord<String, String> message : messages) {
			logger.warn("Partition: {}, Offset: {}, Key: {}, Value: {}", message.partition(), message.offset(), message.key(), message.value());
		}
		logger.warn("Finished processing BATCH complete");
	}
	public static void main(String[] args) {
		SpringApplication.run(CursoKafkaSpringApplication.class, args);
	}
	/*SYNC*/
	@Override
	public void run(String... args) throws Exception {
		for(int i=0; i<100; i++) {
			kafkaTemplate.send("l2ap-topic", String.valueOf(i),String.format("Sample Message %d", i));
		}
	}

	/*ASYNC*/
	/*
	@Override
	public void run(String... args) throws Exception {
		// 1. Cambia el tipo de retorno a CompletableFuture
		CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("l2ap-topic", "Sample Message Kafka L2AP");

		// 2. Usa whenComplete en lugar de addCallback
		future.whenComplete((result, ex) -> {
			if (ex == null) {
				// Caso de Éxito (Success)
				logger.warn("Message sent successfully: {}", result.getRecordMetadata().offset());
			} else {
				// Caso de Error (Failure)
				logger.error("Failed to send message: {}", ex.getMessage());
			}
		});

	}
	*/
}
