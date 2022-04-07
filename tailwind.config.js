module.exports = {
  purge: [
      './resources/main.hbs',
  ],
  theme: {
    extend: {
      screens: {
        'dark': {'raw': '(prefers-color-scheme: dark)'},
        // => @media (prefers-color-scheme: dark) { ... }
      }
    },
  },
  variants: {
    textColor: ['responsive', 'visited'],
  },
  plugins: [],
}
