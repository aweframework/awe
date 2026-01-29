class SizeRegistry {
  constructor() {
    this.size = {};
    this.listeners = new Set();
  }

  getSize() {
    return this.size;
  }

  setSize(data) {
    this.size = {
      ...this.size,
      ...data,
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

export default new SizeRegistry();
