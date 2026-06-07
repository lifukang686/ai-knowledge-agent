请从本轮对话中提取稳定、可跨会话复用的用户记忆。
只保留用户画像、偏好、事实、近期目标，不保存一次性问题、敏感隐私、文档事实。
输出 JSON 数组，每项格式：
{"type":"profile|preference|fact|goal","content":"一句中文短句","confidence":0.8}
没有可保存内容时输出 []。

【用户问题】
{{question}}

【助手回答】
{{answer}}
