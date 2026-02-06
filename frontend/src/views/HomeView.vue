<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from "element-plus";
import { homePageTokenInit } from "../utils/JwtUtils";
import { chatRequest, chatStreamURL, fetchHistory, logout } from '../api';
import {
  Promotion,
  Setting,
  Delete,
  UserFilled,
  Service,
  Moon,
  Sunny,
  Monitor,
  Expand,
  Fold,
  SwitchButton,
  RefreshRight
} from "@element-plus/icons-vue";
import { marked } from 'marked';
import DOMPurify from "dompurify";
import type { ChatMessage } from "../interfaces/ApiTypes";
import { v4 as uuidV4 } from 'uuid'; // 如果没有安装 uuid，可以用 Date.now() + Math.random() 代替

// --- 状态定义 ---

// UI 状态
const isSidebarCollapsed = ref(false);
const showSettings = ref(false);
const themeMode = ref<'light' | 'dark' | 'auto'>('auto');

// 聊天数据
const prompt = ref('');
const messages = ref<ChatMessage[]>([]); // 存储对话列表
const isLoading = ref(false);
const isStreaming = ref(false);
const currentSessionId = ref('');
const historyLoading = ref(false);

// SSE 实例
let eventSource: EventSource | null = null;
const messagesContainer = ref<HTMLElement | null>(null);

// --- 生命周期 ---

onMounted(() => {
  homePageTokenInit();
  initTheme();
});

onUnmounted(() => {
  handleStreamClose();
});

// --- 功能函数：主题设置 ---

const initTheme = () => {
  const storedTheme = localStorage.getItem('app-theme') as 'light' | 'dark' | 'auto';
  if (storedTheme) themeMode.value = storedTheme;
  applyTheme();
};

const applyTheme = () => {
  const htmlEl = document.documentElement;
  const isDark =
      themeMode.value === 'dark' ||
      (themeMode.value === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  if (isDark) {
    htmlEl.classList.add('dark');
  } else {
    htmlEl.classList.remove('dark');
  }
  localStorage.setItem('app-theme', themeMode.value);
};

// 监听系统主题变化 (当为 auto 时)
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  if (themeMode.value === 'auto') applyTheme();
});

const handleThemeChange = (val: 'light' | 'dark' | 'auto') => {
  themeMode.value = val;
  applyTheme();
};

// --- 功能函数：历史记录 ---

const loadHistory = () => {
  historyLoading.value = true;
  // 假设获取最近 50 条
  fetchHistory(50, (data) => {
    console.log(`历史记录加载: ${data.length}`)

    if (data) {
      // 为每条历史消息补全前端需要的字段
      messages.value = data;
      scrollToBottom();
    }
  }, (msg) => {
    ElMessage.warning("获取历史记录失败: " + msg);
  });
  historyLoading.value = false;
};

const deleteMessage = (id: string) => {
  ElMessageBox.confirm('确定要删除这条消息吗？(仅删除本地显示)', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messages.value = messages.value.filter(m => m.id !== id);
    ElMessage.success('消息已移除');
  }).catch(() => {
  });
};

const handleLogout = () => {
  logout(() => {
  });
};

// --- 功能函数：聊天核心 ---

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const renderMarkdown = (content: string) => {
  try {
    const rawHtml = marked.parse(content || '') as string;
    return DOMPurify.sanitize(rawHtml);
  } catch (error) {
    return content;
  }
};

const sendMessage = async () => {
  if (!prompt.value.trim() || isLoading.value) return;

  const userText = prompt.value;
  prompt.value = ''; // 清空输入框

  // 1. 添加用户消息到列表
  messages.value.push({
    role: 'user',
    content: userText,
    id: uuidV4()
  });
  scrollToBottom();

  isLoading.value = true;

  // 2. 发送请求 (携带当前完整的 messages 上下文)
  chatRequest(messages.value, (data) => {
    currentSessionId.value = data.sessionId;

    if (currentSessionId.value) {
      // 3. 准备接收 AI 回复，先占位
      const aiMsgId = uuidV4();
      messages.value.push({
        role: 'assistant',
        content: '',
        id: aiMsgId
      });
      scrollToBottom();

      // 4. 连接 SSE
      connectSSE(currentSessionId.value, aiMsgId);
    } else {
      ElMessage.error("Chat request failed: No Session ID");
      isLoading.value = false;
    }
  });
};

const connectSSE = (sessionId: string, targetMsgId: string) => {
  if (eventSource) eventSource.close();

  const sseUrl = `${chatStreamURL}/${sessionId}`;
  eventSource = new EventSource(sseUrl);
  isStreaming.value = true;

  eventSource.onopen = () => {
    console.log('SSE Connected');
  };

  eventSource.onmessage = (event) => {
    const data = event.data;

    if (data === '[DONE]') {
      handleStreamClose();
    } else if (data.startsWith('[ERROR]')) {
      const errMsg = `[系统错误: ${data.substring(7)}]`;
      updateMessageContent(targetMsgId, errMsg, true);
      handleStreamClose();
    } else {
      // 累加内容到对应的消息中
      if (data.length > 0) { // 假设后端返回的是纯文本片段
        // 注意：有些后端在 SSE 中第一个字符可能是特殊标记，需根据实际情况处理
        // 原代码逻辑：if (data.length > 1) responseContent += data.substring(1);
        // 假设这里直接是内容：
        const contentFragment = data.length > 1 ? data.substring(1) : "";
        updateMessageContent(targetMsgId, contentFragment);
      }
    }
  };

  eventSource.onerror = () => {
    console.error('SSE Error');
    handleStreamClose();
  };
};

