# FQ-OAuth Frontend

## GitHub 登录配置

1. 在 GitHub 创建 OAuth App。
2. 将 Authorization callback URL 配置为：

   ```text
   http://127.0.0.1:5173/github/callback
   ```

3. 复制 `.env.example` 为 `.env.local`，填写 GitHub Client ID：

   ```bash
   cp .env.example .env.local
   ```

   ```dotenv
   VITE_GITHUB_CLIENT_ID=真实的GitHub_Client_ID
   ```

4. 在 FQ-OAuth 后端配置同一个 Client ID 和 Client Secret：

   ```bash
   export FQ_GITHUB_CLIENT_ID=真实的GitHub_Client_ID
   export FQ_GITHUB_CLIENT_SECRET=真实的GitHub_Client_Secret
   ```

5. 重启 Vite 和 FQ-OAuth。Vite 只在启动时读取 `VITE_*` 环境变量。

GitHub 登录成功后，后端会用 GitHub 返回的已验证邮箱匹配本地 `t_users.email`。匹配不到时不会自动创建用户，而是提示联系管理员。
