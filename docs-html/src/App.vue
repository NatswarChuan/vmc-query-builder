<template>
  <div class="flex flex-col md:flex-row min-h-screen">
    <Sidebar ref="sidebarRef" :sections="sections" :current-section-id="currentSection.id"
      @navigate="navigateToSection" />
    <MainContent :key="currentSection.id" :is-first-section="isFirstSection" :is-last-section="isLastSection"
      :previous-section-title="previousSectionTitle" :next-section-title="nextSectionTitle" @next="goToNextSection"
      @prev="goToPreviousSection">
      <component :is="sectionComponents[currentSection.component]" />
    </MainContent>
  </div>
</template>

<script setup>
import { ref, computed, defineAsyncComponent, nextTick, onMounted, onUnmounted } from 'vue';
import Sidebar from './components/Sidebar.vue';
import MainContent from './components/MainContent.vue';


const sectionComponents = {
  IntroductionSection: defineAsyncComponent(() => import('./components/sections/IntroductionSection.vue')),
  InstallationSection: defineAsyncComponent(() => import('./components/sections/InstallationSection.vue')),
  EntitiesSection: defineAsyncComponent(() => import('./components/sections/EntitiesSection.vue')),
  RelationshipsSection: defineAsyncComponent(() => import('./components/sections/RelationshipsSection.vue')),
  RepositoriesSection: defineAsyncComponent(() => import('./components/sections/RepositoriesSection.vue')),
  QueryMethodsSection: defineAsyncComponent(() => import('./components/sections/QueryMethodsSection.vue')),
  QueryBuilderSection: defineAsyncComponent(() => import('./components/sections/QueryBuilderSection.vue')),
  ServiceLayerSection: defineAsyncComponent(() => import('./components/sections/ServiceLayerSection.vue')),
  DtoSection: defineAsyncComponent(() => import('./components/sections/DtoSection.vue')),
  ValidationSection: defineAsyncComponent(() => import('./components/sections/ValidationSection.vue')),
};


const sections = ref([
  { id: 'gioi-thieu', title: '1. Giới thiệu', component: 'IntroductionSection', subs: [] },
  { id: 'cai-dat', title: '2. Cài đặt & Cấu hình', component: 'InstallationSection', subs: [] },
  { id: 'dinh-nghia-entities', title: '3. Định nghĩa Entities', component: 'EntitiesSection', subs: [] },
  {
    id: 'anh-xa-quan-he', title: '4. Ánh xạ Quan hệ', component: 'RelationshipsSection', subs: [
      { id: 'onetoone', title: '4.1. One-to-One' },
      { id: 'onetomany', title: '4.2. One-to-Many' },
      { id: 'manytomany', title: '4.3. Many-to-Many' },
      { id: 'lazy-loading', title: '4.4. Lazy Loading & Tùy chọn Thao tác' }
    ]
  },
  { id: 'tao-repositories', title: '5. Tạo Repositories', component: 'RepositoriesSection', subs: [] },
  {
    id: 'phuong-thuc-truy-van', title: '6. Phương thức Truy vấn', component: 'QueryMethodsSection', subs: [
      { id: 'derived-queries', title: '6.1. Derived Queries' },
      { id: 'vmc-query', title: '6.2. @VMCQuery' }
    ]
  },
  { id: 'query-builder', title: '7. VMCQueryBuilder', component: 'QueryBuilderSection', subs: [] },
  { id: 'service-layer', title: '8. Lớp Service', component: 'ServiceLayerSection', subs: [] },
  {
    id: 'dto', title: '9. DTO & Chuyển đổi', component: 'DtoSection', subs: [
      { id: 'dto-mechanism', title: '9.1. Cơ chế Chuyển đổi' },
      { id: 'dto-usage', title: '9.2. Các hàm Service với DTO' }
    ]
  },
  {
    id: 'validation', title: '10. Validation Dữ liệu', component: 'ValidationSection', subs: [
      { id: 'validation-field', title: '10.1. Validation trên Trường' },
      { id: 'validation-class', title: '10.2. Validation trên Lớp' }
    ]
  }
]);

const currentSectionIndex = ref(0);
const sidebarRef = ref(null);
let resizeObserver = null;



onMounted(() => {
  const sidebarEl = sidebarRef.value?.$el;
  if (sidebarEl) {
    const root = document.documentElement;

    resizeObserver = new ResizeObserver(() => {
      if (window.innerWidth >= 768) {
        root.style.setProperty('--sidebar-width', `${sidebarEl.offsetWidth}px`);
      } else {
        root.style.setProperty('--sidebar-width', '0px');
      }
    });

    resizeObserver.observe(sidebarEl);
  }
});

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
});



const currentSection = computed(() => sections.value[currentSectionIndex.value]);
const isFirstSection = computed(() => currentSectionIndex.value === 0);
const isLastSection = computed(() => currentSectionIndex.value === sections.value.length - 1);

const previousSectionTitle = computed(() => {
  return !isFirstSection.value ? sections.value[currentSectionIndex.value - 1].title : null;
});

const nextSectionTitle = computed(() => {
  return !isLastSection.value ? sections.value[currentSectionIndex.value + 1].title : null;
});

const scrollToTop = () => {
  window.scrollTo(0, 0);
};

const goToNextSection = () => {
  if (!isLastSection.value) {
    currentSectionIndex.value++;
    scrollToTop();
  }
};

const goToPreviousSection = () => {
  if (!isFirstSection.value) {
    currentSectionIndex.value--;
    scrollToTop();
  }
};

const navigateToSection = (sectionId) => {
  let targetIndex = -1;
  let anchorId = null;

  targetIndex = sections.value.findIndex(s => s.id === sectionId);

  if (targetIndex === -1) {
    for (let i = 0; i < sections.value.length; i++) {
      if (sections.value[i].subs.some(sub => sub.id === sectionId)) {
        targetIndex = i;
        anchorId = sectionId;
        break;
      }
    }
  }

  if (targetIndex !== -1 && currentSectionIndex.value !== targetIndex) {
    currentSectionIndex.value = targetIndex;
  }

  if (anchorId) {
    nextTick(() => {
      const el = document.getElementById(anchorId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  } else if (targetIndex !== -1) {
    scrollToTop();
  }
};
</script>
