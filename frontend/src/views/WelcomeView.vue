<script setup lang="ts">

import { onMounted } from "vue";
import { loginPageTokenInit } from "../utils/JwtUtils.ts";

onMounted(() => {
  loginPageTokenInit()
})
</script>

<template>
  <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
    <div id="left-card">
      <el-image style="width: 100%;height: 100%" fit="cover"
                src="src/assets/welcome-image.png"/>
    </div>
    <div id="welcome-title">
      <div>welcome here.</div>
    </div>
    <div id="right-card">
      <router-view v-slot="{Component}">
        <transition name="el-fade-in-linear" mode="out-in">
          <component :is="Component"/>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style scoped>
#left-card {
  flex: 1;
  background-color: black
}

@media (max-width: 512px) {
  #left-card {
    display: none;
  }
}

#right-card {
  max-width: 100%;
  height: 100vh;
  z-index: 1;
  background-color: var(--el-bg-color);
}

@media (min-width: 513px) {
  #right-card {
    max-width: 400px;
  }
}

#welcome-title {
  position: absolute;
  bottom: 30px;
  left: 120px;
  transform: translate(-50%, -50%);
  font-weight: bold;
  color: white;
  text-shadow: 0 0 10px black;
}
</style>
