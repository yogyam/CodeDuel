/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'coderace-blue': '#2563eb',
        'coderace-dark': '#1e293b',
        'coderace-light': '#f8fafc',
      }
    },
  },
  plugins: [],
}
