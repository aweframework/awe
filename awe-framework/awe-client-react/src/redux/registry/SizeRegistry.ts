import type {SizeState} from "../types";

type Listener = () => void;

class SizeRegistry {
  size: SizeState = {};
  listeners = new Set<Listener>();

  getSize(): SizeState {
    return this.size;
  }

  setSize(data: SizeState): void {
    this.size = {
      ...this.size,
      ...data,
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

export default new SizeRegistry();
