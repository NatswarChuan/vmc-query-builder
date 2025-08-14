<template>
  <div class="code-block">
    <button @click="copyCode" class="copy-btn">{{ copyButtonText }}</button>
    <pre><code ref="codeElement" :class="`language-${language}`">{{ code }}</code></pre>
  </div>
</template>

<script setup>
import { ref, onMounted, onUpdated } from 'vue';

const props = defineProps({
  code: String,
  language: String
});

const copyButtonText = ref('Copy');
const codeElement = ref(null);

const copyCode = () => {
  navigator.clipboard.writeText(props.code.trim()).then(() => {
    copyButtonText.value = 'Copied!';
    setTimeout(() => {
      copyButtonText.value = 'Copy';
    }, 2000);
  }).catch(err => {
    console.error('Failed to copy: ', err);
    copyButtonText.value = 'Error!';
  });
};

const highlightCode = () => {
  if (codeElement.value && window.hljs) {

    window.hljs.highlightElement(codeElement.value);
  }
};

onMounted(() => {
  highlightCode();
});

onUpdated(() => {
  highlightCode();
});
</script>
