package com.mrzhang.mieaicodemain.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mrzhang.mieaicodemain.annotation.AuthCheck;
import com.mrzhang.mieaicodemain.common.BaseResponse;
import com.mrzhang.mieaicodemain.common.DeleteRequest;
import com.mrzhang.mieaicodemain.common.ResultUtils;
import com.mrzhang.mieaicodemain.constant.ChatHistoryConstant;
import com.mrzhang.mieaicodemain.constant.UserConstant;
import com.mrzhang.mieaicodemain.exception.BusinessException;
import com.mrzhang.mieaicodemain.exception.ErrorCode;
import com.mrzhang.mieaicodemain.exception.ThrowUtils;
import com.mrzhang.mieaicodemain.model.dto.chathistory.ChatHistoryAddRequest;
import com.mrzhang.mieaicodemain.model.dto.chathistory.ChatHistoryQueryRequest;
import com.mrzhang.mieaicodemain.model.entity.App;
import com.mrzhang.mieaicodemain.model.entity.User;
import com.mrzhang.mieaicodemain.model.vo.ChatHistoryVO;
import com.mrzhang.mieaicodemain.service.AppService;
import com.mrzhang.mieaicodemain.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.mrzhang.mieaicodemain.model.entity.ChatHistory;
import com.mrzhang.mieaicodemain.service.ChatHistoryService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史控制层
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * 创建对话历史
     *
     * @param chatHistoryAddRequest 创建对话历史请求
     * @param request               请求
     * @return 对话历史 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChatHistory(@RequestBody ChatHistoryAddRequest chatHistoryAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 构造入库对象
        ChatHistory chatHistory = new ChatHistory();
        BeanUtil.copyProperties(chatHistoryAddRequest, chatHistory);
        chatHistory.setUserId(loginUser.getId());
        // 参数校验
        chatHistoryService.validChatHistory(chatHistory, true);
        // 校验应用是否存在
        Long appId = chatHistory.getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 插入数据库
        boolean result = chatHistoryService.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(chatHistory.getId());
    }

    /**
     * 删除对话历史
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChatHistory(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ChatHistory oldChatHistory = chatHistoryService.getById(id);
        ThrowUtils.throwIf(oldChatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChatHistory.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chatHistoryService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据 id 获取对话历史
     *
     * @param id      对话历史id
     * @param request 请求
     * @return 对话历史
     */
    @GetMapping("/get/vo")
    public BaseResponse<ChatHistoryVO> getChatHistoryVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ChatHistory chatHistory = chatHistoryService.getById(id);
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验权限：仅应用创建者和管理员可查看
        App app = appService.getById(chatHistory.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该对话历史");
        }
        
        return ResultUtils.success(chatHistoryService.getChatHistoryVO(chatHistory));
    }

    /**
     * 根据应用ID获取最新对话历史
     *
     * @param appId   应用ID
     * @param limit   限制数量（默认10条）
     * @param request 请求
     * @return 对话历史列表
     */
    @GetMapping("/latest")
    public BaseResponse<List<ChatHistoryVO>> getLatestChatHistory(@RequestParam Long appId,
                                                                  @RequestParam(defaultValue = "10") Integer limit,
                                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(limit <= 0 || limit > ChatHistoryConstant.MAX_LIMIT, ErrorCode.PARAMS_ERROR, "查询数量限制在1-" + ChatHistoryConstant.MAX_LIMIT + "之间");
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验权限：仅应用创建者和管理员可查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        }
        
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryService.getLatestChatHistory(appId, limit);
        return ResultUtils.success(chatHistoryVOList);
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除对话历史
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistoryByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChatHistory oldChatHistory = chatHistoryService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldChatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        boolean b = chatHistoryService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 管理员分页获取对话历史列表
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史列表
     */
    @PostMapping("/list/page/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryVOByPageAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), 
                chatHistoryService.getQueryWrapper(chatHistoryQueryRequest));
        return ResultUtils.success(getChatHistoryVOPage(chatHistoryPage));
    }

    /**
     * 管理员根据 id 获取对话历史
     *
     * @param id 对话历史id
     * @return 对话历史
     */
    @GetMapping("/get/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ChatHistory> getChatHistoryById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ChatHistory chatHistory = chatHistoryService.getById(id);
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(chatHistory);
    }

    /**
     * 根据应用ID删除对话历史（管理员）
     *
     * @param appId 应用ID
     * @return 删除结果
     */
    @PostMapping("/delete/by-app/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistoryByAppId(@RequestParam Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        boolean result = chatHistoryService.removeByAppId(appId);
        return ResultUtils.success(result);
    }

    /**
     * 封装分页结果
     *
     * @param chatHistoryPage 对话历史分页
     * @return 对话历史VO分页
     */
    private Page<ChatHistoryVO> getChatHistoryVOPage(Page<ChatHistory> chatHistoryPage) {
        List<ChatHistory> chatHistoryList = chatHistoryPage.getRecords();
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(chatHistoryPage.getPageNumber(), chatHistoryPage.getPageSize(), chatHistoryPage.getTotalRow());
        if (chatHistoryList.isEmpty()) {
            return chatHistoryVOPage;
        }
        // 填充信息
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryService.getChatHistoryVOList(chatHistoryList);
        chatHistoryVOPage.setRecords(chatHistoryVOList);
        return chatHistoryVOPage;
    }
}
