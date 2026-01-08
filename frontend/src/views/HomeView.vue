<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { ElMessage } from "element-plus";
import { homePageTokenInit } from "../utils/JwtUtils.ts";
import HeaderBar from "../components/home/HeaderBar.vue";
import { chatRequest, chatStreamURL } from '../api';
import { More } from "@element-plus/icons-vue";
import { marked } from 'marked';
import DOMPurify from "dompurify";

const prompt = ref('');
const currentSessionId = ref('');
const responseContent = ref('');
const renderedContent = ref('')
const isLoading = ref(false);
const isStreaming = ref(false);
const statusText = ref('未发送请求');
let eventSource: EventSource | null = null;

onMounted(() => {
  homePageTokenInit()
})

const statusColor = computed(() => {
  if (isStreaming.value) return 'green';
  if (isLoading.value) return 'orange';
  if (statusText.value.includes('错误')) return 'red';
  return '#333';
});

const connectSSE = (sessionId: string) => {
  if (eventSource) {
    eventSource.close();
  }

  const sseUrl = `${chatStreamURL}/${sessionId}`;
  eventSource = new EventSource(sseUrl);

  eventSource.onopen = () => {
    statusText.value = 'SSE 连接已建立，等待数据流...';
    isStreaming.value = true;
  };

  eventSource.onmessage = (event) => {
    const data = event.data;

    if (data === '[DONE]') {
      statusText.value = '流式传输完成。';
      handleStreamClose();
    } else if (data.startsWith('[ERROR]')) {
      responseContent.value += `\n[系统错误: ${data.substring(7)}]`;
      statusText.value = '传输错误。';
      handleStreamClose();
    } else {
      if (data.length > 1) {
        responseContent.value += data.substring(1);
      }
    }
  };

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error);
    // 如果错误发生，尝试获取 HTTP 状态码（EventSource 无法直接获取，但可以在 Network 标签页看到）
    statusText.value = 'SSE 连接中断或超时。请检查网络和服务器日志。';
    handleStreamClose();
  };

  eventSource.onerror = (_error) => {
    statusText.value = 'SSE 连接中断或超时。';
    handleStreamClose();
  };
};

const handleStreamClose = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  isStreaming.value = false;
  isLoading.value = false;
};

const sendChatRequest = async () => {
  if (!prompt.value.trim() || isLoading.value) return;

  // 重置状态
  responseContent.value = '';
  currentSessionId.value = '';
  isLoading.value = true;
  statusText.value = '发送请求中...';

  // 发送 POST 请求，获取后端生成的 sessionId
  chatRequest([{ role: 'user', content: prompt.value, id: undefined }], (data) => {
    currentSessionId.value = data.sessionId
    ElMessage.success(`Chat request successfully! (id: ${currentSessionId.value})`);
    if (currentSessionId.value) {
      statusText.value = `请求已入队 (${currentSessionId.value})`;
      connectSSE(currentSessionId.value);
    } else {
      ElMessage.error("chat request failed");
      isLoading.value = false;
      isStreaming.value = false;
    }
  })
};

marked.setOptions({
  breaks: true,
  gfm: true,
});

watch(responseContent, async (newValue) => {
  if (!newValue) {
    renderedContent.value = '';
    return;
  }

  try {
    const rawHtml = await marked.parse(newValue);
    renderedContent.value = DOMPurify.sanitize(rawHtml);
  } catch (error) {
    renderedContent.value = newValue;
  }
}, { immediate: true });

onUnmounted(() => {
  // 组件销毁时关闭连接
  handleStreamClose();
});
</script>

<template>
  <div class="layout-container">
    <el-container>
      <el-aside width="200px">
        <el-scrollbar>
          <el-menu>
            <el-sub-menu index="1">
              <template #title>
                <el-icon>
                  <More/>
                </el-icon>
                Menu
              </template>
              <el-menu-item-group>
                <template #title>Group 1</template>
                <el-menu-item index="1-1">Option 1</el-menu-item>
                <el-menu-item index="1-2">Option 2</el-menu-item>
              </el-menu-item-group>
              <el-menu-item-group title="Group 2">
                <el-menu-item index="1-3">Option 3</el-menu-item>
              </el-menu-item-group>
              <el-sub-menu index="1-4">
                <template #title>Option4</template>
                <el-menu-item index="1-4-1">Option 4-1</el-menu-item>
              </el-sub-menu>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </el-aside>
      <el-container>
        <el-header>
          <HeaderBar/>
        </el-header>
        <el-main>
          <div class="chat-area">
            <p><strong>会话ID:</strong> {{ currentSessionId || '待生成' }}</p>
            <p><strong>状态:</strong>
              <span :style="{ color: statusColor }">{{ statusText }}</span>
            </p>
            <div class="response-box">
              <p><strong>LLM 回复 (流式):</strong></p>
              <div class="response-content" v-html="renderedContent"></div>
            </div>
          </div>
        </el-main>
        <el-footer height="120px">
          <div style="height: 60px">
            <el-row justify="space-between">
              <el-col :span="16">
                <el-input v-model="prompt" placeholder="请输入消息..." :disabled="isLoading"/>
              </el-col>
              <el-col :span="8">
                <el-button @click="sendChatRequest" :disabled="isLoading"
                           type="success" plain style="width: 100%">
                  {{ isLoading ? '发送中...' : '发送请求' }}
                </el-button>
              </el-col>
            </el-row>
          </div>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  background-color: var(--el-bg-color);
}

.chat-area {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #ccc;
}

.response-box {
  border: 1px dashed #0056b3;
  padding: 10px;
  margin-top: 10px;
  min-height: 100px;
}

.response-content {
  color: #333;
  white-space: pre-wrap;
}
</style>