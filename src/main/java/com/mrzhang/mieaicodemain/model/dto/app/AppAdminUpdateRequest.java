package com.mrzhang.mieaicodemain.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用管理员更新请求
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

    private static final long serialVersionUID = 1L;
}