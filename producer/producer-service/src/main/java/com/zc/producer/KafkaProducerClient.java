package com.zc.producer;

import com.zc.api.model.MessageEntity;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by zhaocong on 2020-03-12
 */
@Service
public class KafkaProducerClient {


    private KafkaTemplate<String, MessageEntity> kafkaTemplate = null;

    @Value("${kafka.producer.servers}")
    private String servers;
    @Value("${kafka.producer.retries}")
    private int retries;
    @Value("${kafka.producer.batch.size}")
    private int batchSize;
    @Value("${kafka.producer.linger}")
    private int linger;
    @Value("${kafka.producer.buffer.memory}")
    private int bufferMemory;

    @PostConstruct
    public void init() {
        kafkaTemplate = new KafkaTemplate<>(producerFactory());
    }


    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "-1");
        return props;
    }

    public ProducerFactory<String, MessageEntity> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs(),
                new StringSerializer(),
                new JsonSerializer<MessageEntity>());
    }

    public void send(String topic, MessageEntity value) throws ExecutionException, InterruptedException {
        this.send(topic, null, value);
    }

    public void send(String topic, String key, MessageEntity value) throws ExecutionException, InterruptedException {
        ProducerRecord<String, MessageEntity> record = null;
        if (key == null) {
            record = new ProducerRecord<>(topic, value);
        } else {
            record = new ProducerRecord<>(topic, key, value);
        }
        ListenableFuture<SendResult<String, MessageEntity>> future = kafkaTemplate.send(record);

        // 异步发送
        future.addCallback(new ListenableFutureCallback<SendResult<String, MessageEntity>>() {
            @Override
            public void onFailure(Throwable throwable) {
            }

            @Override
            public void onSuccess(SendResult<String, MessageEntity> stringMessageEntitySendResult) {
                System.out.println("async_ack!!!");
            }
        });

        //同步发送
        SendResult<String, MessageEntity> stringMessageEntitySendResult = future.get();
        System.out.println("sync_ack!!!");

    }

}
