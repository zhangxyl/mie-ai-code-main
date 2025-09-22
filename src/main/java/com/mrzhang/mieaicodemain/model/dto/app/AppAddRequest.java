package com.mrzhang.mieaicodemain.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;


    private static final long serialVersionUID = 1L;
}