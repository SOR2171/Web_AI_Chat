export interface JwtAuth {
    token: string;
    expire: number;
}

export interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

export interface LoginResponse {
    token: string;
    expire: number;
    username: string;
}

export interface ChatResponse {
    sessionId: string
}

export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    id?: string;
}

export interface HistoryRequest {
    limit: number;
}

// 假设后端历史记录返回的格式
export interface HistoryResponse {
    list: ChatMessage[];
}

// 更新 ChatRequest Payload 接口 (仅供参考，axios调用时使用any或此接口)
export interface ChatRequestPayload {
    modelId: number;
    characterId: number;
    messages: ChatMessage[];
    uuid: string;
}


export type SuccessCallback<T = any> = (data: T) => void;
export type FailureCallback = (message: string, code: number, url: string) => void;
export type ErrorCallback = (err: any) => void;