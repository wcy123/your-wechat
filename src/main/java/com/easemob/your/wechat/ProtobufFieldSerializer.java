package com.easemob.your.wechat;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ProtobufFieldSerializer<T extends Message> extends JsonSerializer<T> {

    private static JsonFormat.Printer printer = JsonFormat.printer();
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(printer.print(value));
    }
}
