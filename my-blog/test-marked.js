const { marked } = require('marked')
const hljs = require('highlight.js')

marked.setOptions({
  gfm: true,
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
})

const md = '```js\nconst count = ref(0)\nconst doubled = computed(() => count.value * 2)\n```'
const result = marked.parse(md)
console.log(result)
console.log('---')
console.log('Has hljs class:', result.includes('hljs'))