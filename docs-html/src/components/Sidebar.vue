<template>
  <aside
    class="w-full md:w-64 lg:w-72 bg-white/90 backdrop-blur-sm border-r border-slate-200 p-4 md:p-6 md:sticky top-0 h-screen overflow-y-auto">
    <h1 class="text-xl font-bold text-slate-900 mb-6">VMC Query Builder</h1>
    <nav id="navigation" class="flex flex-col space-y-2">
      <div v-for="section in sections" :key="section.id">
        <a :href="`#${section.id}`"
           class="nav-link text-slate-600 hover:bg-slate-100 p-2 rounded-md transition-colors duration-200 block"
           :class="{ 'active': currentSectionId === section.id }">
          {{ section.title }}
        </a>
        <div v-if="section.subs && section.subs.length > 0" class="ml-4 mt-2 space-y-2 border-l-2 border-slate-200 pl-4">
          <a v-for="sub in section.subs" :key="sub.id" :href="`#${sub.id}`"
             class="nav-sublink block text-sm text-slate-500 hover:text-blue-600">
            {{ sub.title }}
          </a>
        </div>
      </div>
    </nav>
  </aside>
</template>

<script setup>
defineProps({
  sections: Array,
  currentSectionId: String
});
</script>