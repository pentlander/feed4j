module.exports = {
  purge: [
      './src/main/resources/templates/index.jte',
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
