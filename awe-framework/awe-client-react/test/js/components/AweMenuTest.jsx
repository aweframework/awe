import React from 'react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweMenu from "../../../src/components/AweMenu";

const options = [
  {
    "elementType": "Option",
    "elementList": [],
    "name": "information",
    "restricted": false,
    "id": "information",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "information",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "information",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": false,
    "options": []
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "information-restricted",
    "module": "Inf",
    "restricted": false,
    "id": "information-restricted",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "information-restricted",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "information-restricted",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": false,
    "options": []
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "information-restricted-changed",
    "module": "Inf Changed",
    "restricted": false,
    "id": "information-restricted-changed",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "information-restricted-changed",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "information-restricted-changed",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": false,
    "options": []
  },
  {
    "elementType": "Option",
    "label": "MENU_TOOLS",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SITES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-site",
            "restricted": false,
            "id": "new-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-site",
            "restricted": false,
            "id": "update-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-site",
            "restricted": false,
            "id": "view-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "sites",
        "icon": "sitemap",
        "restricted": false,
        "text": "MENU_TOOLS_SITES",
        "id": "sites",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sites",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sites",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-site",
            "restricted": false,
            "id": "new-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-site",
            "restricted": false,
            "id": "update-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-site",
            "restricted": false,
            "id": "view-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_MODULES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-module",
            "restricted": false,
            "id": "new-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-module",
            "restricted": false,
            "id": "update-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-module",
            "restricted": false,
            "id": "view-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "modules",
        "icon": "puzzle-piece",
        "restricted": false,
        "text": "MENU_TOOLS_MODULES",
        "id": "modules",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "modules",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "modules",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-module",
            "restricted": false,
            "id": "new-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-module",
            "restricted": false,
            "id": "update-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-module",
            "restricted": false,
            "id": "view-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_USERS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-user",
            "restricted": false,
            "id": "new-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-user",
            "restricted": false,
            "id": "update-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-user",
            "restricted": false,
            "id": "view-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "users",
        "icon": "user",
        "restricted": false,
        "text": "MENU_TOOLS_USERS",
        "id": "users",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "users",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "users",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-user",
            "restricted": false,
            "id": "new-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-user",
            "restricted": false,
            "id": "update-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-user",
            "restricted": false,
            "id": "view-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_PROFILES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-profile",
            "restricted": false,
            "id": "new-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-profile",
            "restricted": false,
            "id": "update-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-profile",
            "restricted": false,
            "id": "view-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "profiles",
        "icon": "group",
        "restricted": false,
        "text": "MENU_TOOLS_PROFILES",
        "id": "profiles",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "profiles",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "profiles",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-profile",
            "restricted": false,
            "id": "new-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-profile",
            "restricted": false,
            "id": "update-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-profile",
            "restricted": false,
            "id": "view-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_DATABASE",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-database",
            "restricted": false,
            "id": "new-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-database",
            "restricted": false,
            "id": "update-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-database",
            "restricted": false,
            "id": "view-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "databases",
        "icon": "database",
        "restricted": false,
        "text": "MENU_TOOLS_DATABASE",
        "id": "databases",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "databases",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "databases",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-database",
            "restricted": false,
            "id": "new-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-database",
            "restricted": false,
            "id": "update-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-database",
            "restricted": false,
            "id": "view-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_THEMES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-theme",
            "restricted": false,
            "id": "new-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-theme",
            "restricted": false,
            "id": "update-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-theme",
            "restricted": false,
            "id": "view-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "themes",
        "icon": "picture-o",
        "restricted": false,
        "text": "MENU_TOOLS_THEMES",
        "id": "themes",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "themes",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "themes",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-theme",
            "restricted": false,
            "id": "new-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-theme",
            "restricted": false,
            "id": "update-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-theme",
            "restricted": false,
            "id": "view-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_EML_SRV",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-email-server",
            "restricted": false,
            "id": "new-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-email-server",
            "restricted": false,
            "id": "update-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "email-servers",
        "icon": "envelope",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_EML_SRV",
        "id": "email-servers",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "email-servers",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "email-servers",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-email-server",
            "restricted": false,
            "id": "new-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-email-server",
            "restricted": false,
            "id": "update-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_QUE",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-queue",
            "restricted": false,
            "id": "new-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-queue",
            "restricted": false,
            "id": "update-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "queues",
        "icon": "exchange",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_QUE",
        "id": "queues",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "queues",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "queues",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-queue",
            "restricted": false,
            "id": "new-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-queue",
            "restricted": false,
            "id": "update-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWEKEY",
        "elementList": [],
        "name": "sequences",
        "icon": "key",
        "restricted": false,
        "text": "MENU_TOOLS_AWEKEY",
        "id": "sequences",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sequences",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sequences",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_APPPAR",
        "elementList": [],
        "name": "application-parameters",
        "icon": "cogs",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_APPPAR",
        "id": "application-parameters",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-parameters",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-parameters",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_BROADCAST",
        "elementList": [],
        "name": "broadcast-messages",
        "icon": "rss",
        "restricted": false,
        "text": "MENU_BROADCAST",
        "id": "broadcast-messages",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "broadcast-messages",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "broadcast-messages",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_LOG",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-log",
            "restricted": false,
            "id": "view-log",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-log",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-log",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "log",
        "icon": "file-text",
        "restricted": false,
        "text": "MENU_LOG",
        "id": "log",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "log",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "log",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-log",
            "restricted": false,
            "id": "view-log",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-log",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-log",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SQL_EXTRACTOR",
        "elementList": [],
        "name": "sqlExtractor",
        "icon": "database",
        "restricted": false,
        "text": "MENU_SQL_EXTRACTOR",
        "id": "sqlExtractor",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sqlExtractor",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sqlExtractor",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "tools",
    "icon": "wrench",
    "restricted": false,
    "text": "MENU_TOOLS",
    "id": "tools",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "tools",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "tools",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SITES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-site",
            "restricted": false,
            "id": "new-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-site",
            "restricted": false,
            "id": "update-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-site",
            "restricted": false,
            "id": "view-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "sites",
        "icon": "sitemap",
        "restricted": false,
        "text": "MENU_TOOLS_SITES",
        "id": "sites",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sites",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sites",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-site",
            "restricted": false,
            "id": "new-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-site",
            "restricted": false,
            "id": "update-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-site",
            "restricted": false,
            "id": "view-site",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-site",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-site",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_MODULES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-module",
            "restricted": false,
            "id": "new-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-module",
            "restricted": false,
            "id": "update-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-module",
            "restricted": false,
            "id": "view-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "modules",
        "icon": "puzzle-piece",
        "restricted": false,
        "text": "MENU_TOOLS_MODULES",
        "id": "modules",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "modules",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "modules",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-module",
            "restricted": false,
            "id": "new-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-module",
            "restricted": false,
            "id": "update-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-module",
            "restricted": false,
            "id": "view-module",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-module",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-module",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_USERS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-user",
            "restricted": false,
            "id": "new-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-user",
            "restricted": false,
            "id": "update-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-user",
            "restricted": false,
            "id": "view-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "users",
        "icon": "user",
        "restricted": false,
        "text": "MENU_TOOLS_USERS",
        "id": "users",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "users",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "users",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-user",
            "restricted": false,
            "id": "new-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-user",
            "restricted": false,
            "id": "update-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-user",
            "restricted": false,
            "id": "view-user",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-user",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-user",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_PROFILES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-profile",
            "restricted": false,
            "id": "new-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-profile",
            "restricted": false,
            "id": "update-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-profile",
            "restricted": false,
            "id": "view-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "profiles",
        "icon": "group",
        "restricted": false,
        "text": "MENU_TOOLS_PROFILES",
        "id": "profiles",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "profiles",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "profiles",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-profile",
            "restricted": false,
            "id": "new-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-profile",
            "restricted": false,
            "id": "update-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-profile",
            "restricted": false,
            "id": "view-profile",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-profile",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-profile",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_DATABASE",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-database",
            "restricted": false,
            "id": "new-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-database",
            "restricted": false,
            "id": "update-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-database",
            "restricted": false,
            "id": "view-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "databases",
        "icon": "database",
        "restricted": false,
        "text": "MENU_TOOLS_DATABASE",
        "id": "databases",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "databases",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "databases",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-database",
            "restricted": false,
            "id": "new-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-database",
            "restricted": false,
            "id": "update-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-database",
            "restricted": false,
            "id": "view-database",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-database",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-database",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_THEMES",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-theme",
            "restricted": false,
            "id": "new-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-theme",
            "restricted": false,
            "id": "update-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-theme",
            "restricted": false,
            "id": "view-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "themes",
        "icon": "picture-o",
        "restricted": false,
        "text": "MENU_TOOLS_THEMES",
        "id": "themes",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "themes",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "themes",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-theme",
            "restricted": false,
            "id": "new-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-theme",
            "restricted": false,
            "id": "update-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-theme",
            "restricted": false,
            "id": "view-theme",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-theme",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-theme",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_EML_SRV",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-email-server",
            "restricted": false,
            "id": "new-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-email-server",
            "restricted": false,
            "id": "update-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "email-servers",
        "icon": "envelope",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_EML_SRV",
        "id": "email-servers",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "email-servers",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "email-servers",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-email-server",
            "restricted": false,
            "id": "new-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-email-server",
            "restricted": false,
            "id": "update-email-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-email-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-email-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_QUE",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-queue",
            "restricted": false,
            "id": "new-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-queue",
            "restricted": false,
            "id": "update-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "queues",
        "icon": "exchange",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_QUE",
        "id": "queues",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "queues",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "queues",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-queue",
            "restricted": false,
            "id": "new-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-queue",
            "restricted": false,
            "id": "update-queue",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-queue",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-queue",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWEKEY",
        "elementList": [],
        "name": "sequences",
        "icon": "key",
        "restricted": false,
        "text": "MENU_TOOLS_AWEKEY",
        "id": "sequences",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sequences",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sequences",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_AWE_APPPAR",
        "elementList": [],
        "name": "application-parameters",
        "icon": "cogs",
        "restricted": false,
        "text": "MENU_TOOLS_AWE_APPPAR",
        "id": "application-parameters",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-parameters",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-parameters",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_BROADCAST",
        "elementList": [],
        "name": "broadcast-messages",
        "icon": "rss",
        "restricted": false,
        "text": "MENU_BROADCAST",
        "id": "broadcast-messages",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "broadcast-messages",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "broadcast-messages",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_LOG",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-log",
            "restricted": false,
            "id": "view-log",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-log",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-log",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "log",
        "icon": "file-text",
        "restricted": false,
        "text": "MENU_LOG",
        "id": "log",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "log",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "log",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "view-log",
            "restricted": false,
            "id": "view-log",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "view-log",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "view-log",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SQL_EXTRACTOR",
        "elementList": [],
        "name": "sqlExtractor",
        "icon": "database",
        "restricted": false,
        "text": "MENU_SQL_EXTRACTOR",
        "id": "sqlExtractor",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "sqlExtractor",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "sqlExtractor",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  },
  {
    "elementType": "Option",
    "label": "MENU_SETTINGS",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SECURITY",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREENS_ACCESS",
            "elementList": [],
            "name": "screen-access",
            "icon": "eye-slash",
            "restricted": false,
            "text": "MENU_TOOLS_SCREENS_ACCESS",
            "id": "screen-access",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "screen-access",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "screen-access",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREEN_ENCRYPT",
            "elementList": [],
            "name": "encrypt-tools",
            "icon": "lock",
            "restricted": false,
            "text": "MENU_TOOLS_SCREEN_ENCRYPT",
            "id": "encrypt-tools",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "encrypt-tools",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "encrypt-tools",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "security",
        "icon": "unlock-alt",
        "restricted": false,
        "text": "MENU_TOOLS_SECURITY",
        "id": "security",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "security",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "security",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREENS_ACCESS",
            "elementList": [],
            "name": "screen-access",
            "icon": "eye-slash",
            "restricted": false,
            "text": "MENU_TOOLS_SCREENS_ACCESS",
            "id": "screen-access",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "screen-access",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "screen-access",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREEN_ENCRYPT",
            "elementList": [],
            "name": "encrypt-tools",
            "icon": "lock",
            "restricted": false,
            "text": "MENU_TOOLS_SCREEN_ENCRYPT",
            "id": "encrypt-tools",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "encrypt-tools",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "encrypt-tools",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SCR_CNF",
        "elementList": [],
        "name": "screen-configuration",
        "icon": "laptop",
        "restricted": false,
        "text": "MENU_TOOLS_SCR_CNF",
        "id": "screen-configuration",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "screen-configuration",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "screen-configuration",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "settings",
    "icon": "cog",
    "restricted": false,
    "text": "MENU_SETTINGS",
    "id": "settings",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "settings",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "settings",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SECURITY",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREENS_ACCESS",
            "elementList": [],
            "name": "screen-access",
            "icon": "eye-slash",
            "restricted": false,
            "text": "MENU_TOOLS_SCREENS_ACCESS",
            "id": "screen-access",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "screen-access",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "screen-access",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREEN_ENCRYPT",
            "elementList": [],
            "name": "encrypt-tools",
            "icon": "lock",
            "restricted": false,
            "text": "MENU_TOOLS_SCREEN_ENCRYPT",
            "id": "encrypt-tools",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "encrypt-tools",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "encrypt-tools",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "security",
        "icon": "unlock-alt",
        "restricted": false,
        "text": "MENU_TOOLS_SECURITY",
        "id": "security",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "security",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "security",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREENS_ACCESS",
            "elementList": [],
            "name": "screen-access",
            "icon": "eye-slash",
            "restricted": false,
            "text": "MENU_TOOLS_SCREENS_ACCESS",
            "id": "screen-access",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "screen-access",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "screen-access",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TOOLS_SCREEN_ENCRYPT",
            "elementList": [],
            "name": "encrypt-tools",
            "icon": "lock",
            "restricted": false,
            "text": "MENU_TOOLS_SCREEN_ENCRYPT",
            "id": "encrypt-tools",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "encrypt-tools",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "encrypt-tools",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SCR_CNF",
        "elementList": [],
        "name": "screen-configuration",
        "icon": "laptop",
        "restricted": false,
        "text": "MENU_TOOLS_SCR_CNF",
        "id": "screen-configuration",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "screen-configuration",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "screen-configuration",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  },
  {
    "elementType": "Option",
    "label": "MENU_TEST",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_TEST_CRITERIA",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test",
            "icon": "keyboard-o",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test-left",
            "icon": "align-right",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test-left",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test-left",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test-left",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA_RESET",
            "elementList": [],
            "name": "criteria-reset",
            "icon": "refresh",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA_RESET",
            "id": "criteria-reset",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-reset",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-reset",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "criteria",
        "icon": "object-group",
        "restricted": false,
        "text": "MENU_TEST_CRITERIA",
        "id": "criteria",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "criteria",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "criteria",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test",
            "icon": "keyboard-o",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test-left",
            "icon": "align-right",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test-left",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test-left",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test-left",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA_RESET",
            "elementList": [],
            "name": "criteria-reset",
            "icon": "refresh",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA_RESET",
            "id": "criteria-reset",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-reset",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-reset",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_BUTTONS",
        "elementList": [],
        "name": "button-test",
        "icon": "check",
        "restricted": false,
        "text": "MENU_TEST_BUTTONS",
        "id": "button-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "button-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "button-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_MATRIX",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_ALL",
            "elementList": [],
            "name": "matrix-test",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_ALL",
            "id": "matrix-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_NUMBERS",
            "elementList": [],
            "name": "matrix-test-numbers",
            "icon": "list-ol",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_NUMBERS",
            "id": "matrix-test-numbers",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-numbers",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-numbers",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_SUGGEST",
            "elementList": [],
            "name": "matrix-test-suggest",
            "icon": "list-ul",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_SUGGEST",
            "id": "matrix-test-suggest",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-suggest",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-suggest",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_HAVING",
            "elementList": [],
            "name": "matrix-test-having",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_HAVING",
            "id": "matrix-test-having",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-having",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-having",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_LOAD_ALL",
            "elementList": [],
            "name": "matrix-test-load-all",
            "icon": "table",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_LOAD_ALL",
            "id": "matrix-test-load-all",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-load-all",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-load-all",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_TREE",
            "elementList": [],
            "name": "matrix-test-tree",
            "icon": "sitemap",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_TREE",
            "id": "matrix-test-tree",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-tree",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-tree",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "matrix",
        "icon": "table",
        "restricted": false,
        "text": "MENU_TEST_MATRIX",
        "id": "matrix",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "matrix",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "matrix",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_ALL",
            "elementList": [],
            "name": "matrix-test",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_ALL",
            "id": "matrix-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_NUMBERS",
            "elementList": [],
            "name": "matrix-test-numbers",
            "icon": "list-ol",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_NUMBERS",
            "id": "matrix-test-numbers",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-numbers",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-numbers",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_SUGGEST",
            "elementList": [],
            "name": "matrix-test-suggest",
            "icon": "list-ul",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_SUGGEST",
            "id": "matrix-test-suggest",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-suggest",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-suggest",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_HAVING",
            "elementList": [],
            "name": "matrix-test-having",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_HAVING",
            "id": "matrix-test-having",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-having",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-having",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_LOAD_ALL",
            "elementList": [],
            "name": "matrix-test-load-all",
            "icon": "table",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_LOAD_ALL",
            "id": "matrix-test-load-all",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-load-all",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-load-all",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_TREE",
            "elementList": [],
            "name": "matrix-test-tree",
            "icon": "sitemap",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_TREE",
            "id": "matrix-test-tree",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-tree",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-tree",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_CHARTS",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHARTS",
            "elementList": [],
            "name": "chart-test",
            "icon": "bar-chart-o",
            "restricted": false,
            "text": "MENU_TEST_CHARTS",
            "id": "chart-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "elementList": [],
            "name": "chart-series-test",
            "icon": "line-chart",
            "restricted": false,
            "text": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "id": "chart-series-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-series-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-series-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_GRID_CHART",
            "elementList": [],
            "name": "grid-and-chart",
            "icon": "file-text-o",
            "restricted": false,
            "text": "MENU_GRID_CHART",
            "id": "grid-and-chart",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "grid-and-chart",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "grid-and-chart",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_QUEUES",
            "elementList": [],
            "name": "queue-test",
            "icon": "exchange",
            "restricted": false,
            "text": "MENU_TEST_QUEUES",
            "id": "queue-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "queue-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "queue-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "chart",
        "icon": "area-chart",
        "restricted": false,
        "text": "MENU_TEST_CHARTS",
        "id": "chart",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "chart",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "chart",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHARTS",
            "elementList": [],
            "name": "chart-test",
            "icon": "bar-chart-o",
            "restricted": false,
            "text": "MENU_TEST_CHARTS",
            "id": "chart-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "elementList": [],
            "name": "chart-series-test",
            "icon": "line-chart",
            "restricted": false,
            "text": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "id": "chart-series-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-series-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-series-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_GRID_CHART",
            "elementList": [],
            "name": "grid-and-chart",
            "icon": "file-text-o",
            "restricted": false,
            "text": "MENU_GRID_CHART",
            "id": "grid-and-chart",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "grid-and-chart",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "grid-and-chart",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_QUEUES",
            "elementList": [],
            "name": "queue-test",
            "icon": "exchange",
            "restricted": false,
            "text": "MENU_TEST_QUEUES",
            "id": "queue-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "queue-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "queue-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_LAYOUT",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 1",
            "elementList": [],
            "name": "layout-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 1",
            "id": "layout-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 2",
            "elementList": [],
            "name": "layout2-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 2",
            "id": "layout2-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout2-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout2-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "layout",
        "icon": "list-alt",
        "restricted": false,
        "text": "MENU_TEST_LAYOUT",
        "id": "layout",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "layout",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "layout",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 1",
            "elementList": [],
            "name": "layout-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 1",
            "id": "layout-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 2",
            "elementList": [],
            "name": "layout2-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 2",
            "id": "layout2-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout2-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout2-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_WIZARD",
        "elementList": [],
        "name": "wizard-test",
        "icon": "magic",
        "restricted": false,
        "text": "MENU_TEST_WIZARD",
        "id": "wizard-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "wizard-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "wizard-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_ACCORDIONS",
        "elementList": [],
        "name": "accordion-test",
        "icon": "bars",
        "restricted": false,
        "text": "MENU_TEST_ACCORDIONS",
        "id": "accordion-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "accordion-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "accordion-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SCREENS_ACCESS",
        "elementList": [],
        "name": "screen-test",
        "icon": "eye-slash",
        "restricted": false,
        "text": "MENU_TOOLS_SCREENS_ACCESS",
        "id": "screen-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "screen-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "screen-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_PIVOT",
        "elementList": [],
        "name": "pivot-test",
        "icon": "table",
        "restricted": false,
        "text": "MENU_TEST_PIVOT",
        "id": "pivot-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "pivot-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "pivot-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_FILEMANAGER",
        "elementList": [],
        "name": "filemanager-test",
        "icon": "folder",
        "restricted": false,
        "text": "MENU_TEST_FILEMANAGER",
        "id": "filemanager-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "filemanager-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "filemanager-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "test",
    "module": "Test",
    "icon": "flask",
    "restricted": false,
    "text": "MENU_TEST",
    "id": "test",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "test",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "test",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_TEST_CRITERIA",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test",
            "icon": "keyboard-o",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test-left",
            "icon": "align-right",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test-left",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test-left",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test-left",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA_RESET",
            "elementList": [],
            "name": "criteria-reset",
            "icon": "refresh",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA_RESET",
            "id": "criteria-reset",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-reset",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-reset",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "criteria",
        "icon": "object-group",
        "restricted": false,
        "text": "MENU_TEST_CRITERIA",
        "id": "criteria",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "criteria",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "criteria",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test",
            "icon": "keyboard-o",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA",
            "elementList": [],
            "name": "criteria-test-left",
            "icon": "align-right",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA",
            "id": "criteria-test-left",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-test-left",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-test-left",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CRITERIA_RESET",
            "elementList": [],
            "name": "criteria-reset",
            "icon": "refresh",
            "restricted": false,
            "text": "MENU_TEST_CRITERIA_RESET",
            "id": "criteria-reset",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "criteria-reset",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "criteria-reset",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_BUTTONS",
        "elementList": [],
        "name": "button-test",
        "icon": "check",
        "restricted": false,
        "text": "MENU_TEST_BUTTONS",
        "id": "button-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "button-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "button-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_MATRIX",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_ALL",
            "elementList": [],
            "name": "matrix-test",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_ALL",
            "id": "matrix-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_NUMBERS",
            "elementList": [],
            "name": "matrix-test-numbers",
            "icon": "list-ol",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_NUMBERS",
            "id": "matrix-test-numbers",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-numbers",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-numbers",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_SUGGEST",
            "elementList": [],
            "name": "matrix-test-suggest",
            "icon": "list-ul",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_SUGGEST",
            "id": "matrix-test-suggest",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-suggest",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-suggest",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_HAVING",
            "elementList": [],
            "name": "matrix-test-having",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_HAVING",
            "id": "matrix-test-having",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-having",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-having",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_LOAD_ALL",
            "elementList": [],
            "name": "matrix-test-load-all",
            "icon": "table",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_LOAD_ALL",
            "id": "matrix-test-load-all",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-load-all",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-load-all",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_TREE",
            "elementList": [],
            "name": "matrix-test-tree",
            "icon": "sitemap",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_TREE",
            "id": "matrix-test-tree",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-tree",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-tree",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "matrix",
        "icon": "table",
        "restricted": false,
        "text": "MENU_TEST_MATRIX",
        "id": "matrix",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "matrix",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "matrix",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_ALL",
            "elementList": [],
            "name": "matrix-test",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_ALL",
            "id": "matrix-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_NUMBERS",
            "elementList": [],
            "name": "matrix-test-numbers",
            "icon": "list-ol",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_NUMBERS",
            "id": "matrix-test-numbers",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-numbers",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-numbers",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_SUGGEST",
            "elementList": [],
            "name": "matrix-test-suggest",
            "icon": "list-ul",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_SUGGEST",
            "id": "matrix-test-suggest",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-suggest",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-suggest",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_HAVING",
            "elementList": [],
            "name": "matrix-test-having",
            "icon": "list-alt",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_HAVING",
            "id": "matrix-test-having",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-having",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-having",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_LOAD_ALL",
            "elementList": [],
            "name": "matrix-test-load-all",
            "icon": "table",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_LOAD_ALL",
            "id": "matrix-test-load-all",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-load-all",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-load-all",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_MATRIX_TREE",
            "elementList": [],
            "name": "matrix-test-tree",
            "icon": "sitemap",
            "restricted": false,
            "text": "MENU_TEST_MATRIX_TREE",
            "id": "matrix-test-tree",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "matrix-test-tree",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "matrix-test-tree",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_CHARTS",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHARTS",
            "elementList": [],
            "name": "chart-test",
            "icon": "bar-chart-o",
            "restricted": false,
            "text": "MENU_TEST_CHARTS",
            "id": "chart-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "elementList": [],
            "name": "chart-series-test",
            "icon": "line-chart",
            "restricted": false,
            "text": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "id": "chart-series-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-series-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-series-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_GRID_CHART",
            "elementList": [],
            "name": "grid-and-chart",
            "icon": "file-text-o",
            "restricted": false,
            "text": "MENU_GRID_CHART",
            "id": "grid-and-chart",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "grid-and-chart",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "grid-and-chart",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_QUEUES",
            "elementList": [],
            "name": "queue-test",
            "icon": "exchange",
            "restricted": false,
            "text": "MENU_TEST_QUEUES",
            "id": "queue-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "queue-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "queue-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "chart",
        "icon": "area-chart",
        "restricted": false,
        "text": "MENU_TEST_CHARTS",
        "id": "chart",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "chart",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "chart",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHARTS",
            "elementList": [],
            "name": "chart-test",
            "icon": "bar-chart-o",
            "restricted": false,
            "text": "MENU_TEST_CHARTS",
            "id": "chart-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "elementList": [],
            "name": "chart-series-test",
            "icon": "line-chart",
            "restricted": false,
            "text": "MENU_TEST_CHART_DYNAMIC_SERIES",
            "id": "chart-series-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "chart-series-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "chart-series-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_GRID_CHART",
            "elementList": [],
            "name": "grid-and-chart",
            "icon": "file-text-o",
            "restricted": false,
            "text": "MENU_GRID_CHART",
            "id": "grid-and-chart",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "grid-and-chart",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "grid-and-chart",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_QUEUES",
            "elementList": [],
            "name": "queue-test",
            "icon": "exchange",
            "restricted": false,
            "text": "MENU_TEST_QUEUES",
            "id": "queue-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "queue-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "queue-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_LAYOUT",
        "elementList": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 1",
            "elementList": [],
            "name": "layout-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 1",
            "id": "layout-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 2",
            "elementList": [],
            "name": "layout2-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 2",
            "id": "layout2-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout2-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout2-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ],
        "name": "layout",
        "icon": "list-alt",
        "restricted": false,
        "text": "MENU_TEST_LAYOUT",
        "id": "layout",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "layout",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "layout",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 1",
            "elementList": [],
            "name": "layout-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 1",
            "id": "layout-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          },
          {
            "elementType": "Option",
            "label": "MENU_TEST_LAYOUT 2",
            "elementList": [],
            "name": "layout2-test",
            "icon": "th-large",
            "restricted": false,
            "text": "MENU_TEST_LAYOUT 2",
            "id": "layout2-test",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "layout2-test",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "layout2-test",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": true,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_WIZARD",
        "elementList": [],
        "name": "wizard-test",
        "icon": "magic",
        "restricted": false,
        "text": "MENU_TEST_WIZARD",
        "id": "wizard-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "wizard-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "wizard-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_ACCORDIONS",
        "elementList": [],
        "name": "accordion-test",
        "icon": "bars",
        "restricted": false,
        "text": "MENU_TEST_ACCORDIONS",
        "id": "accordion-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "accordion-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "accordion-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TOOLS_SCREENS_ACCESS",
        "elementList": [],
        "name": "screen-test",
        "icon": "eye-slash",
        "restricted": false,
        "text": "MENU_TOOLS_SCREENS_ACCESS",
        "id": "screen-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "screen-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "screen-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_PIVOT",
        "elementList": [],
        "name": "pivot-test",
        "icon": "table",
        "restricted": false,
        "text": "MENU_TEST_PIVOT",
        "id": "pivot-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "pivot-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "pivot-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_TEST_FILEMANAGER",
        "elementList": [],
        "name": "filemanager-test",
        "icon": "folder",
        "restricted": false,
        "text": "MENU_TEST_FILEMANAGER",
        "id": "filemanager-test",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "filemanager-test",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "filemanager-test",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "scheduler-separator",
    "restricted": false,
    "id": "scheduler-separator",
    "reload": false,
    "separator": true,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "scheduler-separator",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "scheduler-separator",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": []
  },
  {
    "elementType": "Option",
    "label": "MENU_SCHEDULER",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_MANAGEMENT",
        "elementList": [],
        "name": "scheduler-management",
        "icon": "cogs",
        "restricted": false,
        "text": "MENU_SCHEDULER_MANAGEMENT",
        "id": "scheduler-management",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-management",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-management",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_TASKS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-task",
            "restricted": false,
            "id": "new-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-task",
            "restricted": false,
            "id": "update-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-tasks",
        "icon": "tasks",
        "restricted": false,
        "text": "MENU_SCHEDULER_TASKS",
        "id": "scheduler-tasks",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-tasks",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-tasks",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-task",
            "restricted": false,
            "id": "new-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-task",
            "restricted": false,
            "id": "update-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_SERVERS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-server",
            "restricted": false,
            "id": "new-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-server",
            "restricted": false,
            "id": "update-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-servers",
        "icon": "server",
        "restricted": false,
        "text": "MENU_SCHEDULER_SERVERS",
        "id": "scheduler-servers",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-servers",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-servers",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-server",
            "restricted": false,
            "id": "new-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-server",
            "restricted": false,
            "id": "update-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_CALENDARS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-calendar",
            "restricted": false,
            "id": "new-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-calendar",
            "restricted": false,
            "id": "update-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-calendars",
        "icon": "calendar",
        "restricted": false,
        "text": "MENU_SCHEDULER_CALENDARS",
        "id": "scheduler-calendars",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-calendars",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-calendars",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-calendar",
            "restricted": false,
            "id": "new-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-calendar",
            "restricted": false,
            "id": "update-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      }
    ],
    "name": "scheduler",
    "icon": "clock-o",
    "restricted": false,
    "text": "MENU_SCHEDULER",
    "id": "scheduler",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "scheduler",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "scheduler",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_MANAGEMENT",
        "elementList": [],
        "name": "scheduler-management",
        "icon": "cogs",
        "restricted": false,
        "text": "MENU_SCHEDULER_MANAGEMENT",
        "id": "scheduler-management",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-management",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-management",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_TASKS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-task",
            "restricted": false,
            "id": "new-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-task",
            "restricted": false,
            "id": "update-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-tasks",
        "icon": "tasks",
        "restricted": false,
        "text": "MENU_SCHEDULER_TASKS",
        "id": "scheduler-tasks",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-tasks",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-tasks",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-task",
            "restricted": false,
            "id": "new-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-task",
            "restricted": false,
            "id": "update-scheduler-task",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-task",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-task",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_SERVERS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-server",
            "restricted": false,
            "id": "new-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-server",
            "restricted": false,
            "id": "update-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-servers",
        "icon": "server",
        "restricted": false,
        "text": "MENU_SCHEDULER_SERVERS",
        "id": "scheduler-servers",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-servers",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-servers",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-server",
            "restricted": false,
            "id": "new-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-server",
            "restricted": false,
            "id": "update-scheduler-server",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-server",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-server",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_SCHEDULER_CALENDARS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-calendar",
            "restricted": false,
            "id": "new-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-calendar",
            "restricted": false,
            "id": "update-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "scheduler-calendars",
        "icon": "calendar",
        "restricted": false,
        "text": "MENU_SCHEDULER_CALENDARS",
        "id": "scheduler-calendars",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "scheduler-calendars",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "scheduler-calendars",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-scheduler-calendar",
            "restricted": false,
            "id": "new-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-scheduler-calendar",
            "restricted": false,
            "id": "update-scheduler-calendar",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-scheduler-calendar",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-scheduler-calendar",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      }
    ]
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "notifier-separator",
    "restricted": false,
    "id": "notifier-separator",
    "reload": false,
    "separator": true,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "notifier-separator",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "notifier-separator",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": []
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "user-settings",
    "restricted": false,
    "id": "user-settings",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "user-settings",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "user-settings",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": false,
    "options": []
  },
  {
    "elementType": "Option",
    "label": "MENU_NOTIFIER",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_NOTIFIER_SUBSCRIPTIONS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-subscription",
            "restricted": false,
            "id": "new-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-subscription",
            "restricted": false,
            "id": "update-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "subscriptions",
        "icon": "ticket",
        "restricted": false,
        "text": "MENU_NOTIFIER_SUBSCRIPTIONS",
        "id": "subscriptions",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "subscriptions",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "subscriptions",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-subscription",
            "restricted": false,
            "id": "new-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-subscription",
            "restricted": false,
            "id": "update-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_NOTIFIER_NOTIFICATIONS",
        "elementList": [],
        "name": "notifications",
        "icon": "bell",
        "restricted": false,
        "text": "MENU_NOTIFIER_NOTIFICATIONS",
        "id": "notifications",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "notifications",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "notifications",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "notifier",
    "icon": "flash",
    "restricted": false,
    "text": "MENU_NOTIFIER",
    "id": "notifier",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "notifier",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "notifier",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_NOTIFIER_SUBSCRIPTIONS",
        "elementList": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-subscription",
            "restricted": false,
            "id": "new-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-subscription",
            "restricted": false,
            "id": "update-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ],
        "name": "subscriptions",
        "icon": "ticket",
        "restricted": false,
        "text": "MENU_NOTIFIER_SUBSCRIPTIONS",
        "id": "subscriptions",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "subscriptions",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "subscriptions",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": [
          {
            "elementType": "Option",
            "elementList": [],
            "name": "new-subscription",
            "restricted": false,
            "id": "new-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "new-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "new-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          },
          {
            "elementType": "Option",
            "elementList": [],
            "name": "update-subscription",
            "restricted": false,
            "id": "update-subscription",
            "reload": false,
            "separator": false,
            "menuScreen": false,
            "actions": [
              {
                "elementType": "ButtonAction",
                "type": "screen",
                "elementList": [],
                "target": "update-subscription",
                "context": "screen/private/home",
                "silent": false,
                "async": false,
                "reload": false,
                "parameters": {
                  "target": "update-subscription",
                  "value": null,
                  "label": null,
                  "serverAction": "screen",
                  "targetAction": null,
                  "reload": false
                }
              }
            ],
            "dynamic": false,
            "visible": false,
            "options": []
          }
        ]
      },
      {
        "elementType": "Option",
        "label": "MENU_NOTIFIER_NOTIFICATIONS",
        "elementList": [],
        "name": "notifications",
        "icon": "bell",
        "restricted": false,
        "text": "MENU_NOTIFIER_NOTIFICATIONS",
        "id": "notifications",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "notifications",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "notifications",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "developer-separator",
    "restricted": false,
    "id": "developer-separator",
    "reload": false,
    "separator": true,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "developer-separator",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "developer-separator",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": []
  },
  {
    "elementType": "Option",
    "label": "MENU_DEVELOPER",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_PATH",
        "elementList": [],
        "name": "path-manager",
        "icon": "terminal",
        "restricted": false,
        "text": "MENU_PATH",
        "id": "path-manager",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "path-manager",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "path-manager",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_LANGUAGES",
        "elementList": [],
        "name": "local-manager",
        "icon": "language",
        "restricted": false,
        "text": "MENU_LANGUAGES",
        "id": "local-manager",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "local-manager",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "local-manager",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "developer",
    "icon": "paint-brush",
    "restricted": false,
    "text": "MENU_DEVELOPER",
    "id": "developer",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "developer",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "developer",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_PATH",
        "elementList": [],
        "name": "path-manager",
        "icon": "terminal",
        "restricted": false,
        "text": "MENU_PATH",
        "id": "path-manager",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "path-manager",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "path-manager",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_LANGUAGES",
        "elementList": [],
        "name": "local-manager",
        "icon": "language",
        "restricted": false,
        "text": "MENU_LANGUAGES",
        "id": "local-manager",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "local-manager",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "local-manager",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  },
  {
    "elementType": "Option",
    "elementList": [],
    "name": "help-separator",
    "restricted": false,
    "id": "help-separator",
    "reload": false,
    "separator": true,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "help-separator",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "help-separator",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": []
  },
  {
    "elementType": "Option",
    "label": "MENU_HELP",
    "elementList": [
      {
        "elementType": "Option",
        "label": "MENU_USER_HELP",
        "elementList": [],
        "name": "user-help",
        "icon": "book",
        "restricted": false,
        "text": "MENU_USER_HELP",
        "id": "user-help",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "user-help",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "user-help",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_HELP_APPLICATION",
        "elementList": [],
        "name": "application-help",
        "icon": "question",
        "restricted": false,
        "text": "MENU_HELP_APPLICATION",
        "id": "application-help",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-help",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-help",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_APP_INF",
        "elementList": [],
        "name": "application-info",
        "icon": "info",
        "restricted": false,
        "text": "MENU_APP_INF",
        "id": "application-info",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-info",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-info",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ],
    "name": "help",
    "icon": "question-circle",
    "restricted": false,
    "text": "MENU_HELP",
    "id": "help",
    "reload": false,
    "separator": false,
    "menuScreen": false,
    "actions": [
      {
        "elementType": "ButtonAction",
        "type": "screen",
        "elementList": [],
        "target": "help",
        "context": "screen/private/home",
        "silent": false,
        "async": false,
        "reload": false,
        "parameters": {
          "target": "help",
          "value": null,
          "label": null,
          "serverAction": "screen",
          "targetAction": null,
          "reload": false
        }
      }
    ],
    "dynamic": false,
    "visible": true,
    "options": [
      {
        "elementType": "Option",
        "label": "MENU_USER_HELP",
        "elementList": [],
        "name": "user-help",
        "icon": "book",
        "restricted": false,
        "text": "MENU_USER_HELP",
        "id": "user-help",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "user-help",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "user-help",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_HELP_APPLICATION",
        "elementList": [],
        "name": "application-help",
        "icon": "question",
        "restricted": false,
        "text": "MENU_HELP_APPLICATION",
        "id": "application-help",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-help",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-help",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      },
      {
        "elementType": "Option",
        "label": "MENU_APP_INF",
        "elementList": [],
        "name": "application-info",
        "icon": "info",
        "restricted": false,
        "text": "MENU_APP_INF",
        "id": "application-info",
        "reload": false,
        "separator": false,
        "menuScreen": false,
        "actions": [
          {
            "elementType": "ButtonAction",
            "type": "screen",
            "elementList": [],
            "target": "application-info",
            "context": "screen/private/home",
            "silent": false,
            "async": false,
            "reload": false,
            "parameters": {
              "target": "application-info",
              "value": null,
              "label": null,
              "serverAction": "screen",
              "targetAction": null,
              "reload": false
            }
          }
        ],
        "dynamic": false,
        "visible": true,
        "options": []
      }
    ]
  }
];

