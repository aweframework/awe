import {useEffect, useState} from "react";
import {observeModeClasses} from "../utilities/themeAppearance";
import {isDarkMode, observeSystemMode} from "../utilities/signin";

/**
 * Resolved colour mode of the page: the mode class of <html> (light or dark), else the system preference. It follows
 * the changes of both
 * @returns {boolean} The colour mode is dark
 */
export function useDarkMode() {
  const [dark, setDark] = useState(() => isDarkMode());

  useEffect(() => {
    const update = () => setDark(isDarkMode());
    const stopClasses = observeModeClasses(document.documentElement, ["light", "dark"], update);
    const stopSystem = observeSystemMode(update);
    update();
    return () => {
      stopClasses();
      stopSystem();
    };
  }, []);

  return dark;
}
