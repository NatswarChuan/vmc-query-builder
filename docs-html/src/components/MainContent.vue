<template>
  <main class="flex-1 p-6 md:p-10 relative">
    <div class="max-w-4xl mx-auto pb-28">
      <slot></slot>
    </div>

    <div class="fixed bottom-0 w-full bg-white/80 backdrop-blur-sm border-t border-slate-200" :style="{
      left: 'var(--sidebar-width, 0px)',
      width: 'calc(100% - var(--sidebar-width, 0px))'
    }">
      <div class="px-6 md:px-10 py-4 flex items-center gap-4">
        <button @click="$emit('prev')" v-if="!isFirstSection"
          class="text-left p-4 border border-slate-300 rounded-lg hover:border-blue-500 hover:shadow-md transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
          <span class="text-sm font-medium text-slate-500">Quay lại</span>
          <span class="block text-lg font-semibold text-slate-800 mt-1 truncate">{{ previousSectionTitle }}</span>
        </button>

        <button @click="$emit('next')" v-if="!isLastSection"
          class="ml-auto text-right p-4 border border-slate-300 rounded-lg hover:border-blue-500 hover:shadow-md transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
          <span class="text-sm font-medium text-slate-500">Tiếp theo</span>
          <span class="block text-lg font-semibold text-slate-800 mt-1 truncate">{{ nextSectionTitle }}</span>
        </button>
      </div>
    </div>
  </main>
</template>

<script setup>

defineProps({
  isFirstSection: Boolean,
  isLastSection: Boolean,
  previousSectionTitle: String,
  nextSectionTitle: String
});
defineEmits(['next', 'prev']);
</script>
