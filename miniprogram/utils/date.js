function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(value) {
  if (value === null || value === undefined || value === '') return ''

  if (typeof value === 'string') {
    const source = value.trim()
    const match = source.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
    if (match) return `${match[1]}-${pad(match[2])}-${pad(match[3])}`
  }

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

module.exports = { formatDate }
