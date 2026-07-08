import {flattenMenuOptions, searchOptions, normalizeText} from "../../../main/resources/js/awe/data/menuSearch";

// Simple dictionary-based translate mock (label key -> visible text)
const dictionary = {
  "SALES": "Ventas",
  "INVOICES": "Facturas",
  "NEW_INVOICE": "Nueva factura",
  "INVOICE_LIST": "Listado de facturas",
  "PURCHASES": "Compras",
  "BILLING": "Facturación",
  "RECEIVED_INVOICES": "Facturas recibidas"
};
const translate = (key) => dictionary[String(key || "")] || String(key || "");

// Default allowed predicate mirroring allowedOption filter (visible and not restricted)
const isAllowed = (option) => option.visible !== false && !option.restricted;

// Sample deep menu tree (JSON shape served for Option entities)
const menu = [
  {
    name: "sales", label: "SALES", visible: true, options: [
      {
        name: "invoices", label: "INVOICES", visible: true, options: [
          {name: "newInvoice", label: "NEW_INVOICE", visible: true, actions: [{type: "screen"}]},
          {name: "invoiceList", label: "INVOICE_LIST", visible: true, actions: [{type: "screen"}]}
        ]
      }
    ]
  },
  {
    name: "purchases", label: "PURCHASES", visible: true, options: [
      {
        name: "billing", label: "BILLING", visible: true, options: [
          {name: "receivedInvoices", label: "RECEIVED_INVOICES", visible: true, actions: [{type: "screen"}]}
        ]
      }
    ]
  }
];

describe('awe-framework/awe-client-angular/src/main/resources/js/awe/data/menuSearch.js', function () {

  describe('normalizeText', function () {
    it('should lowercase and strip accents', function () {
      expect(normalizeText("Facturación")).toBe("facturacion");
    });

    it('should handle null/undefined safely', function () {
      expect(normalizeText(null)).toBe("");
      expect(normalizeText(undefined)).toBe("");
    });
  });

  describe('flattenMenuOptions', function () {
    it('should flatten the tree into leaf options with translated breadcrumb paths', function () {
      const result = flattenMenuOptions(menu, {translate, isAllowed});

      expect(result).toHaveLength(3);
      expect(result[0].path).toEqual(["Ventas", "Facturas", "Nueva factura"]);
      expect(result[0].option.name).toBe("newInvoice");
      expect(result[2].path).toEqual(["Compras", "Facturación", "Facturas recibidas"]);
    });

    it('should exclude separators', function () {
      const withSeparator = [
        {name: "a", label: "SALES", visible: true, separator: true},
        {name: "b", label: "PURCHASES", visible: true, actions: []}
      ];
      const result = flattenMenuOptions(withSeparator, {translate, isAllowed});

      expect(result).toHaveLength(1);
      expect(result[0].option.name).toBe("b");
    });

    it('should exclude options rejected by isAllowed', function () {
      const restricted = [
        {name: "a", label: "SALES", visible: true, actions: []},
        {name: "b", label: "PURCHASES", visible: true, restricted: true, actions: []}
      ];
      const result = flattenMenuOptions(restricted, {translate, isAllowed});

      expect(result).toHaveLength(1);
      expect(result[0].option.name).toBe("a");
    });

    it('should default isAllowed to allow-all when the predicate is omitted', function () {
      const result = flattenMenuOptions([{name: "sales", label: "SALES"}], {translate});

      expect(result).toHaveLength(1);
      expect(result[0].path).toEqual(["Ventas"]);
    });

    it('should fall back to the option name when there is no label', function () {
      const result = flattenMenuOptions([{name: "only-name"}], {translate, isAllowed});

      expect(result[0].path).toEqual(["only-name"]);
    });

    it('should treat a branch with no allowed children as a leaf itself', function () {
      const branch = [
        {
          name: "sales", label: "SALES", visible: true, options: [
            {name: "hidden", label: "INVOICES", visible: false}
          ]
        }
      ];
      const result = flattenMenuOptions(branch, {translate, isAllowed});

      expect(result).toHaveLength(1);
      expect(result[0].option.name).toBe("sales");
      expect(result[0].path).toEqual(["Ventas"]);
    });
  });

  describe('searchOptions', function () {
    it('should return an empty array for a blank query', function () {
      expect(searchOptions(menu, "", {translate, isAllowed})).toEqual([]);
      expect(searchOptions(menu, "   ", {translate, isAllowed})).toEqual([]);
    });

    it('should match by leaf label, accent-insensitive', function () {
      const result = searchOptions(menu, "facturacion", {translate, isAllowed});

      expect(result.map(r => r.option.name)).toEqual(["receivedInvoices"]);
    });

    it('should match every token independently (AND semantics)', function () {
      const result = searchOptions(menu, "nue fact", {translate, isAllowed});

      expect(result.map(r => r.option.name)).toEqual(["newInvoice"]);
    });

    it('should match by option name as well as label', function () {
      const result = searchOptions(menu, "invoicelist", {translate, isAllowed});

      expect(result.map(r => r.option.name)).toEqual(["invoiceList"]);
    });

    it('should match ancestors through the breadcrumb path', function () {
      const result = searchOptions(menu, "ventas", {translate, isAllowed});

      expect(result.map(r => r.option.name)).toEqual(["newInvoice", "invoiceList"]);
    });
  });
});
