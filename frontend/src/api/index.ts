import axios from 'axios'
import { ElMessage } from "element-plus";
import type {
    ApiResponse,
    AuthStruct,
    FailureCallback,
    LoginResponse,
    SuccessCallback
} from "../interfaces/ApiTypes.ts";

// --- Constants & Defaults ---

const authItemName = 'access_token';

const defaultFailure: FailureCallback = (message, code, url) => {
    console.warn(`Request to ${url} failed ${code}: ${message}`);
    ElMessage.warning('Something went wrong: ' + message);
}

const defaultError: ErrorCallback = (err) => {
    console.error(err);
    ElMessage.error('Something went wrong, please contact the administrator.');
}

// --- Token Management ---

function takeAccessToken(): string | null {
    const str = localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName);
    if (!str) return null;

    try {
        const authObject: AuthStruct = JSON.parse(str);
        if (authObject.expire < Date.now()) {
            deleteAccessToken();
            ElMessage.warning('Your session has expired, please log in again.');
            return null;
        }
        return authObject.token;
    } catch (e) {
        deleteAccessToken();
        return null;
    }
}

function storeAccessToken(token: string, remember: boolean, expire: number): void {
    const authObject: AuthStruct = {
        token: token,
        expire: expire
    };
    const authString = JSON.stringify(authObject);
    if (remember) {
        localStorage.setItem(authItemName, authString);
    } else {
        sessionStorage.setItem(authItemName, authString);
        localStorage.removeItem(authItemName);
    }
}

function deleteAccessToken(): void {
    localStorage.removeItem(authItemName);
    sessionStorage.removeItem(authItemName);
}

function accessHeader(): Record<string, string> {
    const token = takeAccessToken();
    return token ? {
        'Authorization': 'Bearer ' + token
    } : {};
}

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

function logout(
    success: () => void,
    failure: FailureCallback = defaultFailure
): void {
    get<void>(
        '/api/auth/logout',
        () => {
            deleteAccessToken();
            ElMessage.success('Logout successfully!');
            success();
        },
        failure
    );
}

export { get, post, login, logout };