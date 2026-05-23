<script setup>
import NavBar from './components/NavBar.vue'
import { onMounted } from 'vue'
import { initArticles } from './store/articles.js'

onMounted(() => {
  initArticles()
})
</script>

<template>
  <NavBar />
  <router-view />
</template>