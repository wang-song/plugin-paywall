<template>
  <node-view-wrapper>
    <div 
      class="paywall-content" 
      ref="paywallRef"
      @click.stop="handleContainerClick"
    >
      <div class="paywall-header">
        <div class="paywall-info" v-if="!isEditing">
          <span class="paywall-price">付费金额 ¥{{ node.attrs.price }}</span>
          <span v-if="node.attrs.preview" class="paywall-preview">{{ node.attrs.preview }}</span>
        </div>
        <div class="paywall-form" v-else>
          <div class="form-group">
            <label class="form-label">付费金额</label>
            <div class="form-input-group">
              <span class="form-addon">¥</span>
              <input
                v-model="editingPrice"
                type="number"
                min="0.01"
                step="0.01"
                class="form-input"
                placeholder="请输入付费金额"
              />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">内容预览</label>
            <textarea
              v-model="editingPreview"
              rows="1"
              class="form-input"
              placeholder="请输入内容预览（可选）"
            ></textarea>
          </div>
        </div>
        <div class="paywall-actions">
          <button class="paywall-button delete" @click.stop="deleteNode" title="删除付费内容">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 6h18"></path>
              <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
              <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
      <node-view-content />
    </div>
  </node-view-wrapper>
</template>

<script lang="ts" setup>
import { NodeViewWrapper, NodeViewContent, type NodeViewProps } from "@halo-dev/richtext-editor";
import { ref, watch, onMounted, onUnmounted } from "vue";

const props = defineProps<NodeViewProps & {
  node: {
    attrs: {
      price: string;
      preview: string;
    };
  };
}>();

const isEditing = ref(false);
const editingPrice = ref(props.node.attrs.price);
const editingPreview = ref(props.node.attrs.preview);
const paywallRef = ref<HTMLElement | null>(null);

const handleEdit = () => {
  editingPrice.value = props.node.attrs.price;
  editingPreview.value = props.node.attrs.preview;
  isEditing.value = true;
};

// 监听价格变化
watch(editingPrice, (newPrice) => {
  if (newPrice && Number(newPrice) > 0) {
    props.updateAttributes({
      price: newPrice,
      preview: editingPreview.value
    });
  }
});

// 监听预览内容变化
watch(editingPreview, (newPreview) => {
  if (editingPrice.value && Number(editingPrice.value) > 0) {
    props.updateAttributes({
      price: editingPrice.value,
      preview: newPreview
    });
  }
});

// 处理容器点击
const handleContainerClick = () => {
  if (!isEditing.value) {
    editingPrice.value = props.node.attrs.price;
    editingPreview.value = props.node.attrs.preview;
    isEditing.value = true;
  }
};

// 修改 handleClickOutside 函数
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  const element = paywallRef.value;
  
  if (isEditing.value && element && !element.contains(target)) {
    // 如果价格有效，则保存并退出编辑模式
    if (editingPrice.value && Number(editingPrice.value) > 0) {
      props.updateAttributes({
        price: editingPrice.value,
        preview: editingPreview.value
      });
      isEditing.value = false;
    }
  }
};

// 挂载时添加事件监听器
onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
});

// 卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});

</script>

<style>
/* 优化付费内容容器样式 */
.paywall-content {
  position: relative;
  padding: 1.25rem;
  background: linear-gradient(to bottom, #ffffff, #f8fafc);
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05),
              0 0 0 1px rgba(0, 0, 0, 0.02);
  cursor: pointer; /* 添加指针样式 */
  transition: all 0.2s ease;
}

.paywall-content:hover {
  border-color: #1a73e8;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.1);
}

/* 编辑状态样式 */
.paywall-content.editing {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.paywall-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.paywall-info {
  flex: 1;
}

.paywall-price {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
  color: #1a73e8;
  font-size: 1rem;
  background: linear-gradient(135deg, rgba(26, 115, 232, 0.1) 0%, rgba(66, 133, 244, 0.1) 100%);
  padding: 0.5rem 1.25rem;
  border-radius: 9999px;  /* 完全圆角 */
  box-shadow: 0 2px 4px rgba(26, 115, 232, 0.1);
  border: 1px solid rgba(26, 115, 232, 0.2);
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
}

.paywall-price:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(26, 115, 232, 0.15);
}

.paywall-price::before {
  content: "¥";
  margin-right: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  opacity: 0.8;
}

/* 优化预览文本样式 */
.paywall-preview {
  display: block;
  margin-top: 0.75rem;
  color: #6b7280;
  font-size: 0.875rem;
  line-height: 1.5;
  padding: 0.5rem 0.75rem;
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 0.5rem;
  border-left: 3px solid #1a73e8;
}

.paywall-actions {
  display: flex;
  gap: 0.5rem;
}

.paywall-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.paywall-button.edit {
  display: none;
}

.paywall-button.delete {
  color: #dc2626;
  border: 1px solid #dc2626;
}

.paywall-button.delete:hover {
  color: #fff;
  background-color: #dc2626;
}

.paywall-form {
  flex: 1;
  margin-right: 1rem;
}

.form-group {
  margin-bottom: 0.75rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

/* 优化标签样式 */
.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
  letter-spacing: 0.025em;
}

/* 优化输入框样式 */
.form-input-group {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 9999px;
  border: 1px solid #e5e7eb;
  padding: 0.25rem;
  transition: all 0.3s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.form-input-group:focus-within {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}


.form-addon {
  padding: 0.5rem 0.75rem;
  color: #1a73e8;
  font-weight: 600;
  font-size: 1rem;
  background-color: rgba(26, 115, 232, 0.1);
  border: none;
  border-radius: 9999px;
  margin-right: 0.25rem;
}
.form-input-group .form-input {
  border: none;
  border-radius: 9999px;
  font-size: 1rem;
  font-weight: 500;
  color: #1a73e8;
  padding: 0.5rem 0.75rem;
  width: 100%;
  background: transparent;
}
.form-input-group .form-input:focus {
  outline: none;
}
/* 优化预览文本框样式 */
textarea.form-input {
  border-radius: 1rem !important;
  resize: none;
  padding: 0.75rem 1rem !important;
  line-height: 1.5;
  background-color: white !important;
  border: 1px solid #e5e7eb !important;
  transition: all 0.3s ease;
}

.form-input {
  display: block;
  width: 100%;
  padding: 0.375rem 0.75rem;
  color: #374151;
  background-color: #fff;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  transition: border-color 0.2s;
}



.form-input:focus {
  outline: none;
  border-color: #1a73e8;
}

.form-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.form-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.form-button.save {
  color: #059669;
  border: 1px solid #059669;
}

.form-button.save:hover {
  color: #fff;
  background-color: #059669;
}

.form-button.cancel {
  color: #6b7280;
  border: 1px solid #6b7280;
}

.form-button.cancel:hover {
  color: #fff;
  background-color: #6b7280;
}
</style> 
