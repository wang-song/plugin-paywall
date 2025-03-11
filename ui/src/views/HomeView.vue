<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Toast, VModal } from "@halo-dev/components";
import axios from "axios";
import RiSaveLine from "~icons/ri/save-line";
import RiSettings2Line from "~icons/ri/settings-2-line";
import RiTestTubeLine from "~icons/ri/test-tube-line";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true
});

interface VmqConfig {
  serverUrl: string;
  key: string;
  notifyUrl: string;
  isHttps: boolean;
}

interface TestResponse {
  status: string;
  message?: string;
}

const formState = ref<VmqConfig>({
  serverUrl: "",
  key: "",
  notifyUrl: "",
  isHttps: false,
});

const loading = ref(false);
const testing = ref(false);
const showTestDialog = ref(false);
const showProgressDialog = ref(false);

// 获取配置
onMounted(async () => {
  try {
    const { data } = await apiClient.get(
      `/apis/plugin-paywall.halo.run/v1alpha1/setting/getSettings`
    );
    if (data.spec) {
      formState.value = {
        serverUrl: data.spec.serverUrl || "",
        key: data.spec.key || "",
        notifyUrl: data.spec.notifyUrl || "",
        isHttps: data.spec.https || false
      };
    }
  } catch (e) {
    console.error("获取配置失败:", e);
    Toast.error("获取配置失败");
  }
});

// 保存配置
const handleSubmit = async () => {
  loading.value = true;
  const settingString = JSON.stringify({
    serverUrl: formState.value.serverUrl,
    key: formState.value.key,
    notifyUrl: formState.value.notifyUrl,
    isHttps: formState.value.isHttps
  });
  
  try {
    await apiClient.post(
      `/apis/plugin-paywall.halo.run/v1alpha1/setting/saveSettings/${encodeURIComponent(settingString)}`
    );
    Toast.success("保存成功");
  } catch (e) {
    console.error("保存失败:", e);
    Toast.error("保存失败");
  } finally {
    loading.value = false;
  }
};

// 测试连接按钮点击处理
const handleTestClick = () => {
  console.log("测试按钮被点击");
  if (!formState.value.serverUrl || !formState.value.key) {
    Toast.error("请先填写服务器地址和通信密钥");
    return;
  }
  showTestDialog.value = true;
};

// 确认测试
const handleConfirmTest = async () => {
  const settingString = JSON.stringify({
    serverUrl: formState.value.serverUrl,
    key: formState.value.key,
    notifyUrl: formState.value.notifyUrl,
    isHttps: formState.value.isHttps
  });
  

  showTestDialog.value = false;
  showProgressDialog.value = true;
  testing.value = true;
  let timeoutId: number | undefined;

  try {
    // 设置超时计时器
    const timeoutPromise = new Promise<never>((_, reject) => {
      timeoutId = window.setTimeout(() => {
        reject(new Error("连接超时"));
      }, 20000); // 20秒超时
    });

    // 发起测试请求
    const testPromise = apiClient.post<TestResponse>(
      `/apis/plugin-paywall.halo.run/v1alpha1/setting/testSettings/${encodeURIComponent(settingString)}`,
      formState.value
    );

    // 使用 Promise.race 来处理超时情况
    const response = await Promise.race([testPromise, timeoutPromise]);
    
    if (timeoutId) {
      window.clearTimeout(timeoutId);
    }
    showProgressDialog.value = false;

    
    if (response.data.status !== "SUCCESS") {
      Toast.error(response.data.message || "连接测试失败");
    } else {
      Toast.success("连接测试成功");
    }
  } catch (e: any) {
    showProgressDialog.value = false;
    
    if (e.message === "连接超时") {
      Toast.error("连接测试超时，请检查服务器地址是否正确");
    } else {
      console.error("测试连接失败:", e);
      Toast.error("测试连接失败");
    }
  } finally {
    testing.value = false;
    if (timeoutId) {
      window.clearTimeout(timeoutId);
    }
  }
};

