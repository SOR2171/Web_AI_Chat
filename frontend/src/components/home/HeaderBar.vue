<script setup lang="ts">
import { logout } from "../../api";
import router from "../../router";
import { UserFilled } from "@element-plus/icons-vue";

function userLogout() {
  logout(() => {
    router.push('/')
  });
}
</script>

<template>
  <div style="margin: 20px">
    <el-row justify="space-between">
      <el-col span="12">
        <el-space/>
      </el-col>
      <el-col span="3">
        <div class="page-title">
          Web AI Chat
        </div>
      </el-col>
      <el-col span="9">
        <div>
          <el-dropdown>
            <el-icon style="width: 40px; height: 40px">
              <el-avatar :icon="UserFilled"/>
            </el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-button @click="userLogout" text>
                    logout
                  </el-button>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: bold;
}
</style>