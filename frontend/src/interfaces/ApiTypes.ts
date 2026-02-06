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

export type SuccessCallback<T = any> = (data: T) => void;
export type FailureCallback = (message: string, code: number, url: string) => void;