package com.easemob.your.wechat;

import java.io.IOException;
import java.net.HttpCookie;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by wangchunye on 2/3/17.
 */
public class HttpCookieJsonDeserializer extends JsonDeserializer<HttpCookie> {
    @Override
    public HttpCookie deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        final ObjectNode t1 = jsonParser.readValueAs(ObjectNode.class);
        final HttpCookie httpCookie =
                new HttpCookie(t1.get("name").asText(),
                        t1.get("value").asText());

        final JsonNode comment = t1.get("comment");
        if (comment.isTextual()) {
            httpCookie.setComment(comment.asText());
        }
        final JsonNode domain = t1.get("domain");
        if (comment.isTextual()) {
            httpCookie.setDomain(comment.asText());
        }
        final JsonNode maxAge = t1.get("maxAge");
        if (maxAge.isNumber()) {
            httpCookie.setMaxAge(comment.asLong());
        }
        final JsonNode path = t1.get("path");
        if (path.isTextual()) {
            httpCookie.setPath(path.asText());
        }
        return httpCookie;
    }
}