// 取消测试
const handleCancelTest = () => {
  console.log("取消测试");
  showTestDialog.value = false;
};
</script>

<template>
  <!-- 移除外层的居中容器，直接使用左对齐布局 -->
  <div class="page-container">
    <!-- 标题区域 -->
    <div class="page-header">
      <RiSettings2Line class="icon" />
      <h1 class="title">V免签配置</h1>
    </div>

    <!-- 表单区域 -->
    <div class="page-content">
      <form @submit.prevent="handleSubmit">
        <!-- 服务器配置区块 -->
        <section class="content-section">
          <h2 class="section-title">服务器配置</h2>
          <div class="form-group">
            <label for="serverUrl">服务器地址</label>
            <input
              id="serverUrl"
              v-model="formState.serverUrl"
              type="text"
              class="form-input"
              placeholder="请输入V免签服务器地址：vmq.test.com:8080"
              required
            />
            <div class="form-help">V免签服务端的访问地址，不要添加http://或https://</div>
          </div>

          <div class="form-group checkbox-group">
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="formState.isHttps"
                class="checkbox-input"
              />
              <span class="checkbox-text">使用 HTTPS 协议</span>
            </label>
            <div class="form-help">如果您的服务器支持 HTTPS，请勾选此项，如果使用的 HTTP 协议，请取消勾选。</div>
          </div>
        </section>

        <!-- 安全配置区块 -->
        <section class="content-section">
          <h2 class="section-title">安全配置</h2>
          <div class="form-group">
            <label for="key">通信密钥</label>
            <input
              id="key"
              v-model="formState.key"
              type="password"
              class="form-input"
              placeholder="请输入V免签通信密钥"
              required
            />
            <div class="form-help">在V免签后台系统设置中配置的通信密钥</div>
          </div>
        </section>

        <!-- 回调配置区块 -->
        <section class="content-section">
          <h2 class="section-title">回调配置</h2>
          <div class="form-group">
            <label for="notifyUrl">回调地址</label>
            <input
              id="notifyUrl"
              v-model="formState.notifyUrl"
              type="text"
              class="form-input"
              placeholder="请输入回调地址"
            />
            <div class="form-help">暂时没用，随意填写</div>
          </div>
        </section>

        <!-- 操作按钮区域 -->
        <section class="content-section action-section">
          <div class="button-group">
            <button type="submit" class="btn btn-primary" :disabled="loading">
              <RiSaveLine class="icon" />
              {{ loading ? "保存中..." : "保存配置" }}
            </button>
            <button 
              type="button" 
              class="btn btn-secondary" 
              :disabled="testing"
              @click="handleTestClick"
            >
              <RiTestTubeLine class="icon" />
              {{ testing ? "测试中..." : "测试连接" }}
            </button>
          </div>
        </section>
      </form>
    </div>

    <!-- 确认对话框 -->
    <VModal
      v-if="showTestDialog"
      v-model:show="showTestDialog"
      title="测试连接"
      :width="480"
    >
      <div class="test-confirm-dialog">
        <div class="test-info">
          <div class="info-icon">
            <RiTestTubeLine class="icon" />
          </div>
          <div class="info-content">
            <p class="info-title">测试说明</p>
            <p class="info-desc">
              开始测试后会在VMQ服务器上创建一个<span class="highlight">0.01元</span>的订单，
              然后关闭该订单，以验证通信是否正常。
            </p>
            <p class="info-tip">测试通过后，别忘记保存配置！</p>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="handleCancelTest">取消</button>
          <button class="btn btn-primary" @click="handleConfirmTest">开始测试</button>
        </div>
      </template>
    </VModal>

    <!-- 进度条对话框 -->
    <VModal
      v-if="showProgressDialog"
      v-model:show="showProgressDialog"
      title="测试连接中"
      :width="400"
      :closable="false"
    >
      <div class="test-progress">
        <div class="progress-bar">
          <div class="progress-inner"></div>
        </div>
        <div class="progress-text">正在测试连接，请稍候...</div>
      </div>
    </VModal>
  </div>
