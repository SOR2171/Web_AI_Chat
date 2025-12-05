## Web_AI_Chat

English / [简体中文](./README_CN.md)

> ~~*It's really frustrating that you can't use websocket with jakarta.servlet.*~~

---

## Introduction

This project is a web-based AI chat application that allows users to interact with AI models through a web interface.
It is based on another repository [SillyTavern-Extension-ChatBridge](https://github.com/AyeeMinerva/SillyTavern-Extension-ChatBridge).
If you want to set up your own AI chat server, please refer to that repository.
This project mainly focuses on providing a web interface and resolve the user's data with database.

If you don't want to use SillyTavern, you can also use other AI chat that support similar APIs, like:

```python
# Example for OpenAI API in Python
client = OpenAI(
    api_key="your-user-api-key",
    base_url="http://base-url/v1"
)
```

But if you use SillyTavern, you can straightly use all the characters in SillyTavern.

## Features

- User registration and login
- Chat history management
- Support for multiple AI chat models
- ~~ChatBridge seems not supporting multi-character~~

---

## Setup Instructions

This project is based on [Springboot-Vue3_Test](https://github.com/SOR2171/Springboot-Vue3_Test), so the steps are basically the same.

#### Differences

1. Database: In `application-example.yaml` and `database.sql`, the database name is changed to `web_ai_chat`
2. Frontend: You need to find a background picture and put it in `./frontend/src/assets/welcome-image.png`.
