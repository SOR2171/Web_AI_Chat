<script setup lang="ts">
import {useDark, useToggle} from "@vueuse/core";

useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: 'dark',
  valueLight: 'light',
})

useDark({
      onChanged(dark) {
        useToggle(dark)
      }
    }
)
</script>

<template>
  <div>
    <router-view/>
  </div>
</template>

<style scoped>

</style>
