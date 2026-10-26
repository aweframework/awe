import type {MenuOption} from "../types";

type Listener = () => void;

/** Breadcrumbs of the menu: the trail of the current option and its key */
interface Breadcrumbs {
  items: unknown[];
  option: string | null;
}

class MenuRegistry {
  options: MenuOption[] = [];
  breadcrumbs: Breadcrumbs = { items: [], option: null };
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

  setBreadcrumbs(optionKey: string, crumbs: unknown[]): void {
    this.breadcrumbs = { items: crumbs, option: optionKey };
    this.emit();
  }

  clear(): void {
    this.options = [];
    this.breadcrumbs = { items: [], option: null };
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
