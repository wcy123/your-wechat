package com.easemob.your.wechat;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@XmlRootElement(name = "error")
@Data
public class ApiBaseResponse {
    Integer ret;
    String message;
    String skey;
    String wxsid;
    String wxuin;
    String pass_ticket;
}
