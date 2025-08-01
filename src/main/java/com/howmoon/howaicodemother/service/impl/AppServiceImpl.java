package com.howmoon.howaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.howmoon.howaicodemother.model.entity.App;
import com.howmoon.howaicodemother.mapper.AppMapper;
import com.howmoon.howaicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/usersx">howmoon</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
