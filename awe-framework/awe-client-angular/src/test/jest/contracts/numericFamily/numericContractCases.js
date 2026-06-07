module.exports = {
  decimalValue: {
    controller: {
      numberFormat: {min: 50, max: 100000, precision: 2, aSep: ".", aDec: ","}
    },
    update: {selected: 1200.21},
    expected: {
      selected: 1200.21,
      values: [{value: 1200.21, label: "1.200,21"}],
      visible: "1.200,21"
    }
  },
  zeroValue: {
    controller: {
      numberFormat: {min: 0, precision: 0}
    },
    update: {selected: 0},
    expected: {
      selected: 0,
      values: [{value: 0, label: "0"}],
      visible: "0"
    }
  },
  emptyValue: {
    controller: {
      numberFormat: {min: 0, precision: 0}
    },
    update: {selected: ""},
    expected: {
      selected: "",
      values: [{value: "", label: ""}],
      visible: ""
    }
  },
  nullValue: {
    controller: {
      numberFormat: {min: 0, precision: 0}
    },
    update: {selected: null},
    expected: {
      selected: null,
      values: [{value: null, label: ""}],
      visible: ""
    }
  },
  wrappedSelection: {
    controller: {
      numberFormat: {min: 0, precision: 0}
    },
    selected: [{value: 7, label: "ignored"}],
    expected: {
      selected: 7,
      values: [{value: 7, label: "7"}],
      visible: "7"
    }
  }
};
