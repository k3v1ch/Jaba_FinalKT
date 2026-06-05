/** @type {import('tailwindcss').Config} */
module.exports = {
  // Scan the markup/JS so only the utility classes actually used end up in the
  // final stylesheet. All classes in this project are static string literals,
  // so the content scan captures every class with no runtime compiler needed.
  content: ['./index.html', './app.js'],
  theme: {
    extend: {},
  },
  plugins: [],
};
