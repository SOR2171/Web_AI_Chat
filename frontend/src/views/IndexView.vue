<script setup lang="ts">
import {logout} from "../api";
import router from "../router/index.ts";

function userLogout() {
  logout(() => {
    router.push('/')
  });
}
</script>

<template>
  <div>
    <el-button @click="userLogout">
      logout
    </el-button>
  </div>
</template>

<style scoped>

</style>