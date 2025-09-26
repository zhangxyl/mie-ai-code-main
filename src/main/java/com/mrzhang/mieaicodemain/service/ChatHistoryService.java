package com.mrzhang.mieaicodemain.service;

import com.mrzhang.mieaicodemain.model.dto.chathistory.ChatHistoryQueryRequest;
import com.mrzhang.mieaicodemain.model.entity.User;
import com.mrzhang.mieaicodemain.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mrzhang.mieaicodemain.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史服务层
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 校验对话历史
     *
     * @param chatHistory 对话历史
     * @param add         是否为创建校验
     */
    void validChatHistory(ChatHistory chatHistory, boolean add);

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 获取对话历史封装
     *
     * @param chatHistory 对话历史
     * @return 对话历史封装
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 获取对话历史封装列表
     *
     * @param chatHistoryList 对话历史列表
     * @return 对话历史封装列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 保存用户消息
     *
     * @param appId   应用ID
     * @param message 消息内容
     * @param user    用户
     * @return 对话历史ID
     */
    Long saveUserMessage(Long appId, String message, User user);

    /**
     * 保存AI消息
     *
     * @param appId   应用ID
     * @param message 消息内容
     * @param user    用户
     * @return 对话历史ID
     */
    Long saveAiMessage(Long appId, String message, User user);

    /**
     * 保存错误消息
     *
     * @param appId    应用ID
     * @param errorMsg 错误消息
     * @param user     用户
     * @return 对话历史ID
     */
    Long saveErrorMessage(Long appId, String errorMsg, User user);

    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID删除对话历史
     *
     * @param appId 应用ID
     * @return 删除结果
     */
    boolean removeByAppId(Long appId);

    /**
     * 获取应用的最新对话历史
     *
     * @param appId 应用ID
     * @param limit 限制数量
     * @return 对话历史列表
     */
    List<ChatHistoryVO> getLatestChatHistory(Long appId, int limit);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 加载对话历史到内存
     *
     * @param appId
     * @param chatMemory
     * @param maxCount   最多加载多少条
     * @return 加载成功的条数
     */
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
