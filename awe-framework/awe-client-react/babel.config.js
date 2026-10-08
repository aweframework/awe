module.exports = {
  presets: [
    [
      '@babel/preset-env',
      {
        targets: {
          esmodules: true
        }
      }
    ],
    ['@babel/preset-react', { runtime: 'automatic' }]
  ],
  env: {
    development: {
      compact: false
    }
  }
};
