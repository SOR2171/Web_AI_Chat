## 网页AI聊天

[English](./README.md) / 简体中文

> ~~*基于 jakarta.servlet 的网络用不了 websocket 真是太痛苦了*~~

---

## 简介

本项目是一个基于网页的 AI 聊天应用程序，允许用户通过网页界面与 AI 模型进行交互。
它基于另一个仓库 [SillyTavern-Extension-ChatBridge](https://github.com/AyeeMinerva/SillyTavern-Extension-ChatBridge)。
如果你想搭建自己的 AI 聊天服务器，请参考该仓库。
本项目主要侧重于提供一个网页界面，并结合数据库来管理用户数据。

如果你不想使用 SillyTavern，也可以使用其他支持类似 API 的 AI 聊天服务 URL，例如：

```python
# Python 中使用 OpenAI API 的示例
client = OpenAI(
    api_key="your-user-api-key",
    base_url="http://base-url/v1"
)
```

但如果你使用 SillyTavern，则可以直接使用 SillyTavern 中的所有角色。

## 功能特性

- 用户注册与登录
- 聊天历史记录管理
- 支持多种 AI 聊天模型(目前硬编码在后端)
- ~~ChatBridge 似乎不支持多角色~~

---

## 项目搭建

本项目基于 [Springboot-Vue3_Test](https://github.com/SOR2171/Springboot-Vue3_Test)，因此步骤基本相同。

#### 不同之处

1. 数据库：在 `application-example.yaml` 和 `database.sql` 文件中，数据库名称已更改为 `web_ai_chat`。
2. 前端：你需要找到一张背景图片，并将其放置在 `./frontend/src/assets/welcome-image.png` 路径下。