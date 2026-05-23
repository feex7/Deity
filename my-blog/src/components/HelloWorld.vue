<script setup>
import { ref } from 'vue'

const count = ref(0)
</script>

<template>
  <section id="center">
    <div class="avatar-container">
      <img 
        src="/touxiang.jpg" 
        class="avatar" 
        alt="头像" 
      />
    </div>
  </section>
</template>

<style scoped>
#center {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.avatar-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .avatar {
    width: 150px;
    height: 150px;
  }
}
</style>
