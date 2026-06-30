import {formatNumber, translateNumberFormat} from "../../../src/utilities/numbers";

describe("awe-react-client/test/js/utilities/numbersTest.jsx", () => {
  it("uses English separators when both decimal and grouping characters are configured", () => {
    expect(translateNumberFormat({ decimalCharacter: ".", digitGroupSeparator: ",", precision: 2 })).toMatchObject({
      locale: "en-US",
      useGrouping: true,
      minFractionDigits: 2,
      maxFractionDigits: 2
    });
    expect(formatNumber(1000.5, { decimalCharacter: ".", digitGroupSeparator: ",", precision: 2 })).toBe("1,000.50");
  });

  it("uses German separators when decimal and grouping characters are inverted", () => {
    expect(translateNumberFormat({ decimalCharacter: ",", digitGroupSeparator: ".", precision: 2 })).toMatchObject({
      locale: "de",
      useGrouping: true,
      minFractionDigits: 2,
      maxFractionDigits: 2
    });
    expect(formatNumber(1000.5, { decimalCharacter: ",", digitGroupSeparator: ".", precision: 2 })).toBe("1.000,50");
  });

  it("keeps default English separator semantics for partial formats", () => {
    expect(translateNumberFormat({ min: 0, precision: 2 })).toMatchObject({
      locale: "en-US",
      useGrouping: true,
      minFractionDigits: 2,
      maxFractionDigits: 2,
      min: 0
    });
    expect(formatNumber(1000.5, { min: 0, precision: 2 })).toBe("1,000.50");
  });

  it("normalizes values according to configured precision without changing numeric intent", () => {
    expect(formatNumber(1000.5, { precision: 0 })).toBe("1,001");
  });
});
