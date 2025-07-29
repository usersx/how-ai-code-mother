<template>
  <a-layout-header class="global-header">
    <div class="left">
      <img class="logo" src="@/assets/howmoon.png" alt="logo" />
      <span class="title">Howmoon AI零代码应用生成平台</span>
      <a-menu
        mode="horizontal"
        :selectedKeys="selectedKeys"
        @click="onMenuClick"
        style="flex:1"
      >
        <a-menu-item v-for="item in menuItems" :key="item.key">
          <router-link :to="item.path">{{ item.label }}</router-link>
        </a-menu-item>
      </a-menu>
    </div>
    <div class="right">
      <a-button type="primary">登录</a-button>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

// 当前高亮菜单项
const selectedKeys = ref<string[]>([route.path]);

const menuItems = [
  { key: '/', label: '首页', path: '/' },
  { key: '/about', label: '关于', path: '/about' }
];

// 菜单点击事件
function onMenuClick(e: any) {
  const key = e.key as string;
  selectedKeys.value = [key];
  if (key.startsWith('/')) {
    router.push(key);
  }
}

// 路由变化时同步高亮
let removeAfterEach: any;
onMounted(() => {
  removeAfterEach = router.afterEach((to) => {
    selectedKeys.value = [to.path];
  });
});
onUnmounted(() => {
  if (removeAfterEach) removeAfterEach();
});
</script>

<style scoped>
.global-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px #f0f1f2;
}
.left {
  display: flex;
  align-items: center;
}
.logo {
  width: 40px;
  height: 40px;
  margin-right: 12px;
}
.title {
  font-size: 20px;
  font-weight: bold;
  margin-right: 32px;
}
.right {
  display: flex;
  align-items: center;
}
</style> 