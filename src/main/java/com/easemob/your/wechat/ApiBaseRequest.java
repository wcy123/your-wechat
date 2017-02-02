package com.easemob.your.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiBaseRequest {
    @JsonProperty("BaseRequest")
    BaseRequest baseRequest;

    @Data
    @Builder
    public static class BaseRequest {
        @JsonProperty("Skey")
        String skey;
        @JsonProperty("Sid")
        String wxsid;
        @JsonProperty("Uin")
        String wxuin;
        @JsonProperty("DeviceID")
        String deviceId;
    }
}
