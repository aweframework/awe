module.exports = {
  selectSimple: {
    values: [
      {value: "A", label: "Alpha"},
      {value: "B", label: "Beta"}
    ],
    selected: ["B"]
  },
  selectInvalidRequired: {
    values: [
      {value: "A", label: "Alpha"},
      {value: "B", label: "Beta"}
    ],
    selected: "Z"
  },
  suggestStrictUnresolved: {
    values: [{value: "resolved", label: "Resolved"}],
    storedValues: [{value: "resolved", label: "Resolved"}],
    selected: ["missing"]
  },
  suggestFreeText: {
    values: [],
    storedValues: [],
    selected: "free-text"
  },
  suggestMultipleCommaValue: {
    values: [
      {value: "a,test", label: "a,test"},
      {value: "plain", label: "plain"}
    ],
    storedValues: [{value: "a,test", label: "a,test"}],
    selected: null
  }
};
