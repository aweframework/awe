module.exports = {
  basicPayload: {
    id: "GrdContract",
    selection: [4],
    specificFields: {
      max: 25,
      page: 1,
      sort: [{id: "value", direction: "asc"}]
    },
    expectedData: {
      GrdContract: [4],
      "GrdContract.data": {
        max: 25,
        page: 1,
        sort: [{id: "value", direction: "asc"}]
      },
      id: [4],
      "id.selected": 4,
      value: ["lele"],
      "value.selected": "lele",
      other: ["asda"],
      "other.selected": "asda",
      "GrdContract-id": [4],
      "GrdContract.selectedRowAddress": {view: "report", component: "GrdContract", row: 4}
    }
  },
  multipleSelection: {
    id: "GrdMultiContract",
    selection: [1, 4],
    specificFields: {
      max: 50,
      page: 3,
      sort: [{id: "id", direction: "desc"}]
    },
    expectedData: {
      GrdMultiContract: [1, 4],
      "GrdMultiContract.data": {
        max: 50,
        page: 3,
        sort: [{id: "id", direction: "desc"}]
      },
      id: [1, 4],
      "id.selected": [1, 4],
      value: ["tutu", "lele"],
      "value.selected": ["tutu", "lele"],
      other: [null, "asda"],
      "other.selected": [null, "asda"],
      "GrdMultiContract-id": [1, 4]
    }
  },
  sanitizedSelection: {
    id: "GrdSanitizedContract",
    selection: [4, 999],
    expectedSelection: [4],
    expectedData: {
      GrdSanitizedContract: [4],
      "GrdSanitizedContract.data": {
        max: 20,
        page: 1,
        sort: []
      },
      id: [4],
      "id.selected": 4,
      value: ["lele"],
      "value.selected": "lele",
      other: ["asda"],
      "other.selected": "asda",
      "GrdSanitizedContract-id": [4],
      "GrdSanitizedContract.selectedRowAddress": {view: "report", component: "GrdSanitizedContract", row: 4}
    }
  },
  emptySelection: {
    id: "GrdEmptyContract",
    selection: [],
    expectedData: {
      GrdEmptyContract: null,
      "GrdEmptyContract.data": {
        max: 20,
        page: 1,
        sort: []
      },
      id: [],
      "id.selected": null,
      value: [],
      "value.selected": null,
      other: [],
      "other.selected": null,
      "GrdEmptyContract-id": []
    }
  },
  serverAggregation: {
    id: "GrdServerContract",
    selection: [4],
    specificFields: {
      max: 100,
      page: 2,
      sort: [{id: "value", direction: "desc"}]
    },
    expectedData: {
      GrdServerContract: [4],
      "GrdServerContract.data": {
        max: 100,
        page: 2,
        sort: [{id: "value", direction: "desc"}]
      },
      id: [4],
      "id.selected": 4,
      value: ["lele"],
      "value.selected": "lele",
      other: ["asda"],
      "other.selected": "asda",
      "GrdServerContract-id": [4],
      "GrdServerContract.selectedRowAddress": {view: "report", component: "GrdServerContract", row: 4}
    },
    targetSpecificFields: {
      max: 15,
      page: 9,
      sort: [{id: "other", direction: "asc"}]
    }
  }
};
