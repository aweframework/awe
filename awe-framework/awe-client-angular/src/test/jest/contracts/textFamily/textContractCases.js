module.exports = {
  textValue: {
    id: "CrtText",
    update: {selected: "plain text contract"},
    expected: {
      selected: "plain text contract",
      visible: "plain text contract"
    }
  },
  emptyValue: {
    id: "CrtEmptyText",
    update: {selected: ""},
    expected: {
      selected: "",
      visible: ""
    }
  },
  nullValue: {
    id: "CrtNullableText",
    update: {selected: null},
    expected: {
      selected: null,
      visible: ""
    }
  },
  wrappedSelection: {
    id: "CrtWrappedText",
    update: {
      selected: [{value: "normalized text", label: "Ignored label"}]
    },
    expected: {
      selected: "normalized text",
      visible: "normalized text"
    }
  },
  textareaValue: {
    id: "TxtNarrative",
    controller: {
      areaRows: 4
    },
    update: {selected: "line 1\nline 2"},
    expected: {
      selected: "line 1\nline 2",
      visible: "line 1\nline 2"
    }
  }
};
