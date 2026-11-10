// CSS the real AWE React components need. It is the list main.jsx of the client imports, without what styles the
// application shell (body, #root, layout, PrimeFlex utilities) and without color-scheme of main.css: the colour mode
// is the one of the Docusaurus theme (color-scheme of Infima), which the tokens read through light-dark().
import '@awe-tokens/tokens.css';
import '@awe-tokens/icons/awe-icon.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
// The AWE theme of PrimeReact: plugins/awe-live builds it from this Lara theme
import 'primereact/resources/themes/lara-light-indigo/theme.css';
