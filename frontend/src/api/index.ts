import axios from 'axios'
import { ElMessage } from "element-plus";
import type {
    ApiResponse,
    ChatResponse,
    FailureCallback,
    LoginResponse,
    SuccessCallback
} from "../interfaces/ApiTypes.ts";
import { deleteAccessToken, storeAccessToken, takeAccessToken } from "../utils/JwtUtils.ts";
import router from "../router";

// --- Constants & Defaults ---

const chatStreamURL = `${ axios.defaults.baseURL }/api/chat/stream`;

const defaultFailure: FailureCallback = (message, code, url) => {
    console.warn(`Request to ${ url } failed ${ code }: ${ message }`);
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
    axios.post<ApiResponse<T>>(url, data, {headers: header})
        .then(({data}) => {
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
    axios.get<ApiResponse<T>>(url, {headers: header})
        .then(({data}) => {
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
            ElMessage.success(`Login successfully! Welcome back, ${ data.username }`);
            success(data);
        }
    );
}

function logout(
    success: () => void,
    failure: FailureCallback = defaultFailure
): void {
    get<void>(
        '/api/auth/logout',
        () => {
            deleteAccessToken();
            ElMessage.success('Logout successfully!');
            router.push('/');
            success();
        },
        failure
    );
}

function chatRequest(
    prompt: string,
    success: SuccessCallback<ChatResponse>
): void {
    internalPost<ChatResponse>(
        '/api/chat/request',
        {
            modelId: 1,
            character: 1,
            input: prompt,
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

export { get, post, login, logout, chatRequest, chatStreamURL };