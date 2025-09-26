package com.mrzhang.mieaicodemain.model.dto.chathistory;

import lombok.Data;

import java.io.Serializable;

/**
 * 对话历史创建请求
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
@Data
public class ChatHistoryAddRequest implements Serializable {

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai/error）
     */
    private String messageType;

    /**
     * 应用id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}