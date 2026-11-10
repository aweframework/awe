import type {MenuOption} from "../types";

type Listener = () => void;

/**
 * A level of the breadcrumb: a menu group (it opens the menu at it), a menu option (its actions launch it) or a screen
 * the user came from (its path goes back to it); a level with none of them is plain text
 */
export interface BreadcrumbLevel {
  label: string;
  name?: string;
  group?: boolean;
  actions?: unknown[];
  path?: string;
  /** Option of the screen a level goes back to, to know whether the next screen is on its menu path */
  option?: string;
}

/** Breadcrumbs of the menu: the trail of the current option and its key */
interface Breadcrumbs {
  items: BreadcrumbLevel[];
  option: string | null;
}

/** Request to open the menu expanded at a group; the token makes a repeated request for the same group a new one */
export interface ExpandRequest {
  name: string;
  token: number;
}

class MenuRegistry {
  options: MenuOption[] = [];
  breadcrumbs: Breadcrumbs = { items: [], option: null };
  /** Levels of the screens the current screen was opened from (not from the menu), outermost first */
  origin: BreadcrumbLevel[] = [];
  expandRequest: ExpandRequest | null = null;
  /** Menus that can open at a group (the side menu), mounted now */
  expanders = 0;
  /** Current location of the router (pathname, search and hash) */
  location: string | null = null;
  listeners = new Set<Listener>();

  filterOptions(options: MenuOption[] = []): MenuOption[] {
    const isValid = (option: MenuOption) => option.visible && !option.restricted;
    return options.length === 0 ? [] : options.map((option) => ({
      ...option,
      allowed: isValid(option),
      options: this.filterOptions(option.options)
    }));
  }

  getOptions(): MenuOption[] {
    return this.options;
  }

  setOptions(options: MenuOption[] = []): void {
    this.options = this.filterOptions(options);
    this.emit();
  }

  getBreadcrumbs(): Breadcrumbs {
    return this.breadcrumbs;
  }

  setBreadcrumbs(optionKey: string, crumbs: BreadcrumbLevel[]): void {
    this.breadcrumbs = { items: crumbs, option: optionKey };
    this.emit();
  }

  getOrigin(): BreadcrumbLevel[] {
    return this.origin;
  }

  setOrigin(levels: BreadcrumbLevel[] = []): void {
    this.origin = levels;
    this.emit();
  }

  getExpandRequest(): ExpandRequest | null {
    return this.expandRequest;
  }

  requestExpand(name: string): void {
    this.expandRequest = { name, token: (this.expandRequest?.token ?? 0) + 1 };
    this.emit();
  }

  /** Declare a menu that opens at a group when asked (requestExpand); returns the function that detaches it */
  attachExpander(): () => void {
    this.expanders += 1;
    this.emit();
    return () => {
      this.expanders -= 1;
      this.emit();
    };
  }

  hasExpander(): boolean {
    return this.expanders > 0;
  }

  getLocation(): string | null {
    return this.location;
  }

  setLocation(location: string | null): void {
    if (location !== this.location) {
      this.location = location;
      this.emit();
    }
  }

  clear(): void {
    this.location = null;
    this.options = [];
    this.breadcrumbs = { items: [], option: null };
    this.origin = [];
    this.expandRequest = null;
    this.emit();
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  emit(): void {
    this.listeners.forEach((listener) => listener());
  }
}

export default new MenuRegistry();