describe('awe-react-client/test/js/components/AweMenuTest.jsx', () => {

  it('renders Awe Menu horizontal component', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      menu: {
        options,
        breadcrumbs: [{option: "tuut", label: "Tutut"}, {option: "lala", label: "aaaAA"}],
      },
      components: {
        menu: {
          address: {component: 'grid', view: 'report'},
          screen: {
            breadcrumbs: [],
            report: {name: "opcion", option: "opcion"}
          },
          model: {values: []},
          attributes: {

          }
        }
      }
    };

    renderWithProviders(<AweMenu id="menu"/>, {preloadedState});

    // check
    expect(document.querySelector(".p-menubar")).toBeDefined();
  });

  it('renders Awe Menu vertical component', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        view: "report",
        size: {width: 100, height: 100},
        breadcrumbs: {
          option: "application-info",
          items: []
        },
        report: {
          option: "sites",
          title: "Sites"
        }
      },
      menu: {
        options,
        currentOption: {option: "application-info"}
      },
      components: {
        menu: {
          address: {component: 'grid', view: 'report'},
          screen: {
            breadcrumbs: [],
            report: {name: "opcion", option: "opcion"}
          },
          model: {values: []},
          attributes: {
          }
        }
      }
    };

    renderWithProviders(<AweMenu id="menu" style="vertical"/>, {preloadedState});

    // check
    expect(document.querySelector(".p-menubar")).toBeDefined();
  });

});
