<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { UserFilled, Service, Position, Loading } from '@element-plus/icons-vue';
import MarkdownIt from 'markdown-it';
import DOMPurify from 'dompurify';
import { fetchStream } from '../utils/chatRequest'; // 导入刚才写的工具

// --- 类型定义 ---
interface Message {
  role: 'user' | 'assistant';
  content: string;
}

// --- 状态管理 ---
const messageList = ref<Message[]>([
  {role: 'assistant', content: '你好！我是你的 AI 助手，有什么可以帮你的吗？'}
]);
const inputContent = ref('');
const isLoading = ref(false);
const scrollbarRef = ref();

// --- Markdown 配置 ---
const md = new MarkdownIt({linkify: true, breaks: true});
const renderMarkdown = (text: string) => {
  const rawHtml = md.render(text);
  return DOMPurify.sanitize(rawHtml); // 防止 XSS
};

// --- 核心逻辑 ---
const sendMessage = async () => {
  const text = inputContent.value.trim();
  if (!text || isLoading.value) return;

  // 1. 添加用户消息
  messageList.value.push({role: 'user', content: text});
  inputContent.value = '';
  scrollToBottom();

  // 2. 预占位 AI 消息
  isLoading.value = true;
  messageList.value.push({role: 'assistant', content: ''});

  // 3. 发起流式请求
  await fetchStream({
    url: 'https://api.your-backend.com/chat/completions', // 替换你的后端API
    body: {prompt: text},
    onMessage: (_chunk) => {
      // 实时追加文本实现打字机效果
      scrollToBottom();
    },
    onError: (err) => {
      console.error(err);
    },
    onFinish: () => {
      isLoading.value = false;
    }
  });
};

// --- 滚动到底部 ---
const scrollToBottom = async () => {
  await nextTick();
  const wrap = scrollbarRef.value?.wrapRef;
  if (wrap) {
    wrap.scrollTop = wrap.scrollHeight;
  }
};
</script>

<template>
  <div class="chat-container">
    <el-scrollbar ref="scrollbarRef" class="chat-history">
      <div class="messages-wrapper">
        <div
            v-for="(msg, index) in messageList"
            :key="index"
            class="message-row"
            :class="msg.role === 'user' ? 'user-row' : 'assistant-row'"
        >
          <div class="avatar">
            <el-avatar :icon="msg.role === 'user' ? UserFilled : Service" :size="36"/>
          </div>

          <div class="content-bubble">
            <div
                v-if="msg.role === 'assistant'"
                class="markdown-body"
                v-html="renderMarkdown(msg.content)"
            ></div>
            <div v-else>{{ msg.content }}</div>
          </div>
        </div>

        <div v-if="isLoading" class="loading-indicator">
          <el-icon class="is-loading">
            <Loading/>
          </el-icon>
          正在思考...
        </div>
      </div>
    </el-scrollbar>

    <div class="input-area">
      <div class="input-box">
        <el-input
            v-model="inputContent"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="发送消息..."
            @keydown.enter.prevent="sendMessage"
            :disabled="isLoading"
        />
        <el-button type="primary" :icon="Position" circle @click="sendMessage"
                   :disabled="!inputContent.trim() || isLoading" class="send-btn"/>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh; /* 全屏高度 */
  background-color: #f4f6f8;
}

.chat-history {
  flex: 1; /* 占据剩余空间 */
  padding: 20px;

  .messages-wrapper {
    max-width: 800px;
    margin: 0 auto;
    padding-bottom: 20px;
  }
}

.message-row {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
  gap: 12px;

  &.user-row {
    flex-direction: row-reverse;

    .content-bubble {
      background-color: #95ec69; /* 微信绿风格 */
      color: #000;
      border-radius: 8px 0 8px 8px;
    }
  }

  &.assistant-row {
    .content-bubble {
      background-color: #fff;
      border: 1px solid #e0e0e0;
      border-radius: 0 8px 8px 8px;
    }
  }
}

.content-bubble {
  padding: 10px 16px;
  line-height: 1.6;
  font-size: 15px;
  max-width: 80%;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  /* Markdown 样式微调 */
  :deep(pre) {
    background: #f6f8fa;
    padding: 10px;
    border-radius: 4px;
    overflow-x: auto;
  }

  :deep(code) {
    font-family: Consolas, monospace;
  }
}

.input-area {
  background: #fff;
  border-top: 1px solid #dcdfe6;
  padding: 20px;

  .input-box {
    max-width: 800px;
    margin: 0 auto;
    display: flex;
    gap: 10px;
    align-items: flex-end;
    background: #fff;

    /* 调整 Element Plus 输入框样式使其更像 Chat */
    :deep(.el-textarea__inner) {
      box-shadow: none;
      background: #f4f6f8;
      border-radius: 8px;
      padding: 10px;
      resize: none;

      &:focus {
        background: #fff;
        box-shadow: 0 0 0 1px #409eff;
      }
    }
  }
}

.loading-indicator {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: -10px;
  margin-bottom: 10px;
}
</style>