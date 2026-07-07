package com.fukang.knowledge.agent.module.servicedesk.service;

import com.fukang.knowledge.agent.common.enums.ServiceTypeEnum;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 服务台服务类型解析器。
 * <p>只负责确定 IT/HR 服务范围，具体工具选择交给 Plan-Execute Agent 的规划提示词处理。</p>
 */
@Service
public class ServiceDeskTypeResolver {

    /**
     * 根据用户显式选择或问题关键词解析服务类型。
     */
    public ServiceTypeEnum resolve(String question, ServiceTypeEnum preferredType) {
        if (preferredType != null && preferredType != ServiceTypeEnum.AUTO) {
            return preferredType;
        }
        String text = question != null ? question.toLowerCase(Locale.ROOT) : "";
        if (matchesHr(text)) {
            return ServiceTypeEnum.HR;
        }
        return ServiceTypeEnum.IT;
    }

    /**
     * 判断问题是否更偏 HR 服务范围。
     */
    private boolean matchesHr(String text) {
        return text.contains("年假")
                || text.contains("调休")
                || text.contains("报销")
                || text.contains("hr")
                || text.contains("薪资")
                || text.contains("工资")
                || text.contains("绩效")
                || text.contains("入职")
                || text.contains("离职")
                || text.contains("劳动合同")
                || text.contains("考勤");
    }
}
