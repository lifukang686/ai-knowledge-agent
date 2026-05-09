<template>
  <!-- 主容器，支持主题切换 -->
  <div class="chat-page" :class="{ dark: isDark }">
    <!-- 顶部操作栏 -->
    <div class="header">
      <div class="header-left">
        <span class="logo">知识助手</span>
      </div>
      <div class="header-center">
        <!-- 知识库选择器 -->
        <el-select v-model="selectedKnowledgeBase" placeholder="选择知识库" class="kb-select">
          <el-option label="全部知识库" value="" />
          <el-option
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :label="kb.name"
            :value="kb.id"
          />
        </el-select>
      </div>
      <div class="header-right">
        <!-- 主题切换按钮 -->
        <el-button :icon="isDark ? Sunny : Moon" circle @click="toggleTheme" />
      </div>
    </div>

    <!-- 三栏式布局主体 -->
    <div class="main-content">
      <!-- 左侧栏：历史会话列表 -->
      <div class="sidebar-left">
        <div class="sidebar-header">
          <el-button type="primary" class="new-chat-btn" @click="createNewChat">
            <el-icon><Plus /></el-icon>
            新建会话
          </el-button>
        </div>
        <div class="chat-list">
          <div
            v-for="chat in chatHistory"
            :key="chat.id"
            class="chat-item"
            :class="{ active: currentChatId === chat.id }"
            @click="switchChat(chat.id)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="chat-title">{{ chat.title }}</span>
          </div>
        </div>
      </div>

      <!-- 中间主区域：对话消息流 -->
      <div class="chat-center">
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-for="msg in currentMessages" :key="msg.id" class="message-item">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="message-user">
              <div class="user-avatar">
                <el-icon><User /></el-icon>
              </div>
              <div class="user-content">
                <p>{{ msg.content }}</p>
              </div>
            </div>
            <!-- AI 回复 -->
            <div v-else class="message-assistant">
              <div class="assistant-avatar">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="assistant-content">
                <!-- Markdown 渲染 -->
                <div class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <!-- 引用来源 -->
                <div v-if="msg.references && msg.references.length > 0" class="references">
                  <div class="refs-title">引用来源：</div>
                  <div class="refs-list">
                    <el-tag
                      v-for="ref in msg.references"
                      :key="ref.id"
                      class="ref-tag"
                      @click="openReferenceDialog(ref)"
                    >
                      {{ ref.documentTitle }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部输入框 -->
        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputContent"
              type="textarea"
              :rows="3"
              placeholder="输入您的问题，按 Enter 发送，Shift+Enter 换行"
              @keydown="handleKeyDown"
              class="chat-input"
            />
            <el-button type="primary" class="send-btn" @click="sendMessage">
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧栏：当前引用文档列表 -->
      <div class="sidebar-right">
        <div class="sidebar-header">
          <span class="sidebar-title">相关文档</span>
        </div>
        <div class="docs-list">
          <div v-for="doc in relatedDocs" :key="doc.id" class="doc-item">
            <div class="doc-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="doc-info">
              <div class="doc-name">{{ doc.title }}</div>
              <div class="doc-preview">{{ doc.preview }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 引用预览弹窗 -->
    <el-dialog
      v-model="referenceDialogVisible"
      title="文档片段"
      width="50%"
      :close-on-click-modal="true"
    >
      <div v-if="currentReference" class="ref-preview">
        <h4>{{ currentReference.documentTitle }}</h4>
        <p class="ref-content">{{ currentReference.content }}</p>
      </div>
      <template #footer>
        <el-button @click="referenceDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Moon,
  Sunny,
  Plus,
  ChatDotRound,
  User,
  Connection,
  Promotion,
  Document
} from '@element-plus/icons-vue'
import { marked } from 'marked'

// ==================== 状态管理 ====================

// 主题状态
const isDark = ref(false)

// 知识库数据（模拟）
const knowledgeBases = ref([
  { id: 'kb1', name: '产品知识库' },
  { id: 'kb2', name: '技术文档库' },
  { id: 'kb3', name: '运营资料库' }
])
const selectedKnowledgeBase = ref('')

// 历史会话数据（模拟，最近10条）
const chatHistory = ref([
  { id: 'chat1', title: '如何配置 API 密钥' },
  { id: 'chat2', title: '产品功能介绍' },
  { id: 'chat3', title: '数据库连接问题' },
  { id: 'chat4', title: '用户权限管理' },
  { id: 'chat5', title: '部署指南' },
  { id: 'chat6', title: '常见问题汇总' },
  { id: 'chat7', title: '新功能说明' },
  { id: 'chat8', title: '性能优化建议' },
  { id: 'chat9', title: '安全设置' },
  { id: 'chat10', title: '数据备份' }
])
const currentChatId = ref('chat1')

// 当前会话消息（模拟）
const currentMessages = ref([
  {
    id: 'msg1',
    role: 'user',
    content: '什么是 RAG 检索增强生成？'
  },
  {
    id: 'msg2',
    role: 'assistant',
    content: '**RAG (Retrieval-Augmented Generation)** 是一种结合检索和生成的 AI 技术框架：\n\n1. **检索阶段**：从知识库中找到与用户问题相关的文档片段\n2. **增强阶段**：将检索到的内容作为上下文补充到提示词中\n3. **生成阶段**：基于增强后的提示，由大模型生成精准回答\n\n这种方法可以有效减少幻觉，提高答案的可信度。',
    references: [
      { id: 'ref1', documentTitle: 'RAG技术白皮书.pdf', content: '检索增强生成（RAG）通过将外部知识库检索与大模型生成相结合...' },
      { id: 'ref2', documentTitle: 'AI应用开发指南.docx', content: '实现 RAG 需要先对文档进行向量化处理，然后进行相似度检索...' }
    ]
  }
])

// 相关文档列表（右侧栏）
const relatedDocs = ref([
  { id: 'd1', title: 'RAG技术白皮书.pdf', preview: '检索增强生成技术详解...' },
  { id: 'd2', title: '向量数据库入门.md', preview: '什么是向量检索？如何构建索引...' },
  { id: 'd3', title: '系统架构设计.docx', preview: '平台整体架构与模块划分...' }
])

// 输入与弹窗状态
const inputContent = ref('')
const messageListRef = ref(null)
const referenceDialogVisible = ref(false)
const currentReference = ref(null)

// ==================== 核心功能函数 ====================

/**
 * 切换主题（亮色/暗色）
 */
const toggleTheme = () => {
  isDark.value = !isDark.value
  // 实际项目中可将主题设置保存到 localStorage
}

/**
 * 渲染 Markdown 文本
 */
const renderMarkdown = (text) => {
  return marked(text)
}

/**
 * 创建新会话
 */
const createNewChat = () => {
  const newId = `chat${Date.now()}`
  const newChat = { id: newId, title: '新会话' }
  chatHistory.value.unshift(newChat)
  // 保持只显示最近10条
  if (chatHistory.value.length > 10) {
    chatHistory.value.pop()
  }
  currentChatId.value = newId
  currentMessages.value = []
  ElMessage.success('已创建新会话')
}

/**
 * 切换会话
 */
const switchChat = (chatId) => {
  currentChatId.value = chatId
  // 模拟切换会话时加载对应消息
  if (chatId === 'chat2') {
    currentMessages.value = [
      { id: 'c2m1', role: 'user', content: '介绍一下产品的核心功能' },
      {
        id: 'c2m2',
        role: 'assistant',
        content: '本产品核心功能包括：\n\n- **知识库管理**：文档上传、解析、向量化\n- **智能问答**：基于 RAG 的精准回答\n- **Agent 执行**：多步工具调用完成复杂任务',
        references: [{ id: 'c2r1', documentTitle: '产品手册.pdf', content: '产品核心模块说明...' }]
      }
    ]
  }
}

/**
 * 处理键盘事件：Enter 发送，Shift+Enter 换行
 */
const handleKeyDown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

/**
 * 发送消息
 */
const sendMessage = () => {
  if (!inputContent.value.trim()) {
    return
  }

  // 1. 添加用户消息
  const userMsgId = `msg_${Date.now()}`
  currentMessages.value.push({
    id: userMsgId,
    role: 'user',
    content: inputContent.value.trim()
  })

  const question = inputContent.value
  inputContent.value = ''

  // 滚动到底部
  scrollToBottom()

  // 2. 模拟 AI 回复（延迟一秒）
  setTimeout(() => {
    const aiMsgId = `msg_${Date.now()}`
    currentMessages.value.push({
      id: aiMsgId,
      role: 'assistant',
      content: `这是针对"**${question}**"的模拟回答。\n\n在真实场景中，这里会调用后端的 /api/qa 接口，传入问题和知识库 ID，获取模型生成的答案。`,
      references: [
        { id: aiMsgId + '_r1', documentTitle: '模拟文档1.pdf', content: '相关文档片段内容...' },
        { id: aiMsgId + '_r2', documentTitle: '模拟文档2.docx', content: '另一段参考内容...' }
      ]
    })
    scrollToBottom()
  }, 1000)
}

/**
 * 滚动消息列表到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

/**
 * 打开引用文档预览弹窗
 */
const openReferenceDialog = (ref) => {
  currentReference.value = ref
  referenceDialogVisible.value = true
}

// 组件挂载时滚动到底部
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped lang="scss">
// 定义主题变量
$light-bg: #f5f7fa;
$light-panel: #ffffff;
$light-text: #303133;
$light-border: #e4e7ed;

$dark-bg: #141414;
$dark-panel: #1f1f1f;
$dark-text: #e5eaf3;
$dark-border: #4c4d4f;

.chat-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: $light-bg;
  color: $light-text;
  transition: all 0.3s;

  // 暗色主题样式
  &.dark {
    background-color: $dark-bg;
    color: $dark-text;

    .header,
    .sidebar-header,
    .input-area,
    .chat-item {
      background-color: $dark-panel;
      border-color: $dark-border;
    }

    .message-user .user-content {
      background-color: #409eff;
      color: white;
    }

    .message-assistant .assistant-content {
      background-color: $dark-panel;
      border-color: $dark-border;
    }
  }
}