</template>

<style lang="scss" scoped>
.page-container {
  padding: 1.5rem 2rem;
  background-color: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  
  .icon {
    font-size: 1.5rem;
    color: #3b82f6;
  }

  .title {
    font-size: 1.5rem;
    font-weight: 600;
    color: #1f2937;
  }
}

.page-content {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  width: 800px;  /* 设置固定宽度 */
}

.content-section {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }

  &.action-section {
    background-color: #f9fafb;
    border-radius: 0 0 0.5rem 0.5rem;
  }
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;

  &::before {
    content: "";
    display: block;
    width: 3px;
    height: 1rem;
    background-color: #3b82f6;
    border-radius: 2px;
  }
}

.form-group {
  margin-bottom: 1.5rem;

  &:last-child {
    margin-bottom: 0;
  }

  label {
    display: block;
    font-size: 0.875rem;
    font-weight: 500;
    color: #374151;
    margin-bottom: 0.5rem;
  }

  .form-input {
    width: 100%;
    padding: 0.625rem 0.875rem;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    transition: all 0.2s;

    &:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    &::placeholder {
      color: #9ca3af;
    }
  }

  .form-help {
    margin-top: 0.375rem;
    font-size: 0.75rem;
    color: #6b7280;
  }
}

.checkbox-group {
  margin-top: 0.5rem;
  
  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
    user-select: none;
  }

  .checkbox-input {
    width: 1rem;
    height: 1rem;
    border-radius: 0.25rem;
    border: 1.5px solid #d1d5db;
    cursor: pointer;
    transition: all 0.2s;

    &:checked {
      background-color: #3b82f6;
      border-color: #3b82f6;
    }

    &:focus {
      outline: none;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
  }

  .checkbox-text {
    font-size: 0.875rem;
    color: #374151;
  }
}

.button-group {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }

  .icon {
    font-size: 1.25rem;
  }
}

.btn-primary {
  background-color: #3b82f6;
  color: white;

  &:hover:not(:disabled) {
    background-color: #2563eb;
  }

  &:focus {
    outline: none;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.3);
  }
}

.btn-secondary {
  background-color: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;

  &:hover:not(:disabled) {
    background-color: #e5e7eb;
  }

  &:focus {
    outline: none;
    box-shadow: 0 0 0 3px rgba(55, 65, 81, 0.1);
  }
}

.test-confirm-dialog {
  padding: 1rem;
}

.test-info {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  background-color: #f8fafc;
  border-radius: 0.5rem;
  padding: 1rem;
  border: 1px solid #e5e7eb;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  background-color: #eff6ff;
  border-radius: 0.5rem;
  color: #3b82f6;
  flex-shrink: 0;

  .icon {
    font-size: 1.25rem;
  }
}

.info-content {
  flex: 1;
  
  .info-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #374151;
    margin-bottom: 0.5rem;
  }

  .info-desc {
    font-size: 0.875rem;
    color: #4b5563;
    line-height: 1.5;
    margin-bottom: 0.75rem;

    .highlight {
      color: #3b82f6;
      font-weight: 600;
      padding: 0 0.25rem;
    }
  }

  .info-tip {
    font-size: 0.875rem;
    color: #dc2626;
    font-weight: 500;
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
  margin-top: 1rem;
}

.test-progress {
  padding: 1rem 0;

  .progress-bar {
    width: 100%;
    height: 4px;
    background-color: #e5e7eb;
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: 1rem;
  }

  .progress-inner {
    width: 30%;
    height: 100%;
    background-color: #3b82f6;
    border-radius: 2px;
    animation: progress 1.5s ease-in-out infinite;
  }

  .progress-text {
    text-align: center;
    font-size: 0.875rem;
    color: #6b7280;
  }
}

@keyframes progress {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(400%);
  }
}
</style>
