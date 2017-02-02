package com.easemob.your.wechat;

/**
 * Created by wangchunye on 2/2/17.
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.wcy123.ProtobufMessageConverter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class ProtobufFieldDeserializer<T extends Message> extends JsonDeserializer<T> {
    private static JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
    private static final ConcurrentHashMap<Class<?>, Method> methodCache =
            new ConcurrentHashMap<Class<?>, Method>();
    private final Class<T> clazz;

    public ProtobufFieldDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        // http://stackoverflow.com/questions/3437897/how-to-get-a-class-instance-of-generics-type-t
        final Message.Builder messageBuilder;
        try {
            messageBuilder = getMessageBuilder(clazz);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
        parser.merge(jsonParser.readValueAs(ObjectNode.class).toString(),
                messageBuilder);
        return (T) messageBuilder.build();
    }
    private Message.Builder getMessageBuilder(Class<? extends Message> clazz) throws Exception {
        Method method = methodCache.get(clazz);
        if (method == null) {
            method = clazz.getMethod("newBuilder");
            methodCache.put(clazz, method);
        }
        return (Message.Builder) method.invoke(clazz);
    }

}
