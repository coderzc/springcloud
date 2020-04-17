package com.zc.producer;

import com.zc.dal.plugin.encryption.utils.SpringBeanUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class KafkaProducer {
    @Autowired
    @Qualifier(value = "defaultKafkaTemplate")
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    class ProducerCallback implements ListenableFutureCallback<SendResult<String, Object>> {

        private final long startTime;
        private final String key;

        public ProducerCallback(long startTime, String key) {
            this.startTime = startTime;
            this.key = key;
        }

        @Override
        public void onFailure(Throwable throwable) {
            logger.error(throwable.getMessage());
        }

        @Override
        public void onSuccess(SendResult<String, Object> result) {
            if (result == null) {
                return;
            }
            long elapsedTime = System.currentTimeMillis() - startTime;

            RecordMetadata metadata = result.getRecordMetadata();
            if (metadata != null) {
                StringBuilder record = new StringBuilder();
                record.append("message(")
                        .append("key = ").append(key).append(",")
                        .append("sent to partition(").append(metadata.partition()).append(")")
                        .append("with offset(").append(metadata.offset()).append(")")
                        .append("in ").append(elapsedTime).append(" ms");
                logger.info(record.toString());
            }
        }
    }


    public void send(String topic, Object message) {
        kafkaTemplate.send(topic, message);
    }

    public void send(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
    }

    public void asyncSend(String topic, String key, Object message) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topic,
                key,
                message);

        long startTime = System.currentTimeMillis();

        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        future.addCallback(new ProducerCallback(startTime, key));
    }
}


