package com.mrzhang.mieaicodemain.service;

import com.mrzhang.mieaicodemain.model.dto.app.AppQueryRequest;
import com.mrzhang.mieaicodemain.model.entity.User;
import com.mrzhang.mieaicodemain.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mrzhang.mieaicodemain.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用服务层。
 *
 * @author <a href="https://github.com/zhangxyl">程序员小阳</a>
 */
public interface AppService extends IService<App> {

    /**
     * 校验应用
     *
     * @param app 应用
     * @param add 是否为创建校验
     */
    void validApp(App app, boolean add);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用封装
     *
     * @param app 应用
     * @return 应用封装
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装列表
     *
     * @param appList 应用列表
     * @return 应用封装列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    QueryWrapper getGoodAppQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 通过对话生成应用代码
     *
     * @param appId     应用 ID
     * @param message   提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);
}
