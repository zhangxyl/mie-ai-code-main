package com.mrzhang.mieaicodemain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mrzhang.mieaicodemain.constant.ChatHistoryConstant;
import com.mrzhang.mieaicodemain.constant.UserConstant;
import com.mrzhang.mieaicodemain.exception.BusinessException;
import com.mrzhang.mieaicodemain.exception.ErrorCode;
import com.mrzhang.mieaicodemain.exception.ThrowUtils;
import com.mrzhang.mieaicodemain.model.dto.chathistory.ChatHistoryQueryRequest;
import com.mrzhang.mieaicodemain.model.entity.App;
import com.mrzhang.mieaicodemain.model.entity.User;
import com.mrzhang.mieaicodemain.model.enums.ChatHistoryMessageTypeEnum;
import com.mrzhang.mieaicodemain.model.vo.AppVO;
import com.mrzhang.mieaicodemain.model.vo.ChatHistoryVO;
import com.mrzhang.mieaicodemain.model.vo.UserVO;
import com.mrzhang.mieaicodemain.service.AppService;
import com.mrzhang.mieaicodemain.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mrzhang.mieaicodemain.model.entity.ChatHistory;
import com.mrzhang.mieaicodemain.mapper.ChatHistoryMapper;
import com.mrzhang.mieaicodemain.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话历史服务层实现
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public void validChatHistory(ChatHistory chatHistory, boolean add) {
        if (chatHistory == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String message = chatHistory.getMessage();
        String messageType = chatHistory.getMessageType();
        Long appId = chatHistory.getAppId();

        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }
        // 有参数则校验
        if (StrUtil.isNotBlank(message) && message.length() > ChatHistoryConstant.MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容过长");
        }
        if (StrUtil.isNotBlank(messageType)) {
            ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
            ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型不合法");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        if(lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序
            queryWrapper.orderBy("createTime", false);
        }

        return queryWrapper;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);

        // 填充用户信息
        Long userId = chatHistory.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            chatHistoryVO.setUser(userVO);
        }

        // 填充应用信息
        Long appId = chatHistory.getAppId();
        if (appId != null && appId > 0) {
            App app = appService.getById(appId);
            AppVO appVO = appService.getAppVO(app);
            chatHistoryVO.setApp(appVO);
        }

        return chatHistoryVO;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }

        // 批量获取用户信息
        Set<Long> userIdSet = chatHistoryList.stream()
                .map(ChatHistory::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 批量获取应用信息
        Set<Long> appIdSet = chatHistoryList.stream()
                .map(ChatHistory::getAppId)
                .collect(Collectors.toSet());
        Map<Long, List<App>> appIdAppListMap = appService.listByIds(appIdSet).stream()
                .collect(Collectors.groupingBy(App::getId));

        // 填充信息
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryList.stream().map(chatHistory -> {
            ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
            BeanUtil.copyProperties(chatHistory, chatHistoryVO);

            Long userId = chatHistory.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            chatHistoryVO.setUser(userService.getUserVO(user));

            Long appId = chatHistory.getAppId();
            App app = null;
            if (appIdAppListMap.containsKey(appId)) {
                app = appIdAppListMap.get(appId).get(0);
            }
            chatHistoryVO.setApp(appService.getAppVO(app));

            return chatHistoryVO;
        }).collect(Collectors.toList());

        return chatHistoryVOList;
    }

    @Override
    public Long saveUserMessage(Long appId, String message, User user) {
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(ChatHistoryMessageTypeEnum.USER.getValue())
                .appId(appId)
                .userId(user.getId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        validChatHistory(chatHistory, true);
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存用户消息失败");
        return chatHistory.getId();
    }

    @Override
    public Long saveAiMessage(Long appId, String message, User user) {
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                .appId(appId)
                .userId(user.getId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        validChatHistory(chatHistory, true);
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存AI消息失败");
        return chatHistory.getId();
    }

    @Override
    public Long saveErrorMessage(Long appId, String errorMsg, User user) {
        ChatHistory chatHistory = ChatHistory.builder()
                .message(errorMsg)
                .messageType(ChatHistoryMessageTypeEnum.ERROR.getValue())
                .appId(appId)
                .userId(user.getId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        validChatHistory(chatHistory, true);
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存错误消息失败");
        return chatHistory.getId();
    }
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {

        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageTypeEnum.getValue())
                .appId(appId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        validChatHistory(chatHistory, true);
        return this.save(chatHistory);
    }

    @Override
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public List<ChatHistoryVO> getLatestChatHistory(Long appId, int limit) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(limit <= 0 || limit > ChatHistoryConstant.MAX_LIMIT, ErrorCode.PARAMS_ERROR, "查询数量限制在1-" + ChatHistoryConstant.MAX_LIMIT + "之间");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false)
                .limit(limit);

        List<ChatHistory> chatHistoryList = this.list(queryWrapper);
        return getChatHistoryVOList(chatHistoryList);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}