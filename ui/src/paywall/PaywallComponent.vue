<template>
  <node-view-wrapper class="paywall-content">
    <div class="paywall-header">
      <div class="paywall-info" v-if="!isEditing">
        <span class="paywall-price">付费内容 ¥{{ node.attrs.price }}</span>
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
            rows="2"
            class="form-input"
            placeholder="请输入内容预览（可选）"
          ></textarea>
        </div>
        <div class="form-actions">
          <button class="form-button save" @click="handleSave">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
          </button>
          <button class="form-button cancel" @click="isEditing = false">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="paywall-actions">
        <button v-if="!isEditing" class="paywall-button edit" @click="handleEdit" title="编辑付费内容">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
          </svg>
        </button>
        <button class="paywall-button delete" @click="deleteNode" title="删除付费内容">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18"></path>
            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </div>
    <node-view-content />
  </node-view-wrapper>
</template>

<script lang="ts" setup>
import { NodeViewWrapper, NodeViewContent, type NodeViewProps } from "@halo-dev/richtext-editor";
import { ref } from "vue";

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

const handleEdit = () => {
  editingPrice.value = props.node.attrs.price;
  editingPreview.value = props.node.attrs.preview;
  isEditing.value = true;
};

const handleSave = () => {
  if (!editingPrice.value || Number(editingPrice.value) <= 0) {
    return;
  }
  props.updateAttributes({
    price: editingPrice.value,
    preview: editingPreview.value
  });
  isEditing.value = false;
};
</script>

<style>
.paywall-content {
  position: relative;
  padding: 1rem;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 0.375rem;
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
  font-weight: 700;
  color: #1a73e8;
  font-size: 1.25rem;
  background-color: rgba(26, 115, 232, 0.1);
  padding: 0.375rem 1rem;
  border-radius: 0.5rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.paywall-price::before {
  content: "¥";
  margin-right: 0.375rem;
  font-size: 1rem;
  font-weight: 600;
  opacity: 0.9;
}

.paywall-preview {
  display: block;
  margin-top: 0.5rem;
  color: #6b7280;
  font-size: 0.875rem;
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
  color: #1a73e8;
  border: 1px solid #1a73e8;
}

.paywall-button.edit:hover {
  color: #fff;
  background-color: #1a73e8;
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

.form-label {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.form-input-group {
  display: flex;
  align-items: center;
}

.form-addon {
  padding: 0.375rem 0.75rem;
  color: #1a73e8;
  font-weight: 600;
  font-size: 1.125rem;
  background-color: rgba(26, 115, 232, 0.1);
  border: 1px solid #1a73e8;
  border-right: none;
  border-radius: 0.375rem 0 0 0.375rem;
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

.form-input-group .form-input {
  border-radius: 0 0.375rem 0.375rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a73e8;
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