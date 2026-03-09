/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'page':      '#f1f5f9',
        'card':      '#ffffff',
        'subtle':    '#f8fafc',
        'border':    '#e2e8f0',
        'border-s':  '#f1f5f9',
        'main':      '#1e293b',
        'secondary': '#64748b',
        'muted':     '#94a3b8',
        'accent':    '#3b82f6',
        'accent-bg': '#eff6ff',
        'pos':       '#16a34a',
        'pos-bg':    '#dcfce7',
        'mod':       '#a16207',
        'mod-bg':    '#fef9c3',
        'neg':       '#dc2626',
        'neg-bg':    '#fee2e2',
      },
      borderRadius: {
        'card':   '12px',
        'filter': '10px',
        'input':  '6px',
        'badge':  '20px',
      },
      width: {
        'sidebar-collapsed': '52px',
      },
    },
  },
  plugins: [],
}
