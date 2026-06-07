module.exports = {
  checkbox: {
    checked: {
      id: "CrtBooleanChecked",
      model: {
        selected: 0
      }
    },
    unchecked: {
      id: "CrtBooleanUnchecked",
      model: {
        selected: 1
      }
    },
    defaulted: {
      id: "ButBooleanDefault",
      model: {
        selected: 0,
        defaultValues: "Y"
      }
    },
    stringChecked: {
      id: "ButBooleanString",
      model: {
        selected: 0
      },
      update: {
        selected: ["1"]
      }
    }
  },
  radio: {
    active: {
      id: "RadBooleanActive",
      group: "GrpBooleanState",
      model: {
        selected: null,
        values: [{value: "YES", label: "Yes"}]
      }
    },
    inactive: {
      id: "RadBooleanInactive",
      group: "GrpBooleanInactive",
      model: {
        selected: null,
        values: [{value: "YES", label: "Yes"}]
      }
    },
    normalized: {
      id: "ButBooleanRadio",
      group: "GrpBooleanMode",
      model: {
        selected: null,
        values: [{value: "ON", label: "On"}]
      },
      update: {
        selected: ["ON"]
      }
    }
  }
};
