<script setup lang="ts">
// 只需引入 BasicLayout
import BasicLayout from './layouts/BasicLayout.vue'
import { useLoginUserStore } from './stores/loginUser'
const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()
</script>

<template>
  <BasicLayout />
</template>
