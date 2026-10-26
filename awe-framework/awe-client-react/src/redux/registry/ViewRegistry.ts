import type {ViewData} from "../types";

type Listener = () => void;

class ViewRegistry {
  views: Record<string, ViewData> = {
    base: { loading: true },
    report: { loading: true },
  };
  currentView = "base";
  listeners = new Set<Listener>();

  get(view: string): ViewData | undefined {
    return this.views[view];
  }

  getCurrentView(): string {
    return this.currentView;
  }

  setCurrentView(view: string): void {
    this.currentView = view;
    this.emit();
  }

  setView(view: string, data: ViewData): void {
    this.views = {
      ...this.views,
      [view]: data,
    };
    this.emit();
  }

  updateView(view: string, data: ViewData): void {
    this.views = {
      ...this.views,
      [view]: {
        ...(this.views[view] || {}),
        ...data,
      },
    };
    this.emit();
  }

  clearView(view: string): void {
    this.views = {
      ...this.views,
      [view]: { loading: true },
    };
    this.emit();
  }

  clearAll(): void {
    this.views = {
      base: { loading: true },
      report: { loading: true },
    };
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

export default new ViewRegistry();
