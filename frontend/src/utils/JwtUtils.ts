import type { JwtAuth } from "../interfaces/ApiTypes.ts";
import { ElMessage } from "element-plus";
import router from "../router";

const authItemName = 'access_token';

function takeAccessToken(): string | null {
    const str = localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName);
    if (!str) return null;

    try {
        const authObject: JwtAuth = JSON.parse(str);
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
    const authObject: JwtAuth = {
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

function loginPageTokenInit(): void {
    const storedToken = takeAccessToken();
    if (storedToken) {
        router.push('/home')
    }
}

function homePageTokenInit(): void {
    const storedToken = takeAccessToken();
    if (!storedToken) {
        router.push('/').then(() => {
            ElMessage.warning('Have not logged in yet, please log in.');
        });
    }
}

export {
    takeAccessToken,
    storeAccessToken,
    deleteAccessToken,
    loginPageTokenInit,
    homePageTokenInit
}