// Route method definition
const fixParameters = (p) =>  {if ("subScreenId" in p) {p.subScreenId = p.subScreenId.split("?")[0];} else if ("screenId" in p) {p.screenId = p.screenId.split("?")[0];}};
export const routeMethods = {
  "base": () => "",
  "public": ["$stateParams", (p) => "screen/public/" + p.screenId],
  "private": ["$stateParams", (p) => "screen/private/" + p.screenId],
  "screenData": ["$stateParams", (p) => routeMethods.view(p)],
  "template": (p) => {
    fixParameters(p);
    return angular.element('body').injector().get('ServerData').getScreenData(routeMethods.screen(p), routeMethods.view(p));
  },
  "view": (p) => "subScreenId" in p ? "report" : "base",
  "screen": (p) => "subScreenId" in p ? p.subScreenId.split("?")[0] : "screenId" in p ? p.screenId.split("?")[0] : null
};

// Routing data for view controller
const viewControllerData = {"controller": "ViewController", "template": routeMethods.template, "resolve": {"screenData": routeMethods.screenData, "context": routeMethods.base}};

// Set up states
export const states = [
  {"name": 'index', "url": "/", "views": {"base": {...viewControllerData}}},
  {"name": 'global', "url": "/screen/public/:screenId", "views": {"base": {...viewControllerData}}, "params": {"r": null}},
  {"name": 'public', "url": "/screen/public/:screenId", "views": {"base": {...viewControllerData, "abstract": true}}},
  {"name": 'public.screen', "url": "/:subScreenId", "views": {"report": {...viewControllerData, "resolve": {...viewControllerData.resolve, "context": routeMethods.public}}}, "params": {"r": null}},
  {"name": 'private', "url": "/screen/private/:screenId", "views": {"base": {...viewControllerData, "abstract": true}}},
  {"name": 'private.screen', "url": "/:subScreenId", "views": {"report": {...viewControllerData, "resolve": {...viewControllerData.resolve, "context": routeMethods.private}}}, "params": {"r": null}}
];
