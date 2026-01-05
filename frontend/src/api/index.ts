import axios from 'axios'
import { ElMessage } from "element-plus";
import type {
    ApiResponse, ChatMessage,
    ChatResponse,
    FailureCallback, HistoryResponse,
    LoginResponse,
    SuccessCallback
} from "../interfaces/ApiTypes.ts";
import { deleteAccessToken, storeAccessToken, takeAccessToken } from "../utils/JwtUtils.ts";
import router from "../router";

// --- Constants & Defaults ---

const chatStreamURL = `/api/chat/stream`;

const defaultFailure: FailureCallback = (message, code, url) => {
    console.warn(`Request to ${url} failed ${code}: ${message}`);
    ElMessage.warning('Something went wrong: ' + message);
}

const defaultError: ErrorCallback = (err) => {
    console.error(err);
    ElMessage.error('Something went wrong, please contact the administrator.');
}

const accessHeader = (): Record<string, string> => {
    const token = takeAccessToken();
    return token ? {
        'Authorization': 'Bearer ' + token
    } : {};
};

// --- Internal Request Helpers ---

function internalPost<T>(
    url: string,
    data: any,
    header: Record<string, string>,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError
): void {
    axios.post<ApiResponse<T>>(url, data, { headers: header })
        .then(({ data }) => {
            if (data.code === 200) {
                success(data.data);
            } else {
                failure(data.message, data.code, url);
            }
        })
        .catch(err => error(err));
}

function internalGet<T>(
    url: string,
    header: Record<string, string>,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError
): void {
    axios.get<ApiResponse<T>>(url, { headers: header })
        .then(({ data }) => {
            if (data.code === 200) {
                success(data.data);
            } else {
                failure(data.message, data.code, url);
            }
        })
        .catch(err => error(err));
}

// --- Exported Functions ---

/**
 * Generic GET request
 * @param url API Endpoint
 * @param success Callback on success (code 200)
 * @param failure Callback on business failure (code != 200)
 * @param error Callback on network/system error
 */
function get<T = any>(
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError
): void {
    internalGet<T>(url, accessHeader(), success, failure, error);
}

/**
 * Generic POST request
 * @param url API Endpoint
 * @param data Request body
 * @param success Callback on success (code 200)
 * @param failure Callback on business failure (code != 200)
 * @param error Callback on network/system error
 */
function post<T = any>(
    url: string,
    data: any,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError
): void {
    internalPost<T>(url, data, accessHeader(), success, failure, error);
}

function login(
    username: string,
    password: string,
    remember: boolean,
    success: SuccessCallback<LoginResponse>
): void {
    internalPost<LoginResponse>(
        '/api/auth/login',
        {
            username: username,
            password: password
        },
        {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        (data) => {
            storeAccessToken(data.token, remember, data.expire);
            ElMessage.success(`Login successfully! Welcome back, ${data.username}`);
            success(data);
        }
    );
}

function logout(success: () => void): void {
    deleteAccessToken();
    ElMessage.success('Logout successfully!');
    router.push('/');
    success();
}

/**
 * 发送聊天请求 (修改版)
 * 将 prompt 改为 messages 列表
 */

function chatRequest(
    messages: ChatMessage[], // 接收完整上下文
    success: SuccessCallback<ChatResponse>
): void {
    // 构建符合后端要求的 payload
    // 去除 id 等前端专用字段，只保留 role 和 content
    const payloadMessages = messages.map(m => ({
        role: m.role,
        content: m.content
    }));

    internalPost<ChatResponse>(
        '/api/chat/request',
        {
            modelId: 1,
            characterId: 1,
            messages: payloadMessages, // 这里变成了列表
            uuid: ''
        },
        {
            'Authorization': 'Bearer ' + takeAccessToken()
        },
        (data) => {
            success(data);
        }
    );
}

/**
 * 获取历史记录
 */
function fetchHistory(
    limit: number,
    success: SuccessCallback<HistoryResponse>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError
): void {
    // 假设后端接口为 POST /api/chat/history
    post<HistoryResponse>(
        '/api/chat/history',
        { limit },
        success,
        failure,
        error
    );
}

export { get, post, login, logout, chatRequest, fetchHistory, chatStreamURL };