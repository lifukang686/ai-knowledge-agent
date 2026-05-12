package com.fukang.knowledge.agent.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * <p>配置分页插件、Mapper 扫描路径以及公共字段的自动填充策略（创建时间、更新时间）</p>
 */
@Configuration
@MapperScan("com.fukang.knowledge.agent.infrastructure.persistence.mapper")
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 配置 MyBatis-Plus 拦截器插件
     * <p>当前配置了 PostgreSQL 分页插件，后续可根据实际数据库类型调整</p>
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 插入时的自动填充策略
     * <p>自动填充 createTime、updateTime 为当前时间</p>
     *
     * @param metaObject MyBatis-Plus 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时的自动填充策略
     * <p>自动填充 updateTime 为当前时间</p>
     *
     * @param metaObject MyBatis-Plus 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
