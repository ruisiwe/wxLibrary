const CATEGORY_COLORS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C',
  '#909399', '#9B59B6', '#16A085', '#E67E22'
]

function categoryColor(categoryId) {
  const text = String(categoryId == null ? '' : categoryId)
  let hash = 0
  for (let index = 0; index < text.length; index++) {
    hash = ((hash * 31) + text.charCodeAt(index)) >>> 0
  }
  return CATEGORY_COLORS[hash % CATEGORY_COLORS.length]
}

module.exports = { CATEGORY_COLORS, categoryColor }