// 顶部栏
.header {
  height: 60px;
  background-color: $light-panel;
  border-bottom: 1px solid $light-border;
  display: flex;
  align-items: center;
  padding: 0 24px;
  justify-content: space-between;

  .logo {
    font-size: 18px;
    font-weight: bold;
  }

  .kb-select {
    width: 220px;
  }
}

// 主体内容区
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// 通用侧边栏
.sidebar-left,
.sidebar-right {
  width: 260px;
  background-color: $light-panel;
  border-right: 1px solid $light-border;
  display: flex;
  flex-direction: column;
}

.sidebar-right {
  border-right: none;
  border-left: 1px solid $light-border;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid $light-border;
  display: flex;
  justify-content: center;
}

.new-chat-btn {
  width: 100%;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  .chat-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 4px;
    transition: background 0.2s;

    &:hover {
      background-color: #ecf5ff;
    }

    &.active {
      background-color: #ecf5ff;
      color: #409eff;
      font-weight: 500;
    }

    .chat-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

// 中间对话区域
.chat-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;

  .message-item {
    margin-bottom: 24px;
  }

  .message-user,
  .message-assistant {
    display: flex;
    gap: 12px;
  }

  .message-user {
    flex-direction: row-reverse;

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background-color: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .user-content {
      background-color: #409eff;
      color: white;
      padding: 12px 16px;
      border-radius: 12px 12px 4px 12px;
      max-width: 70%;
    }
  }

  .message-assistant {
    .assistant-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background-color: #67c23a;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .assistant-content {
      background-color: $light-panel;
      border: 1px solid $light-border;
      padding: 12px 16px;
      border-radius: 12px 12px 12px 4px;
      max-width: 75%;

      .references {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px dashed $light-border;

        .refs-title {
          font-size: 12px;
          color: #909399;
          margin-bottom: 8px;
        }

        .refs-list {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .ref-tag {
            cursor: pointer;

            &:hover {
              background-color: #409eff;
              color: white;
            }
          }
        }
      }
    }
  }

  // Markdown 基础样式
  .markdown-body {
    line-height: 1.6;

    h1,
    h2,
    h3,
    h4 {
      margin-top: 16px;
      margin-bottom: 8px;
    }

    p {
      margin: 8px 0;
    }

    ul,
    ol {
      padding-left: 20px;
      margin: 8px 0;
    }

    code {
      background-color: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

// 底部输入区
.input-area {
  padding: 16px 24px 24px;
  background-color: $light-panel;
  border-top: 1px solid $light-border;

  .input-wrapper {
    display: flex;
    gap: 12px;
    align-items: flex-end;
  }

  .chat-input {
    flex: 1;
  }
}

// 右侧相关文档
.docs-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  .doc-item {
    display: flex;
    gap: 12px;
    padding: 12px;
    border-radius: 8px;
    margin-bottom: 8px;
    background-color: #f9fafc;

    .doc-icon {
      color: #409eff;
      font-size: 24px;
    }

    .doc-info {
      flex: 1;
      min-width: 0;

      .doc-name {
        font-weight: 500;
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .doc-preview {
        font-size: 12px;
        color: #909399;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

// 引用预览弹窗
.ref-preview {
  h4 {
    margin-bottom: 12px;
  }

  .ref-content {
    background-color: #f5f7fa;
    padding: 16px;
    border-radius: 8px;
    line-height: 1.6;
  }
}
</style>
