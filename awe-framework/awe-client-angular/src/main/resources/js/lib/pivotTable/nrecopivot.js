//
// NReco PivotTable Extensions
// Author: Vitaliy Fedorchenko
//
// Copyright (c) nrecosite.com - All Rights Reserved
// THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
// KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
// PARTICULAR PURPOSE.
//
  (function () {
    let  $;

    $ = jQuery;

    let  applyDrillDownHandler = function (wrapper, pvtData, tElem) {
      if (!wrapper.options.drillDownHandler)
        return;
      $(tElem).addClass('pvtValDrillDown').on("click", "td.pvtVal,td.pvtTotal", function () {
        let  cssClasses = $(this).attr('class').split(' ');
        let  colIdx = -1, rowIdx = -1;
        if ($.inArray("pvtVal", cssClasses) >= 0) {
          $.each(cssClasses, function () {
            if (this.indexOf('row') == 0)
              rowIdx = parseInt(this.substring(3));
            if (this.indexOf('col') == 0)
              colIdx = parseInt(this.substring(3));
          });
        }
        if ($.inArray("rowTotal", cssClasses) >= 0) {
          let  dataFor = $(this).attr('data-for');
          rowIdx = parseInt(dataFor.substring(3));
        }
        if ($.inArray("colTotal", cssClasses) >= 0) {
          let  dataFor = $(this).attr('data-for');
          colIdx = parseInt(dataFor.substring(3));
        }
        let  dataFilter = {};
        if (colIdx >= 0) {
          for (let  cAttrIdx = 0; cAttrIdx < pvtData.colAttrs.length; cAttrIdx++) {
            let  colKeys = pvtData.getColKeys();
            let  cValues = colKeys[colIdx];
            dataFilter[pvtData.colAttrs[cAttrIdx]] = cValues[cAttrIdx];
          }
        }
        if (rowIdx >= 0) {
          for (let  rAttrIdx = 0; rAttrIdx < pvtData.rowAttrs.length; rAttrIdx++) {
            let  rowKeys = pvtData.getRowKeys();
            let  rValues = rowKeys[rowIdx];
            dataFilter[pvtData.rowAttrs[rAttrIdx]] = rValues[rAttrIdx];
          }
        }
        wrapper.options.drillDownHandler(dataFilter);
      });
    };

    let  sortDataByCol = function (pvtData, sortByColIdx, ascDesc) {
      let  sortRowVals = [];
      let  rowKey, colKey, aggregator, i;

      pvtData.sorted = false; // flush row/col order
      let  rowKeys = pvtData.getRowKeys();
      let  colKeys = pvtData.getColKeys();

      for (i in rowKeys) {
        rowKey = rowKeys[i];
        colKey = sortByColIdx != null ? colKeys[sortByColIdx] : [];
        aggregator = pvtData.getAggregator(rowKey, colKey);
        sortRowVals.push({val: aggregator.value(), key: rowKey});
      }
      sortRowVals.sort(function (a, b) {
        return ascDesc * window.pivotSortFunction(a.val, b.val);
      });
      pvtData.rowKeys = [];
      for (i = 0; i < sortRowVals.length; i++)
        pvtData.rowKeys.push(sortRowVals[i].key);
      pvtData.sorted = true;
    };

    let  sortDataByRow = function (pvtData, sortByRowIdx, ascDesc) {
      let  sortColVals = [];
      let  rowKey, colKey, aggregator, i;

      pvtData.sorted = false; // flush row/col order
      let  rowKeys = pvtData.getRowKeys();
      let  colKeys = pvtData.getColKeys();

      for (i in colKeys) {
        colKey = colKeys[i];
        rowKey = sortByRowIdx != null ? rowKeys[sortByRowIdx] : [];
        aggregator = pvtData.getAggregator(rowKey, colKey);
        sortColVals.push({val: aggregator.value(), key: colKey});
      }
      sortColVals.sort(function (a, b) {
        return ascDesc * window.pivotSortFunction(a.val, b.val);
      });
      pvtData.colKeys = [];
      for (i = 0; i < sortColVals.length; i++)
        pvtData.colKeys.push(sortColVals[i].key);
      pvtData.sorted = true;
    };

    let  applySortHandler = function (wrapper, pvtData, opts, tElem, refreshTable) {
      let  applyAscDescClass = function ($elem, direction) {
        $elem.addClass(direction == "desc" ? "pvtSortDesc" : "pvtSortAsc");
      };
      let  applySort = function (keys, labels, optSortKey, doSort) {
        labels.click(function () {
          let  $lbl = $(this);
          let  keyIdx = $lbl.data('key_index');
          let  key = keys[keyIdx];

          if ($lbl.hasClass("pvtSortAsc")) {
            doSort(pvtData, keyIdx, -1);
            opts.sort = {direction: "desc"};
            opts.sort[optSortKey] = key;
          } else if ($lbl.hasClass("pvtSortDesc")) {
            pvtData.sorted = false;
            opts.sort = null;
          } else {
            doSort(pvtData, keyIdx, 1);
            opts.sort = {direction: "asc"};
            opts.sort[optSortKey] = key;
          }
          refreshTable();
        }).each(function () {
          if (opts.sort && opts.sort[optSortKey]) {
            let  $lbl = $(this);
            let  key = keys[$lbl.data('key_index')];
            if (key.join('_') == opts.sort[optSortKey].join('_')) {
              applyAscDescClass($lbl, opts.sort.direction);
            }
          }
        });
      };
      let  markSortableLabels = function (keys, $labels) {
        let  i = 0;
        $labels.each(function () {
          let  $lbl = $(this);
          let  lblText = $.trim($lbl.text());
          let  k = keys[i];
          if (k != null && k.length > 0 && k[k.length - 1] == lblText) {
            $lbl.addClass("pvtSortable").data('key_index', i);
            i++;
            return;
          }
        });
      };
      let  colKeys = pvtData.getColKeys();
      markSortableLabels(colKeys, $(tElem).find('.pvtColLabel[colspan="1"]'));
      applySort(colKeys, $(tElem).find('.pvtColLabel.pvtSortable[colspan="1"]'), "column_key", sortDataByCol);

      let  rowKeys = pvtData.getRowKeys();
      markSortableLabels(rowKeys, $(tElem).find('.pvtRowLabel[rowspan="1"]'));
      applySort(rowKeys, $(tElem).find('.pvtRowLabel.pvtSortable[rowspan="1"]'), "row_key", sortDataByRow);

      $(tElem).find('tr:not(:first) .pvtTotalLabel').addClass("pvtTotalColSortable").click(function () {
        let  $lbl = $(this);
        if ($lbl.hasClass("pvtSortAsc")) {
          sortDataByRow(pvtData, null, -1);
          opts.sort = {direction: "desc", row_totals: true};
        } else if ($lbl.hasClass("pvtSortDesc")) {
          pvtData.sorted = false;
          opts.sort = null;
        } else {
          sortDataByRow(pvtData, null, 1);
          opts.sort = {direction: "asc", row_totals: true};
        }
        refreshTable();
      }).each(function () {
        let  $lbl = $(this);
        if (opts.sort && opts.sort.row_totals) {
          applyAscDescClass($lbl, opts.sort.direction);
        }
      });

      $(tElem).find('tr:first .pvtTotalLabel').addClass("pvtTotalRowSortable").click(function () {
        let  $lbl = $(this);
        if ($lbl.hasClass("pvtSortAsc")) {
          sortDataByCol(pvtData, null, -1);
          opts.sort = {direction: "desc", col_totals: true};
        } else if ($lbl.hasClass("pvtSortDesc")) {
          pvtData.sorted = false;
          opts.sort = null;
        } else {
          sortDataByCol(pvtData, null, 1);
          opts.sort = {direction: "asc", col_totals: true};
        }
        refreshTable();
      }).each(function () {
        let  $lbl = $(this);
        if (opts.sort && opts.sort.col_totals) {
          applyAscDescClass($lbl, opts.sort.direction);
        }
      });

    };
    let  preparePivotData = function (pvtData) {
      let  i, j, aggregator;
      let  colKeys = pvtData.getColKeys();
      let  rowKeys = pvtData.getRowKeys();
      let  data = [];
      let  totalsRow = [];
      let  totalsCol = [];
      for (i in rowKeys) {
        data[i] = [];
        for (j in colKeys) {
          aggregator = pvtData.getAggregator(rowKeys[i], colKeys[j]);
          data[i][j] = aggregator.value();
        }
        totalsCol[i] = pvtData.getAggregator(rowKeys[i], []).value();
      }
      for (j in colKeys) {
        totalsRow[j] = pvtData.getAggregator([], colKeys[j]).value();
      }
      return {
        columnKeys: colKeys,
        columnAttrs: pvtData.colAttrs,
        rowKeys: rowKeys,
        rowAttrs: pvtData.rowAttrs,
        matrix: data,
        totals: {row: totalsRow, column: totalsCol}
      };
    };

    window.NRecoPivotTableExtensions = function (options) {
      this.options = $.extend(NRecoPivotTableExtensions.defaults, options);
    };

    window.NRecoPivotTableExtensions.prototype.sortDataByOpts = function (pvtData, opts) {
      pvtData.sorted = false;
      if (opts && opts.sort) {
        let  ascDesc = opts.sort.direction == "desc" ? -1 : 1;
        if (opts.sort.column_key) {
          let  colKeys = pvtData.getColKeys();
          let  sortByKeyStr = opts.sort.column_key.join('_');
          for (let  i in colKeys)
            if (sortByKeyStr == colKeys[i].join('_')) {
              sortDataByCol(pvtData, i, ascDesc);
            }
        } else if (opts.sort.row_key) {
          let  rowKeys = pvtData.getRowKeys();
          let  sortByKeyStr = opts.sort.row_key.join('_');
          for (let  i in rowKeys)
            if (sortByKeyStr == rowKeys[i].join('_')) {
              sortDataByRow(pvtData, i, ascDesc);
            }
        } else if (opts.sort.row_totals) {
          sortDataByRow(pvtData, null, ascDesc);
        } else if (opts.sort.col_totals) {
          sortDataByCol(pvtData, null, ascDesc);
        }
      }
    };

    window.NRecoPivotTableExtensions.prototype.wrapTableRenderer = function (tableRenderer) {
      let  wrapper = this;
      return function (pvtData, opts) {
        let  tElem, refreshTable, wrapTable;
        if (opts)
          wrapper.sortDataByOpts(pvtData, opts);
        tElem = tableRenderer(pvtData, opts);
        wrapTable = function ($t) {
          if (wrapper.options.wrapWith) {
            let  $w = $(wrapper.options.wrapWith);
            $w.append($t);
            $t = $w;
          }
          return $t;
        };
        refreshTable = function () {
          let  newTbl = tableRenderer(pvtData, opts);
          applyDrillDownHandler(wrapper, pvtData, newTbl);
          applySortHandler(wrapper, pvtData, opts, newTbl, refreshTable);
          $(tElem).replaceWith(newTbl);
          tElem = newTbl;
        };
        applyDrillDownHandler(wrapper, pvtData, tElem);
        applySortHandler(wrapper, pvtData, opts, tElem, refreshTable);
        return wrapTable(tElem);
      };
    };

    window.NRecoPivotTableExtensions.prototype.wrapPivotExportRenderer = function (renderer) {
      return function (pvtData, opts) {
        let  elem = renderer(pvtData, opts);
        $(elem).addClass("pivotExportData").data("getPivotExportData", function () {
          return preparePivotData(pvtData);
        });
        return elem;
      };
    };

    window.NRecoPivotTableExtensions.defaults = {
      drillDownHandler: null,
      wrapWith: null
    };

  }).call(this);