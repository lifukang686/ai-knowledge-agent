package com.fukang.knowledge.agent.rag;

/**
 * 余弦相似度计算工具类
 * <p>提供向量相似度计算及向量解析的静态工具方法，纯 Java 实现不依赖第三方库。
 * 适用于 RAG 检索场景中对嵌入向量进行相似度比较</p>
 *
 * @deprecated 自 langchain4j 集成后，相似度计算由 pgvector 在数据库层面完成，此类保留仅供测试和性能对比使用
 */
@Deprecated
public final class SimilarityCalculator {

    private static final double ZERO_THRESHOLD = 1e-10;

    private SimilarityCalculator() {
    }

    /**
     * 计算两个向量的余弦相似度
     * <p>通过点积与 L2 范数的比值计算余弦相似度，用于衡量两个向量在方向上的接近程度。
     * 任一向量为零向量时返回 0.0，结果会自动钳制在 [0.0, 1.0] 范围内以消除浮点误差</p>
     *
     * @param a 向量 A
     * @param b 向量 B
     * @return 余弦相似度 (0.0 ~ 1.0)，任一向量为零向量时返回 0.0
     * @throws IllegalArgumentException 如果两个向量长度不一致
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) {
            return 0.0;
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vector length mismatch: a.length=" + a.length + ", b.length=" + b.length);
        }

        double dotProduct = 0.0;
        double normASquared = 0.0;
        double normBSquared = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normASquared += (double) a[i] * a[i];
            normBSquared += (double) b[i] * b[i];
        }

        double normA = Math.sqrt(normASquared);
        double normB = Math.sqrt(normBSquared);

        if (normA < ZERO_THRESHOLD || normB < ZERO_THRESHOLD) {
            return 0.0;
        }

        double similarity = dotProduct / (normA * normB);

        if (similarity < 0.0) {
            return 0.0;
        }
        if (similarity > 1.0) {
            return 1.0;
        }
        return similarity;
    }

    /**
     * 从 JSON 字符串解析浮点向量数组
     * <p>支持格式: "[0.1, 0.2, 0.3]" 或 "[0.1,0.2,0.3]"。
     * 通过字符串操作手动解析，不依赖 Jackson 等外部 JSON 库</p>
     *
     * @param jsonArray JSON 格式的浮点数组字符串
     * @return 解析后的 float[]，解析失败返回 null
     */
    public static float[] parseVector(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return null;
        }

        String trimmed = jsonArray.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }

        String content = trimmed.substring(1, trimmed.length() - 1);
        if (content.isBlank()) {
            return new float[0];
        }

        String[] parts = content.split(",");
        float[] result = new float[parts.length];

        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return result;
    }
}