const path = require('path');

// Maven and CI invoke `npm run test:coverage`, which writes GitLab JUnit, Sonar test execution
// and LCOV reports under target/reports/jest (the paths declared in this module's pom.xml).
module.exports = {
  testEnvironment: '<rootDir>/test/js/jest.environment.cjs',
  roots: ['<rootDir>/test/js'],
  testMatch: [
    '**/redux/reducers/**/*Test.{js,jsx}',
    '**/redux/selectors/**/*Test.{js,jsx}',
    '**/utilities/mergeUtilsTest.{js,jsx}',
    '**/packaging/packageVersionTest.{js,jsx}',
    '**/redux/thunks/messagesThunkTest.{js,jsx}',
    '**/redux/thunks/gridThunkTest.{js,jsx}',
    '**/redux/thunks/filesThunkTest.{js,jsx}',
    '**/redux/thunks/serverThunkTest.{js,jsx}',
    '**/mainTest.{js,jsx}',
    '**/redux/thunks/screenThunkTest.{js,jsx}',
    '**/redux/thunks/suggestThunkTest.{js,jsx}',
    '**/redux/thunks/formThunkTest.{js,jsx}',
    '**/redux/thunks/validateThunkTest.{js,jsx}',
    '**/redux/thunks/componentsThunkTest.{js,jsx}',
    '**/utilities/datesTest.{js,jsx}',
    '**/utilities/utilsTest.{js,jsx}',
    '**/utilities/menuSearchTest.{js,jsx}',
    '**/utilities/numbersTest.{js,jsx}',
    '**/utilities/componentsTest.{js,jsx}',
    '**/redux/registry/ComponentRegistryTest.{js,jsx}',
    '**/hooks/useTextTest.{js,jsx}',
    '**/hooks/useComponentTest.{js,jsx}',
    '**/redux/actions/advancedDependenciesTest.{js,jsx}',
    '**/redux/actions/dependenciesTest.{js,jsx}',
    '**/redux/actions/settingsTest.{js,jsx}',
    '**/redux/actions/menuTest.{js,jsx}',
    '**/services/ComponentServiceTest.{js,jsx}',
    '**/services/FormServiceTest.{js,jsx}',
    '**/services/WebsocketServiceTest.{js,jsx}',
    '**/services/ScreenServiceTest.{js,jsx}',
    '**/services/components/GridServiceTest.{js,jsx}',
    '**/utilities/gridTest.{js,jsx}',
    '**/utilities/testIdsTest.{js,jsx}',
    '**/containers/*Test.{js,jsx}',
    '**/templates/*Test.{js,jsx}',
    '**/widgets/WidgetsTest.{js,jsx}',
    '**/widgets/AweFileManagerTest.{js,jsx}',
    '**/widgets/AweCarouselTest.{js,jsx}',
    '**/widgets/AweLogViewerTest.{js,jsx}',
    '**/components/AweImageTest.{js,jsx}',
    '**/components/AweLinkTest.{js,jsx}',
    '**/components/AweVideoTest.{js,jsx}',
    '**/components/AweWindowTest.{js,jsx}',
    '**/components/AweInfoButtonTest.{js,jsx}',
    '**/components/AweInfoDropdownTest.{js,jsx}',
    '**/components/AweStepsTest.{js,jsx}',
    '**/components/AweTabsTest.{js,jsx}',
    '**/components/AweDialogTest.{js,jsx}',
    '**/components/AweResizableTest.{js,jsx}',
    '**/columns/*Test.{js,jsx}',
    '**/widgets/AwePdfViewerTest.{js,jsx}',
    '**/widgets/AweHelpViewerTest.{js,jsx}',
    '**/criteria/*Test.{js,jsx}',
    '**/components/AweAccordionTest.{js,jsx}',
    '**/components/AweAvatarTest.{js,jsx}',
    '**/components/AweButtonTest.{js,jsx}',
    '**/components/AweTagListTest.{js,jsx}',
    '**/components/TagTest.{js,jsx}',
    '**/components/AweChartTest.{js,jsx}',
    '**/components/AweMenuTest.{js,jsx}',
    '**/components/AweMenuSearchTest.{js,jsx}',
    '**/components/AwePivotTableTest.{js,jsx}',
    '**/components/AweTreeGridTest.{js,jsx}',
    '**/components/AweGridTest.{js,jsx}',
    '**/components/TestIds*Test.{js,jsx}'
  ],
  transform: {
    '^.+\\.(js|jsx)$': ['babel-jest', { presets: ['@babel/preset-env', '@babel/preset-react'] }]
  },
  setupFiles: ['<rootDir>/test/js/jest.setup.js'],
  setupFilesAfterEnv: ['@testing-library/jest-dom'],
  moduleNameMapper: {
    '\\.(css|less)$': 'identity-obj-proxy'
  },
  // Allow babel-jest to transpile ESM-only packages inside node_modules
  transformIgnorePatterns: [
    'node_modules/(?!(quill|lodash-es|parchment)/)'
  ],
  testPathIgnorePatterns: [
    '/node_modules/',
    '/test/js/index.js$'
  ],
  coverageDirectory: '<rootDir>/target/reports/jest/coverage',
  coverageReporters: ['lcov', 'text-summary', 'html'],
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: path.join(__dirname, 'target', 'reports', 'jest', 'junit'),
        outputName: 'javascriptUnitTests.xml',
        suiteName: 'awe-client-react-jest'
      }
    ],
    [
      '@casualbot/jest-sonar-reporter',
      {
        outputDirectory: path.join(__dirname, 'target', 'reports', 'jest', 'sonar'),
        outputName: 'javascriptUnitTests.xml',
        relativePaths: true
      }
    ]
  ]
};
