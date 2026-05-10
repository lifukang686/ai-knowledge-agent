import { Conversation, Message, ConversationCreateRequest, MessageCreateRequest, RAGQueryRequest, ConversationQueryParams, PaginatedResponse } from '../types/conversation';

// Mock data for conversations
const mockConversations: Conversation[] = [
  {
    id: '1',
    title: '公司规章制度查询',
    knowledge_base_id: '1',
    status: 'active',
    message_count: 5,
    last_message_at: '2024-01-20T14:30:00Z',
    created_at: '2024-01-20T14:00:00Z',
    updated_at: '2024-01-20T14:30:00Z'
  },
  {
    id: '2',
    title: '技术架构相关问题',
    knowledge_base_id: '1',
    status: 'completed',
    message_count: 12,
    last_message_at: '2024-01-19T16:45:00Z',
    created_at: '2024-01-19T15:00:00Z',
    updated_at: '2024-01-19T16:45:00Z'
  },
  {
    id: '3',
    title: '产品需求讨论',
    knowledge_base_id: '2',
    status: 'archived',
    message_count: 8,
    last_message_at: '2024-01-18T11:20:00Z',
    created_at: '2024-01-18T10:00:00Z',
    updated_at: '2024-01-18T11:20:00Z'
  }
];

// Mock data for messages
const mockMessages: Message[] = [
  {
    id: 'msg_1',
    conversation_id: '1',
    role: 'user',
    content: '请问公司的考勤制度是怎样的？',
    created_at: '2024-01-20T14:00:00Z',
    updated_at: '2024-01-20T14:00:00Z'
  },
  {
    id: 'msg_2',
    conversation_id: '1',
    role: 'assistant',
    content: '根据公司规章制度，我们的考勤制度如下：\n\n工作时间：周一至周五 9:00-18:00，午休时间 12:00-13:00\n\n考勤要求：\n1. 员工需要按时上下班打卡\n2. 迟到超过15分钟计为迟到\n3. 早退需要提前请假\n4. 每月允许3次迟到，超出部分按公司规定处理\n\n请假制度：\n- 事假需要提前1天申请\n- 病假需要提供医院证明\n- 年假需要提前1周申请\n\n具体的考勤细则可以参考员工手册第3章。',
    sources: [
      {
        document_id: '1',
        document_name: '公司规章制度.pdf',
        chunk_id: 'chunk_3',
        content: '第三章：考勤制度\n\n工作时间：周一至周五 9:00-18:00...',
        score: 0.95
      }
    ],
    created_at: '2024-01-20T14:01:00Z',
    updated_at: '2024-01-20T14:01:00Z'
  },
  {
    id: 'msg_3',
    conversation_id: '1',
    role: 'user',
    content: '那加班有什么规定吗？',
    created_at: '2024-01-20T14:15:00Z',
    updated_at: '2024-01-20T14:15:00Z'
  },
  {
    id: 'msg_4',
    conversation_id: '1',
    role: 'assistant',
    content: '关于加班的规定：\n\n1. 加班需要提前申请并获得主管批准\n2. 工作日加班按1.5倍工资计算\n3. 周末加班按2倍工资计算\n4. 法定节假日加班按3倍工资计算\n5. 加班时间从下班后开始计算，最小单位为1小时\n\n员工每月加班时间原则上不超过36小时。',
    sources: [
      {
        document_id: '1',
        document_name: '公司规章制度.pdf',
        chunk_id: 'chunk_5',
        content: '第四章：加班制度\n\n员工因工作需要加班时，需要...',
        score: 0.88
      }
    ],
    created_at: '2024-01-20T14:16:00Z',
    updated_at: '2024-01-20T14:16:00Z'
  }
];

