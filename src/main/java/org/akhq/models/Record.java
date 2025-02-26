package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.utils.AvroToJsonSerializer;
import org.akhq.utils.PickleDeserializer;
import org.akhq.utils.ProtobufToJsonDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Record {
    private String topic;
    private int partition;
    private long offset;
    private ZonedDateTime timestamp;
    @JsonIgnore
    private TimestampType timestampType;
    private Integer keySchemaId;
    private Integer valueSchemaId;
    private Map<String, String> headers = new HashMap<>();
    @JsonIgnore
    private Deserializer kafkaAvroDeserializer;
    private ProtobufToJsonDeserializer protobufToJsonDeserializer;

    @Getter(AccessLevel.NONE)
    private byte[] bytesKey;

    @Getter(AccessLevel.NONE)
    private String key;

    @Getter(AccessLevel.NONE)
    private byte[] bytesValue;

    @Getter(AccessLevel.NONE)
    private String value;

    private final List<String> exceptions = new ArrayList<>();

    private byte MAGIC_BYTE;

    public Record(RecordMetadata record, SchemaRegistryType schemaRegistryType, byte[] bytesKey, byte[] bytesValue, Map<String, String> headers) {
        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            this.MAGIC_BYTE = (byte) 0x80;
        } else {
            this.MAGIC_BYTE = 0x0;
        }
        this.topic = record.topic();
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault());
        this.bytesKey = bytesKey;
        this.keySchemaId = getAvroSchemaId(this.bytesKey);
        this.bytesValue = bytesValue;
        this.valueSchemaId = getAvroSchemaId(this.bytesValue);
        this.headers = headers;
    }

    public Record(ConsumerRecord<byte[], byte[]> record, SchemaRegistryType schemaRegistryType, Deserializer kafkaAvroDeserializer,
                  ProtobufToJsonDeserializer protobufToJsonDeserializer, byte[] bytesValue) {
        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            this.MAGIC_BYTE = (byte) 0x80;
        } else {
            this.MAGIC_BYTE = 0x0;
        }
        this.topic = record.topic();
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault());
        this.timestampType = record.timestampType();
        this.bytesKey = record.key();
        this.keySchemaId = getAvroSchemaId(this.bytesKey);
        this.bytesValue = bytesValue;
        this.valueSchemaId = getAvroSchemaId(this.bytesValue);
        for (Header header: record.headers()) {
            this.headers.put(header.key(), header.value() != null ? new String(header.value()) : null);
        }

        this.kafkaAvroDeserializer = kafkaAvroDeserializer;
        this.protobufToJsonDeserializer = protobufToJsonDeserializer;
    }

    public String getKey() {
        if (this.key == null) {
            this.key = convertToString(bytesKey, keySchemaId, true);
        }

        return this.key;
    }

    @JsonIgnore
    public String getKeyAsBase64() {
        if (bytesKey == null) {
            return null;
        } else {
            return new String(Base64.getEncoder().encode(bytesKey));
        }
    }

    public String getValue() {
        if (this.value == null) {
            this.value = convertToString(bytesValue, valueSchemaId, false);
        }

        return this.value;
    }

    private String convertToString(byte[] payload, Integer schemaId, boolean isKey) {
        if (payload == null) {
            return null;
        } else if (schemaId != null) {
            try {
                Object toType = kafkaAvroDeserializer.deserialize(topic, payload);

                //for primitive avro type
                if (!(toType instanceof GenericRecord)){
                    return String.valueOf(toType);
                }

                GenericRecord record = (GenericRecord) toType;
                return AvroToJsonSerializer.toJson(record);
            } catch (Exception exception) {
                this.exceptions.add(exception.getMessage());

                return new String(payload);
            }
        } else {
            if (protobufToJsonDeserializer != null) {
                try {
                    String record = protobufToJsonDeserializer.deserialize(topic, payload, isKey);
                    if (record != null) {
                        return record;
                    }
                } catch (Exception exception) {
                    this.exceptions.add(exception.getMessage());

                    return new String(payload);
                }
            }
            return PickleDeserializer.getInstance().deserialize(payload);
//            return Base64.getEncoder().encodeToString(payload);
//            return new String(payload);
        }
    }

    private Integer getAvroSchemaId(byte[] payload) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte magicBytes = buffer.get();
            int schemaId = buffer.getInt();

            if (magicBytes == MAGIC_BYTE && schemaId >= 0) {
                return schemaId;
            }
        } catch (Exception ignore) {

        }
        return null;
    }
}
