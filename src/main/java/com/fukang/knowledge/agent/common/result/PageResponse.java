package com.fukang.knowledge.agent.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应封装
 * <p>封装分页查询的通用响应结构，配合 {@link com.baomidou.mybatisplus.core.metadata.IPage}
 * 使用，提供前端期望的 items/total/page/pageSize 格式</p>
 *
 * @param <T> 列表数据的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** 当前页数据列表 */
    private List<T> items;

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long page;

    /** 每页记录数 */
    private long pageSize;
}