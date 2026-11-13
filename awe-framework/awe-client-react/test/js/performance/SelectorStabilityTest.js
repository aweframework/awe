import fs from "fs";
import path from "path";

const SRC = path.join(__dirname, "..", "..", "..", "src");

const sourceFiles = (dir) => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(dir, entry.name);
  if (entry.isDirectory()) {
    return sourceFiles(file);
  }
  return /\.jsx?$/.test(entry.name) ? [file] : [];
});

describe("awe-react-client/test/js/performance/SelectorStabilityTest.js", () => {
  // useSelector(state => ({...})) or useSelector((state) => [...]) builds a new value on every call, so the component
  // renders again on every dispatch unless an equality function is given
  const freshValueSelector = /useSelector\(\s*\(?\s*state\s*\)?\s*=>\s*(\(\s*\{|\[)/g;

  it("has no useSelector that returns a fresh object or array without an equality function", () => {
    const offenders = sourceFiles(SRC).flatMap(file => {
      const source = fs.readFileSync(file, "utf8");
      return [...source.matchAll(freshValueSelector)]
        .map(match => {
          // The selector closes where its call closes: the equality function would follow the selector argument
          const rest = source.slice(match.index);
          const call = rest.slice(0, rest.indexOf(");") + 2);
          return /shallowEqual|isEqual/.test(call) ? null : `${path.relative(SRC, file)}: ${match[0]}`;
        })
        .filter(Boolean);
    });

    expect(offenders).toEqual([]);
  });
});
