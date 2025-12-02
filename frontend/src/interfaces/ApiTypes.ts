interface JwtAuth {
    token: string;
    expire: number;
}

interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

interface LoginResponse {
    token: string;
    expire: number;
    username: string;
}

type SuccessCallback<T = any> = (data: T) => void;
type FailureCallback = (message: string, code: number, url: string) => void;
type ErrorCallback = (err: any) => void;

export type {
    JwtAuth,
    ApiResponse,
    LoginResponse,
    SuccessCallback,
    FailureCallback,
    ErrorCallback
}