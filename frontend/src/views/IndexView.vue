<script setup lang="ts">
import {logout} from ".//src/net";
import router from ".//src/router/index.ts";

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