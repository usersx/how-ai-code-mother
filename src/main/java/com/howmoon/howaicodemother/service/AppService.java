package com.howmoon.howaicodemother.service;

import com.howmoon.howaicodemother.model.dto.app.AppAddRequest;
import com.howmoon.howaicodemother.model.dto.app.AppQueryRequest;
import com.howmoon.howaicodemother.model.entity.App;
import com.howmoon.howaicodemother.model.entity.User;
import com.howmoon.howaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/usersx">howmoon</a>
 */
public interface AppService extends IService<App> {

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 通过对话生成应用代码
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return
     */
    String deployApp(Long appId,User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造应用查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用封装列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    }