export const conversationService = {
  // Get conversations list
  async getConversations(params: ConversationQueryParams): Promise<PaginatedResponse<Conversation>> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    let filteredConversations = [...mockConversations];
    
    if (params.knowledge_base_id) {
      filteredConversations = filteredConversations.filter(conv => 
        conv.knowledge_base_id === params.knowledge_base_id
      );
    }
    
    if (params.status) {
      filteredConversations = filteredConversations.filter(conv => 
        conv.status === params.status
      );
    }
    
    if (params.title) {
      filteredConversations = filteredConversations.filter(conv => 
        conv.title.toLowerCase().includes(params.title!.toLowerCase())
      );
    }
    
    // Apply pagination
    const start = (params.page - 1) * params.pageSize;
    const end = start + params.pageSize;
    const paginatedConversations = filteredConversations.slice(start, end);
    
    return {
      data: paginatedConversations,
      total: filteredConversations.length,
      page: params.page,
      pageSize: params.pageSize
    };
  },

  // Get conversation by ID
  async getConversationById(id: string): Promise<Conversation> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    const conversation = mockConversations.find(conv => conv.id === id);
    if (!conversation) {
      throw new Error('Conversation not found');
    }
    
    return conversation;
  },

  // Create new conversation
  async createConversation(data: ConversationCreateRequest): Promise<Conversation> {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const newConversation: Conversation = {
      id: String(Date.now()),
      ...data,
      status: 'active',
      message_count: 0,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockConversations.unshift(newConversation);
    return newConversation;
  },

  // Get messages for conversation
  async getMessages(conversationId: string): Promise<Message[]> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    return mockMessages.filter(msg => msg.conversation_id === conversationId)
      .sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime());
  },

  // Send message (RAG query)
  async sendMessage(data: MessageCreateRequest): Promise<Message> {
    await new Promise(resolve => setTimeout(resolve, 1500)); // Simulate RAG processing time
    
    // Add user message
    const userMessage: Message = {
      id: String(Date.now()),
      conversation_id: data.conversation_id,
      role: 'user',
      content: data.content,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockMessages.push(userMessage);
    
    // Simulate RAG response
    const ragResponse = `基于知识库检索，我来回答您的问题：\n\n${data.content}\n\n根据检索到的相关信息，这是一个基于RAG技术的智能回答。系统会从知识库中检索相关内容，然后生成回答。`;
    
    const assistantMessage: Message = {
      id: String(Date.now() + 1),
      conversation_id: data.conversation_id,
      role: 'assistant',
      content: ragResponse,
      sources: [
        {
          document_id: '1',
          document_name: '公司规章制度.pdf',
          chunk_id: 'chunk_1',
          content: '相关文档内容片段...',
          score: 0.85
        }
      ],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockMessages.push(assistantMessage);
    
    // Update conversation message count and last message time
    const conversation = mockConversations.find(conv => conv.id === data.conversation_id);
    if (conversation) {
      conversation.message_count += 2;
      conversation.last_message_at = new Date().toISOString();
      conversation.updated_at = new Date().toISOString();
    }
    
    return assistantMessage;
  },

  // RAG query (for standalone Q&A page)
  async ragQuery(data: RAGQueryRequest): Promise<Message> {
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    // Simulate RAG response
    const ragResponse = `基于知识库检索，我来回答您的问题：\n\n${data.query}\n\n这是一个基于RAG（Retrieval-Augmented Generation）技术的智能回答。系统会从知识库中检索相关内容，然后结合大语言模型生成回答。\n\n检索到的相关信息包括：\n- 文档匹配度：95%\n- 检索到的文档片段：3个\n- 回答置信度：高`;
    
    return {
      id: String(Date.now()),
      conversation_id: data.conversation_id || 'rag_query',
      role: 'assistant',
      content: ragResponse,
      sources: [
        {
          document_id: '1',
          document_name: '知识库文档.pdf',
          chunk_id: 'chunk_1',
          content: '检索到的相关内容片段...',
          score: 0.95
        }
      ],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
  },

  // Archive conversation
  async archiveConversation(id: string): Promise<Conversation> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const conversation = mockConversations.find(conv => conv.id === id);
    if (!conversation) {
      throw new Error('Conversation not found');
    }
    
    conversation.status = 'archived';
    conversation.updated_at = new Date().toISOString();
    
    return conversation;
  },

  // Delete conversation
  async deleteConversation(id: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const conversationIndex = mockConversations.findIndex(conv => conv.id === id);
    if (conversationIndex === -1) {
      throw new Error('Conversation not found');
    }
    
    mockConversations.splice(conversationIndex, 1);
    
    // Also delete related messages
    const messageIndexes = mockMessages
      .map((msg, index) => msg.conversation_id === id ? index : -1)
      .filter(index => index !== -1)
      .sort((a, b) => b - a); // Sort in reverse order for safe deletion
    
    messageIndexes.forEach(index => {
      mockMessages.splice(index, 1);
    });
  }
};