<template>
  <div class="paywall-view" :class="{ 'is-purchased': isPurchased }">
    <div class="paywall-view-header">
      <div class="paywall-view-price">
        <span class="price-label">付费内容</span>
        <span class="price-amount">¥{{ price }}</span>
      </div>
      <div v-if="preview" class="paywall-view-preview">
        {{ preview }}
      </div>
    </div>
    
    <div v-if="!isPurchased" class="paywall-view-actions">
      <button class="purchase-button" @click="handlePurchase">
        立即购买
      </button>
    </div>
    
    <div v-else class="paywall-view-content">
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';

const props = defineProps<{
  price: string;
  preview?: string;
  contentId: string;
}>();

const isPurchased = ref(false);

const handlePurchase = async () => {
  try {
    // TODO: 调用支付接口
    const response = await fetch('/apis/api.plugin.halo.run/v1alpha1/plugins/paywall/purchase', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        contentId: props.contentId,
        price: props.price
      })
    });
    
    if (response.ok) {
      const result = await response.json();
      // 打开支付二维码
      window.open(result.qrCodeUrl, '_blank');
      // TODO: 轮询支付状态
    }
  } catch (error) {
    console.error('Purchase failed:', error);
  }
};
</script>

<style>
.paywall-view {
  margin: 1.5rem 0;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.paywall-view-header {
  padding: 1.5rem;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
}

.paywall-view-price {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.price-label {
  font-size: 0.875rem;
  color: #64748b;
}

.price-amount {
  font-size: 1.5rem;
  font-weight: 600;
  color: #0f172a;
}

.paywall-view-preview {
  margin-top: 0.75rem;
  font-size: 0.875rem;
  color: #64748b;
  line-height: 1.5;
}

.paywall-view-actions {
  padding: 1.5rem;
  display: flex;
  justify-content: center;
}

.purchase-button {
  padding: 0.75rem 2rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.purchase-button:hover {
  background: #2563eb;
}

.paywall-view-content {
  padding: 1.5rem;
}

.is-purchased .paywall-view-header {
  background: #f0fdf4;
  border-color: #bbf7d0;
}
</style> 