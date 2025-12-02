<script setup lang="ts">
import { onMounted } from 'vue'
import { homePageTokenInit } from "../utils/JwtUtils.ts";
import { Message } from "@element-plus/icons-vue";
import HeaderBar from "../components/home/HeaderBar.vue";

const token = ""

onMounted(() => {
  homePageTokenInit(token)
})

</script>

<template>
  <div class="layout-container">
    <el-container>
      <el-aside width="200px">
        <el-scrollbar>
          <el-menu>
            <el-sub-menu index="1">
              <template #title>
                <el-icon>
                  <message/>
                </el-icon>
                Menu
              </template>
              <el-menu-item-group>
                <template #title>Group 1</template>
                <el-menu-item index="1-1">Option 1</el-menu-item>
                <el-menu-item index="1-2">Option 2</el-menu-item>
              </el-menu-item-group>
              <el-menu-item-group title="Group 2">
                <el-menu-item index="1-3">Option 3</el-menu-item>
              </el-menu-item-group>
              <el-sub-menu index="1-4">
                <template #title>Option4</template>
                <el-menu-item index="1-4-1">Option 4-1</el-menu-item>
              </el-sub-menu>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </el-aside>
      <el-container>
        <el-header>
          <HeaderBar/>
        </el-header>
        <el-main>Main</el-main>
        <el-footer>Footer</el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  background-color: var(--el-bg-color);
}
</style>