const updateMessageContent = (id: string, newContent: string, isAppend = true) => {
  const target = messages.value.find(m => m.id === id);
  if (target) {
    target.content = isAppend ? target.content + newContent : newContent;
    scrollToBottom();
  }
};

const handleStreamClose = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  isStreaming.value = false;
  isLoading.value = false;
};

// 配置 markdown
marked.setOptions({ breaks: true, gfm: true });

</script>

<template>
  <div class="AI-layout">

    <aside :class="['sidebar', { 'collapsed': isSidebarCollapsed }]">
      <div class="sidebar-header">
        <div class="menu-btn" @click="isSidebarCollapsed = !isSidebarCollapsed">
          <el-icon size="24">
            <component :is="isSidebarCollapsed ? Expand : Fold"/>
          </el-icon>
        </div>
        <span v-if="!isSidebarCollapsed" class="logo-text">Menu</span>
      </div>

      <div class="sidebar-content">
        <div v-if="!isSidebarCollapsed" class="history-section">
          <div class="section-title">Message Records</div>
          <div class="history-list" v-loading="historyLoading">
            <div class="history-item" @click="loadHistory">
              <el-icon>
                <RefreshRight/>
              </el-icon>
              <span>重新加载历史 ({{ messages.length }})</span>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="sidebar-btn" @click="showSettings = true">
          <el-icon>
            <Setting/>
          </el-icon>
          <span v-if="!isSidebarCollapsed">设置</span>
        </div>
        <div class="sidebar-btn logout" @click="handleLogout">
          <el-icon>
            <SwitchButton/>
          </el-icon>
          <span v-if="!isSidebarCollapsed">退出登录</span>
        </div>
      </div>
    </aside>

    <main class="main-area">
      <header class="main-header">
        <div class="model-selector">Web AI Chat</div>
      </header>

      <div class="chat-container" ref="messagesContainer">

        <div v-if="messages.length === 0 && !isLoading" class="welcome-screen">
          <h1>你好, 有什么想说的话吗？</h1>
          <p class="subtitle">可以问我任何问题，或者只是聊天。</p>
        </div>

        <div v-for="msg in messages" :key="msg.id" :class="['message-row', msg.role]">
          <div class="avatar">
            <el-icon v-if="msg.role === 'user'">
              <UserFilled/>
            </el-icon>
            <el-icon v-else class="ai-icon">
              <Service/>
            </el-icon>
          </div>

          <div class="message-content-wrapper">
            <div class="message-sender">{{ msg.role === 'user' ? 'You' : 'AI' }}</div>
            <div class="message-bubble">
              <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="user-text">{{ msg.content }}</div>
            </div>

            <div class="message-actions">
              <el-icon class="action-btn delete" @click="deleteMessage(msg.id!)" title="删除消息">
                <Delete/>
              </el-icon>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area-wrapper">
        <div class="input-box">
          <el-input
              v-model="prompt"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 6 }"
              placeholder="输入指令..."
              @keydown.enter.prevent="sendMessage"
              resize="none"
              :disabled="isLoading"
              class="chat-input"
          />
          <button
              class="send-btn"
              :disabled="!prompt.trim() || isLoading"
              @click="sendMessage"
          >
            <el-icon v-if="!isLoading">
              <Promotion/>
            </el-icon>
            <span v-else class="loading-dot">...</span>
          </button>
        </div>
        <div class="footer-note">
          AI may display inaccurate info, so double-check its responses.
        </div>
      </div>
    </main>

    <el-dialog v-model="showSettings" title="设置" width="400px" align-center>
      <div class="setting-item">
        <span>外观主题</span>
        <el-radio-group v-model="themeMode" @change="handleThemeChange">
          <el-radio-button label="light">
            <el-icon>
              <Sunny/>
            </el-icon>
            浅色
          </el-radio-button>
          <el-radio-button label="auto">
            <el-icon>
              <Monitor/>
            </el-icon>
            跟随系统
          </el-radio-button>
          <el-radio-button label="dark">
            <el-icon>
              <Moon/>
            </el-icon>
            深色
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-dialog>

  </div>
</template>

<style lang="scss" scoped>
/* 使用 CSS 变量定义颜色，方便暗色模式切换 */
:root {
  --bg-color: #ffffff;
  --sidebar-bg: #f0f4f9;
  --text-color: #1f1f1f;
  --input-bg: #f0f4f9;
  --hover-bg: #e1e5ea;
  --border-color: #e5e7eb;
}

