class ViewRegistry {
  constructor() {
    this.views = {
      base: { loading: true },
      report: { loading: true },
    };
    this.currentView = "base";
    this.listeners = new Set();
  }

  get(view) {
    return this.views[view];
  }

  getCurrentView() {
    return this.currentView;
  }

  setCurrentView(view) {
    this.currentView = view;
    this.emit();
  }

  setView(view, data) {
    this.views = {
      ...this.views,
      [view]: data,
    };
    this.emit();
  }

  updateView(view, data) {
    this.views = {
      ...this.views,
      [view]: {
        ...(this.views[view] || {}),
        ...data,
      },
    };
    this.emit();
  }

  clearView(view) {
    this.views = {
      ...this.views,
      [view]: { loading: true },
    };
    this.emit();
  }

  clearAll() {
    this.views = {
      base: { loading: true },
      report: { loading: true },
    };
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

export default new ViewRegistry();
