// src/utils/chatRequest.ts

// 模拟从 Pinia 或 localStorage 获取 Token
const getToken = () => localStorage.getItem('jwt_token') || '';

interface StreamOptions {
    url: string;
    body: any;
    onMessage: (text: string) => void;
    onError: (err: any) => void;
    onFinish: () => void;
}

export const fetchStream = async ({ url, body, onMessage, onError, onFinish }: StreamOptions) => {
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}` // 重点：在此注入 JWT
            },
            body: JSON.stringify(body),
        });

        if (!response.ok) {
            // 处理 401 Token 过期等错误
            if (response.status === 401) {
                throw new Error('Token Expired');
            }
            throw new Error(response.statusText);
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) throw new Error('No reader available');

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            // 解码二进制流
            const chunk = decoder.decode(value, { stream: true });
            // 注意：这里需要根据你后端的返回格式解析（可能是 SSE 格式 data: {...}）
            // 这里假设后端直接返回文本片段
            onMessage(chunk);
        }

        onFinish();
    } catch (error) {
        onError(error);
    }
};