const path = require('path');

// Dates are local to a time zone that is not UTC (and has daylight saving time), so the tests tell local midnight
// from UTC midnight on every machine. Set before Jest starts its workers, which inherit it.
process.env.TZ = 'Europe/Madrid';

// Maven and CI invoke `npm run test:coverage`, which writes GitLab JUnit, Sonar test execution
// and LCOV reports under target/reports/jest (the paths declared in this module's pom.xml).
module.exports = {
  // In CI the job shares the CPUs of the runner with other jobs: Jest's default
  // of one worker per CPU starves the slow suites (the editor render) past their timeout. CI runs 2 workers, which also
  // fit the slow medium runner (a 4 vCPU VM on a shared host). Even then a slow shared runner can push a test past the
  // 5 s default, so CI allows 15 s per test and hook. Local runs keep Jest's defaults, so a slow test still shows up.
  ...(process.env.CI ? {maxWorkers: 2, testTimeout: 15000} : {}),
  // In CI the transform cache lives in the project so the pipeline can keep it between jobs (.gitlab-ci.yml)
  ...(process.env.CI ? {cacheDirectory: '<rootDir>/.jest-cache'} : {}),
  testEnvironment: '<rootDir>/test/js/jest.environment.cjs',
  roots: ['<rootDir>/test/js'],
  testMatch: [
    '**/redux/reducers/**/*Test.{js,jsx}',
    '**/redux/selectors/**/*Test.{js,jsx}',
    '**/utilities/mergeUtilsTest.{js,jsx}',
    '**/packaging/bootstrapGridCompatTest.{js,jsx}',
    '**/packaging/packageVersionTest.{js,jsx}',
    '**/packaging/packageLicenseTest.{js,jsx}',
    '**/packaging/babelConfigTest.{js,jsx}',
    '**/packaging/dependenciesTest.{js,jsx}',
    '**/packaging/applicationDependenciesTest.{js,jsx}',
    '**/packaging/helpTemplatesTest.{js,jsx}',
    '**/packaging/textUtilitiesTest.{js,jsx}',
    '**/packaging/designTokensTest.{js,jsx}',
    '**/packaging/thymeleafPluginTest.{js,jsx}',
    '**/redux/thunks/messagesThunkTest.{js,jsx}',
    '**/redux/thunks/gridThunkTest.{js,jsx}',
    '**/redux/thunks/gridCellLoadRaceThunkTest.{js,jsx}',
    '**/redux/thunks/gridChangeEventThunkTest.{js,jsx}',
    '**/redux/thunks/fillSuggestThunkTest.{js,jsx}',
    '**/redux/thunks/deferredThunkTest.{js,jsx}',
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
    '**/utilities/actionRegistryTest.{js,jsx}',
    '**/services/actionParityTest.{js,jsx}',
    '**/services/actionContractTest.{js,jsx}',
    '**/containers/ActionsContainerUnknownActionTest.{js,jsx}',
    '**/redux/storeTest.{js,jsx}',
    '**/utilities/menuSearchTest.{js,jsx}',
    '**/utilities/numbersTest.{js,jsx}',
    '**/utilities/componentsTest.{js,jsx}',
    '**/utilities/revealTest.{js,jsx}',
    '**/utilities/chartRegistryTest.{js,jsx}',
    '**/utilities/contextMenuRegistryTest.{js,jsx}',
    '**/utilities/chartFormatTest.{js,jsx}',
    '**/i18n/chartLocalesTest.{js,jsx}',
    '**/utilities/echartsOptionTest.{js,jsx}',
    '**/utilities/echartsSvgTest.{js,jsx}',
    '**/utilities/echartsEventsTest.{js,jsx}',
    '**/utilities/echartsAdvancedOptionsTest.{js,jsx}',
    '**/renderer/*Test.{js,jsx}',
    '**/utilities/classChangesTest.{js,jsx}',
    '**/utilities/sparklineTest.{js,jsx}',
    '**/redux/registry/ComponentRegistryTest.{js,jsx}',
    '**/redux/registry/SpecificDependencyRegistryTest.{js,jsx}',
    '**/utilities/replaceWildcardTest.{js,jsx}',
    '**/hooks/useGridContextMenuTest.{js,jsx}',
    '**/hooks/useTextTest.{js,jsx}',
    '**/hooks/useKeyboardShortcutTest.{js,jsx}',
    '**/hooks/useSuggestTest.{js,jsx}',
    '**/utilities/keyboardTest.{js,jsx}',
    '**/hooks/useDateOptionsTest.{js,jsx}',
    '**/hooks/useComponentTest.{js,jsx}',
    '**/hooks/useMaximizeTest.{js,jsx}',
    '**/hooks/useFocusTrapTest.{js,jsx}',
    '**/hooks/useDocumentHeadTest.{js,jsx}',
    '**/redux/actions/advancedDependenciesTest.{js,jsx}',
    '**/redux/actions/dependenciesTest.{js,jsx}',
    '**/redux/actions/settingsTest.{js,jsx}',
    '**/redux/actions/menuTest.{js,jsx}',
    '**/services/ComponentServiceTest.{js,jsx}',
    '**/services/FormServiceTest.{js,jsx}',
    '**/services/WebsocketServiceTest.{js,jsx}',
    '**/redux/thunks/websocketThunkTest.{js,jsx}',
    '**/services/ScreenServiceTest.{js,jsx}',
    '**/services/components/GridServiceTest.{js,jsx}',
    '**/utilities/gridTest.{js,jsx}',
    '**/utilities/gridLayoutTest.{js,jsx}',
    '**/utilities/gridRowDetailTest.{js,jsx}',
    '**/utilities/structureCellTest.{js,jsx}',
    '**/utilities/sanitizeHtmlTest.{js,jsx}',
    '**/utilities/testIdsTest.{js,jsx}',
    '**/utilities/messageTargetTest.{js,jsx}',
    '**/containers/*Test.{js,jsx}',
    '**/templates/*Test.{js,jsx}',
    '**/widgets/WidgetsTest.{js,jsx}',
    '**/widgets/AweFileManagerTest.{js,jsx}',
    '**/widgets/AweCarouselTest.{js,jsx}',
    '**/widgets/AweLogViewerTest.{js,jsx}',
    '**/components/AweImageTest.{js,jsx}',
    '**/components/AweLinkTest.{js,jsx}',
    '**/components/AweVideoTest.{js,jsx}',
    '**/components/AweMapTest.{js,jsx}',
    '**/components/AweWindowTest.{js,jsx}',
    '**/components/AweInfoButtonTest.{js,jsx}',
    '**/components/AweInfoDropdownTest.{js,jsx}',
    '**/components/AweStepsTest.{js,jsx}',
    '**/components/AweTabsTest.{js,jsx}',
    '**/components/AweDialogTest.{js,jsx}',
    '**/components/AweSidePanelTest.{js,jsx}',
    '**/components/AwePopoverTest.{js,jsx}',
    '**/components/AweResizableTest.{js,jsx}',
    '**/columns/*Test.{js,jsx}',
    '**/styles/*Test.{js,jsx}',
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
    '**/components/pivot/*Test.{js,jsx}',
    '**/components/AweTreeGridTest.{js,jsx}',
    '**/components/AweGridTest.{js,jsx}',
    '**/components/AweLoaderTest.{js,jsx}',
    '**/performance/*Test.{js,jsx}',
    '**/components/AweGridEmptyStateTest.{js,jsx}',
    '**/components/AweGridWrapTest.{js,jsx}',
    '**/components/AweGridToolbarTest.{js,jsx}',
    '**/components/AweGridRadioColumnTest.{js,jsx}',
    '**/components/AweGridRowDetailTest.{js,jsx}',
    '**/components/AweGridRowDetailComponentsTest.{js,jsx}',
    '**/components/ElementHelpTest.{js,jsx}',
    '**/components/ErrorBoundaryTest.{js,jsx}',
    '**/components/ContextMenuFamiliesTest.{js,jsx}',
    '**/components/ShowContextMenuActionTest.{js,jsx}',
    '**/components/TestIds*Test.{js,jsx}'
  ],
  transform: {
    '^.+\\.(js|jsx)$': ['babel-jest', { presets: ['@babel/preset-env', '@babel/preset-react'] }]
  },
  setupFiles: ['<rootDir>/test/js/jest.setup.js'],
  setupFilesAfterEnv: ['@testing-library/jest-dom', '<rootDir>/test/js/jest.setupAfterEnv.js'],
  moduleNameMapper: {
    '\\.(css|less)$': 'identity-obj-proxy'
  },
  // Allow babel-jest to transpile ESM-only packages inside node_modules
  transformIgnorePatterns: [
    'node_modules/(?!(quill|lodash-es|parchment|echarts|zrender)/)'
  ],
  testPathIgnorePatterns: [
    '/node_modules/'
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
