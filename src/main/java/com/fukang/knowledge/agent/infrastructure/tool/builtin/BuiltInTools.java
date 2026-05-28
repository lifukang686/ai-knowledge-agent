package com.fukang.knowledge.agent.infrastructure.tool.builtin;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内置系统工具集
 * <p>通过 {@code @Tool} 注解注册，与页面配置化工具（{@code DynamicToolProvider} + ToolRegistry）共存。
 * 二者通过 {@code AiServices.builder().tools(...)} 同时注入。
 *
 * <pre>
 * 使用方式：
 * {@code
 * AiServices.builder(AgentAiService.class)
 *     .chatLanguageModel(chatModel)
 *     .tools(new BuiltInTools())          // 内置 @Tool 工具
 *     .toolProvider(dynamicToolProvider)  // 页面配置化工具
 *     .build();
 * }
 * </pre>
 * </p>
 *
 * <p>定位：系统级通用工具（稳定、随版本发布），业务专用工具走页面配置化（动态注册、热更新）</p>
 */
@Slf4j
public class BuiltInTools {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());

    /**
     * 获取当前日期时间
     *
     * @return ISO 8601 格式的时间字符串，如 "2026-05-29T00:16:30.123+08:00"
     */
    @Tool("获取当前日期时间，返回 ISO 8601 格式字符串")
    public String getCurrentTime() {
        String now = ISO_FORMATTER.format(Instant.now());
        log.debug("内置工具 getCurrentTime: {}", now);
        return now;
    }

    /**
     * 执行数学表达式计算
     *
     * @param expression 数学表达式，支持加减乘除、括号和常用函数（如 sqrt、abs、max、min）
     * @return 计算结果
     */
    @Tool("执行数学计算，支持加减乘除、括号和常用数学函数")
    public double calculate(@P("数学表达式，如 2+3*4 或 sqrt(16)+max(1,2,3)") String expression) {
        log.debug("内置工具 calculate: expression={}", expression);
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine == null) {
                log.warn("JavaScript 脚本引擎不可用，使用简单求值");
                return simpleEvaluate(expression);
            }
            Object result = engine.eval(expression);
            if (result instanceof Number num) {
                return num.doubleValue();
            }
            return Double.parseDouble(result.toString());
        } catch (Exception e) {
            log.warn("表达式计算失败: expression={}, error={}", expression, e.getMessage());
            throw new IllegalArgumentException("表达式计算失败: " + expression + ", 原因: " + e.getMessage());
        }
    }

    /**
     * 生成指定范围内的随机整数
     *
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 随机整数
     */
    @Tool("生成 [min, max] 范围内的随机整数")
    public int randomInt(
            @P("最小值（含）") int min,
            @P("最大值（含）") int max) {
        if (min > max) {
            throw new IllegalArgumentException("min 不能大于 max: min=" + min + ", max=" + max);
        }
        int result = ThreadLocalRandom.current().nextInt(min, max + 1);
        log.debug("内置工具 randomInt: min={}, max={}, result={}", min, max, result);
        return result;
    }

    /**
     * 简单表达式求值（仅支持基本四则运算，作为 ScriptEngine 不可用时的降级方案）
     */
    private double simpleEvaluate(String expression) {
        String cleaned = expression.replaceAll("\\s+", "");

        if (cleaned.contains("(")) {
            int start = cleaned.lastIndexOf('(');
            int end = cleaned.indexOf(')', start);
            if (start >= 0 && end > start) {
                String inner = cleaned.substring(start + 1, end);
                double innerResult = simpleEvaluate(inner);
                return simpleEvaluate(
                        cleaned.substring(0, start) + innerResult + cleaned.substring(end + 1));
            }
        }

        String[] parts;
        double result;

        if (cleaned.contains("+")) {
            parts = splitByFirstOperator(cleaned, '+');
            return simpleEvaluate(parts[0]) + simpleEvaluate(parts[1]);
        }

        if (cleaned.contains("-") && cleaned.lastIndexOf('-') > 0) {
            parts = splitByFirstOperator(cleaned, '-');
            return simpleEvaluate(parts[0]) - simpleEvaluate(parts[1]);
        }

        if (cleaned.contains("*")) {
            parts = splitByFirstOperator(cleaned, '*');
            return simpleEvaluate(parts[0]) * simpleEvaluate(parts[1]);
        }

        if (cleaned.contains("/")) {
            parts = splitByFirstOperator(cleaned, '/');
            double divisor = simpleEvaluate(parts[1]);
            if (divisor == 0) {
                throw new ArithmeticException("除数不能为零");
            }
            return simpleEvaluate(parts[0]) / divisor;
        }

        return Double.parseDouble(cleaned);
    }

    private String[] splitByFirstOperator(String expr, char operator) {
        int idx = -1;
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == operator && depth == 0) {
                if (operator == '-' && i == 0) continue;
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new IllegalArgumentException("无法解析表达式: " + expr);
        }
        return new String[]{expr.substring(0, idx), expr.substring(idx + 1)};
    }
}