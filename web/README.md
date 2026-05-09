# 企业知识库 AI Agent 平台 - 前端

这是企业知识库 AI Agent 平台的对话界面，基于 Vue 3 + Element Plus 构建。

## 功能特性

- ✅ **三栏式布局**：左侧历史会话、中间对话区域、右侧相关文档
- ✅ **知识库选择**：支持切换不同知识库进行问答
- ✅ **Markdown 渲染**：AI 回答支持 Markdown 格式展示
- ✅ **引用来源**：每条 AI 回复显示引用文档，点击可预览
- ✅ **主题切换**：支持亮色/暗色主题
- ✅ **会话管理**：新建会话、切换历史会话（最近10条）
- ✅ **键盘快捷**：Enter 发送，Shift+Enter 换行

## 技术栈

- Vue 3 (Composition API)
- Element Plus
- SCSS
- Marked (Markdown 解析)

## 快速开始

### 安装依赖

```bash
cd web
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000 即可看到页面。

### 构建生产版本

```bash
npm run build
```

## 项目结构

```
web/
├── src/
│   ├── components/
│   │   └── ChatPage.vue    # 主对话页面组件
│   ├── App.vue
│   └── main.js
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 下一步

当前页面使用模拟数据。真实对接后端时，请修改 `ChatPage.vue` 中的 `sendMessage` 函数，调用后端 `/api/qa` 接口：

```javascript
// 示例：调用真实后端
const response = await fetch('/api/qa', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    question: inputContent.value,
    knowledgeBaseId: selectedKnowledgeBase.value
  })
})
const data = await response.json()
```
