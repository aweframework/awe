// The registry of components goes first: the client modules import each other (a component renders its children
// through structure.tsx, which reads this registry), and the cycle only works when it is entered through the registry
import '@awe-client/components/AweComponents';

// Real components of the AWE React client, ready to use in MDX: <AweButton id="..." />
export {default as AweButton} from '@awe-client/components/AweButton';
// The window template of a screen: its buttons row, breadcrumb and center, hidden and modal sources
export {default as WindowTemplate} from '@awe-client/templates/WindowTemplate';
// Renders a node of the structure of a screen ({elementType, id, elementList...}), like the client does for a screen
export {Components} from '@awe-client/utilities/structure';