/* 暗色模式覆盖 */
:global(html.dark) {
  --bg-color: #131314;
  --sidebar-bg: #1e1f20;
  --text-color: #e3e3e3;
  --input-bg: #1e1f20;
  --hover-bg: #333537;
  --border-color: #444746;
}

.AI-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: var(--bg-color);
  color: var(--text-color);
  font-family: 'Roboto', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  transition: background-color 0.3s, color 0.3s;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background-color: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  padding: 10px;

  &.collapsed {
    width: 70px;

    .logo-text, .section-title, span {
      display: none;
    }

    .sidebar-btn {
      justify-content: center;
    }
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-bottom: 20px;
  cursor: pointer;

  .menu-btn {
    padding: 8px;
    border-radius: 50%;

    &:hover {
      background-color: var(--hover-bg);
    }
  }

  .logo-text {
    margin-left: 10px;
    font-size: 20px;
    font-weight: 500;
    color: #8e8ea0; /* 类似 Gemini 的 Logo 色 */
  }
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;

  .section-title {
    font-size: 12px;
    padding: 10px 15px;
    font-weight: bold;
    color: #666;
  }

  .history-item {
    display: flex;
    align-items: center;
    padding: 10px 15px;
    border-radius: 20px;
    cursor: pointer;
    margin-bottom: 5px;

    &:hover {
      background-color: var(--hover-bg);
    }

    .el-icon {
      margin-right: 10px;
    }

    span {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 14px;
    }
  }
}

.sidebar-footer {
  margin-top: auto;

  .sidebar-btn {
    display: flex;
    align-items: center;
    padding: 12px 15px;
    cursor: pointer;
    border-radius: 8px;
    margin-top: 5px;

    &:hover {
      background-color: var(--hover-bg);
    }

    .el-icon {
      margin-right: 10px;
      font-size: 18px;
    }
  }
}

/* 主区域 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  max-width: 100%;
}

.main-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding-left: 20px;
  font-size: 18px;
  font-weight: 500;
  color: #888;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 15% 120px 15%; /* 底部留白给输入框 */
  scroll-behavior: smooth;

  @media (max-width: 768px) {
    padding: 20px 10px 120px 10px;
  }
}

.welcome-screen {
  height: 80%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;

  h1 {
    font-weight: 500;
    background: -webkit-linear-gradient(45deg, #4285f4, #9b72cb);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-size: 48px;
    margin-bottom: 10px;
  }

  .subtitle {
    color: #888;
    font-size: 20px;
  }
}

/* 消息气泡 */
.message-row {
  display: flex;
  margin-bottom: 30px;

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 15px;
    flex-shrink: 0;

    .el-icon {
      font-size: 24px;
    }

    .ai-icon {
      color: #4285f4;
    }
  }

  .message-content-wrapper {
    flex: 1;
    max-width: 100%;
  }

  .message-sender {
    font-weight: 600;
    margin-bottom: 5px;
    font-size: 14px;
  }

  .message-bubble {
    line-height: 1.6;
    font-size: 16px;

    .user-text {
      white-space: pre-wrap;
    }
  }

  .message-actions {
    margin-top: 5px;
    opacity: 0;
    transition: opacity 0.2s;

    .action-btn {
      cursor: pointer;
      font-size: 16px;
      color: #888;

      &:hover {
        color: #f56c6c;
      }
    }
  }

  &:hover .message-actions {
    opacity: 1;
  }
}

/* 输入区域 */
.input-area-wrapper {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: var(--bg-color); /* 遮挡底部内容 */
  padding: 20px 15% 10px 15%;

  @media (max-width: 768px) {
    padding: 20px 10px 10px 10px;
  }

  .input-box {
    position: relative;
    background-color: var(--input-bg);
    border-radius: 25px;
    padding: 10px 15px;
    display: flex;
    align-items: flex-end;
    border: 1px solid transparent;
    transition: border 0.3s;

    &:focus-within {
      border-color: #ccc;
      background-color: var(--bg-color);
      box-shadow: 0 0 10px rgba(0, 0, 0, 0.05);
    }
  }

  /* 深度选择器修改 element-plus textarea 样式 */
  :deep(.el-textarea__inner) {
    box-shadow: none !important;
    background-color: transparent !important;
    border: none !important;
    padding: 5px 0;
    font-size: 16px;
    color: var(--text-color);
    resize: none;
  }

  .send-btn {
    width: 40px;
    height: 40px;
    border: none;
    background: transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    margin-left: 10px;

    &:hover:not(:disabled) {
      background-color: var(--hover-bg);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  .footer-note {
    text-align: center;
    font-size: 12px;
    color: #888;
    margin-top: 10px;
  }
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Markdown 样式微调 (可选) */
:deep(.markdown-body) {
  background-color: transparent !important;
  color: var(--text-color) !important;
  font-size: 16px;

  pre {
    background-color: var(--sidebar-bg) !important;
    border-radius: 8px;
    padding: 15px;
  }
}
</style>
