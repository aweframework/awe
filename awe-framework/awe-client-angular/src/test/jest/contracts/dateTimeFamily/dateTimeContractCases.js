module.exports = {
  dateValue: {
    mode: "date",
    controller: {
      dateFormat: "dd/mm/yyyy"
    },
    update: {selected: "23/10/2024"},
    expected: {
      selected: "23/10/2024",
      visible: "23/10/2024"
    }
  },
  timeValue: {
    mode: "time",
    update: {selected: "14:35:59"},
    expected: {
      selected: "14:35:59",
      visible: "14:35:59"
    }
  },
  emptyValue: {
    mode: "date",
    update: {selected: ""},
    expected: {
      selected: "",
      visible: ""
    }
  },
  nullValue: {
    mode: "time",
    update: {selected: null},
    expected: {
      selected: null,
      visible: ""
    }
  },
  wrappedSelection: {
    mode: "date",
    selected: [{value: "05/01/2026", label: "ignored"}],
    expected: {
      selected: "05/01/2026",
      visible: "05/01/2026"
    }
  }
};
