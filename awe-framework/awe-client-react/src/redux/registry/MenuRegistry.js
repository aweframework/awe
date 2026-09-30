class MenuRegistry {
  constructor() {
    this.options = [];
    this.breadcrumbs = { items: [], option: null };
    this.listeners = new Set();
  }

  filterOptions(options = []) {
    const isValid = (option) => option.visible && !option.restricted;
    return options.length === 0 ? [] : options.map((option) => ({
      ...option,
      allowed: isValid(option),
      options: this.filterOptions(option.options)
    }));
  }

  getOptions() {
    return this.options;
  }

  setOptions(options = []) {
    this.options = this.filterOptions(options);
    this.emit();
  }

  getBreadcrumbs() {
    return this.breadcrumbs;
  }

  setBreadcrumbs(optionKey, crumbs) {
    this.breadcrumbs = { items: crumbs, option: optionKey };
    this.emit();
  }

  clear() {
    this.options = [];
    this.breadcrumbs = { items: [], option: null };
    this.emit();
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  emit() {
    this.listeners.forEach((listener) => listener());
  }
}

export default new MenuRegistry();
