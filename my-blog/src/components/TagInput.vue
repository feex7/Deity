<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const tags = computed(() => {
  if (!props.modelValue) return []
  return props.modelValue.split(',').map(t => t.trim()).filter(Boolean)
})

const inputText = ref('')
const inputRef = ref(null)

const addTag = (text) => {
  const val = text.trim()
  if (!val) return
  if (tags.value.includes(val)) {
    inputText.value = ''
    return
  }
  const newTags = [...tags.value, val]
  emit('update:modelValue', newTags.join(','))
  inputText.value = ''
}

const removeTag = (index) => {
  const newTags = tags.value.filter((_, i) => i !== index)
  emit('update:modelValue', newTags.join(','))
}

const onKeydown = (e) => {
  if (e.key === 'Enter') {
    e.preventDefault()
    addTag(inputText.value)
    return
  }
  if (e.key === ',' || e.key === ' ') {
    e.preventDefault()
    if (inputText.value.trim()) {
      addTag(inputText.value)
    }
    return
  }
  if (e.key === 'Backspace' && !inputText.value && tags.value.length > 0) {
    removeTag(tags.value.length - 1)
    return
  }
}

const onBlur = () => {
  if (inputText.value.trim()) {
    addTag(inputText.value)
  }
}

const focusInput = () => {
  inputRef.value?.focus()
}
</script>

<template>
  <div class="tag-input" @click="focusInput">
    <span v-for="(tag, i) in tags" :key="i" class="tag-item">
      {{ tag }}
      <button class="tag-remove" @click.stop="removeTag(i)">&times;</button>
    </span>
    <input
      ref="inputRef"
      v-model="inputText"
      class="tag-inner-input"
      :placeholder="tags.length === 0 ? '输入标签，逗号/空格/回车分隔' : ''"
      @keydown="onKeydown"
      @blur="onBlur"
    />
  </div>
</template>

<style scoped>
.tag-input {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  min-height: 42px;
  align-items: center;
  cursor: text;
  transition: border-color 0.2s;
  margin-bottom: 12px;
  box-sizing: border-box;
}

.tag-input:focus-within {
  border-color: rgba(0, 255, 128, 0.3);
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(0, 255, 128, 0.08);
  border: 1px solid rgba(0, 255, 128, 0.15);
  border-radius: 4px;
  font-size: 12px;
  color: rgba(0, 255, 128, 0.8);
  white-space: nowrap;
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  font-size: 10px;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  transition: all 0.15s;
}

.tag-remove:hover {
  background: rgba(255, 68, 68, 0.25);
  color: #ff4444;
}

.tag-inner-input {
  flex: 1;
  min-width: 100px;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 13px;
  outline: none;
  padding: 2px 0;
}

.tag-inner-input::placeholder {
  color: rgba(255, 255, 255, 0.25);
}
</style>