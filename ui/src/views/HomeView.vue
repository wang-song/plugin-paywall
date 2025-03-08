<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Toast } from "@halo-dev/components";
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
  isHttps: boolean;  // 添加 isHttps 字段
}

const formState = ref<VmqConfig>({
  serverUrl: "",
  key: "",
  notifyUrl: "",
  isHttps: false,  // 添加默认值
});

const loading = ref(false);
const testing = ref(false);

onMounted(async () => {
  try {
    // `/apis/plugin-paywall.halo.run/v1alpha1/paywall/purchase/${contentId}`, {
    
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

const testConnection = async () => {
  if (!formState.value.serverUrl || !formState.value.key) {
    Toast.error("请先填写服务器地址和通信密钥");
    return;
  }

  testing.value = true;
  try {
    const response = await apiClient.post(
      `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-vmq/test-connection`,
      formState.value
    );
    if (response.data.success) {
      Toast.success("连接测试成功");
    } else {
      Toast.error(response.data.message || "连接测试失败");
    }
  } catch (e) {
    console.error("测试连接失败:", e);
    Toast.error("测试连接失败");
  } finally {
    testing.value = false;
  }
};
</script>

<template>
  <div class="vmq-settings">
    <div class="settings-header">
      <RiSettings2Line class="icon" />
      <h1 class="title">V免签配置</h1>
    </div>

    <div class="settings-form">
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="serverUrl">服务器地址</label>
          <input
            id="serverUrl"
            v-model="formState.serverUrl"
            type="text"
            class="form-input"
            placeholder="请输入V免签服务器地址，如：http://your-vmq-server.com"
            required
          />
          <div class="form-help">V免签服务端的访问地址，需要确保Halo服务器可以访问</div>
        </div>

        <!-- 添加 HTTPS 复选框 -->
        <div class="form-group checkbox-group">
          <label class="checkbox-label">
            <input
              type="checkbox"
              v-model="formState.isHttps"
              class="checkbox-input"
            />
            <span class="checkbox-text">使用 HTTPS 协议</span>
          </label>
          <div class="form-help">如果您的服务器支持 HTTPS，请勾选此项</div>
        </div>

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

        <div class="form-group">
          <label for="notifyUrl">回调地址</label>
          <input
            id="notifyUrl"
            v-model="formState.notifyUrl"
            type="text"
            class="form-input"
            placeholder="请输入支付成功后的回调通知地址"
            required
          />
          <div class="form-help">支付成功后的回调通知地址，需要确保V免签服务器可以访问</div>
        </div>

        <div class="button-group">
          <button type="submit" class="btn btn-primary" :disabled="loading">
            <RiSaveLine class="icon" />
            {{ loading ? "保存中..." : "保存配置" }}
          </button>
          <button 
            type="button" 
            class="btn btn-secondary" 
            :disabled="testing"
            @click="testConnection"
          >
            <RiTestTubeLine class="icon" />
            {{ testing ? "测试中..." : "测试连接" }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.checkbox-group {
  margin-top: -0.5rem;  // 调整与上方输入框的间距
  
  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
  }

  .checkbox-input {
    width: 1rem;
    height: 1rem;
    border-radius: 0.25rem;
    border: 1px solid #d1d5db;
    cursor: pointer;

    &:checked {
      background-color: #3b82f6;
      border-color: #3b82f6;
    }
  }

  .checkbox-text {
    font-size: 0.875rem;
    color: #374151;
  }
}
.vmq-settings {
  padding: 2rem;
  background-color: #f8fafc;
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 2rem;

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

.settings-form {
  background-color: white;
  padding: 2rem;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  max-width: 600px;
}

.form-group {
  margin-bottom: 1.5rem;

  label {
    display: block;
    font-size: 0.875rem;
    font-weight: 500;
    color: #374151;
    margin-bottom: 0.5rem;
  }

  .form-input {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    transition: all 0.2s;

    &:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 2px rgb(59 130 246 / 0.1);
    }

    &::placeholder {
      color: #9ca3af;
    }
  }

  .form-help {
    margin-top: 0.25rem;
    font-size: 0.75rem;
    color: #6b7280;
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
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.375rem;
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

  &:disabled {
    background-color: #93c5fd;
  }
}

.btn-secondary {
  background-color: #e5e7eb;
  color: #374151;

  &:hover:not(:disabled) {
    background-color: #d1d5db;
  }

  &:disabled {
    background-color: #f3f4f6;
    color: #9ca3af;
  }
}
</style>
