export default class Event {
  constructor(fields) {
    this.id = fields.id;
    this.pid = (fields.thread.split('@')[1]).split('.')[0];
    this.thread = fields.thread.split('@')[0];
    this.clock = fields.order;
    this.dependency = fields.dependency || null;
    this.type = fields.type;
    this.data = fields;
  }

  hasDependency() {
    return this.dependency !== null;
  }

  getThreadIdentifier() {
    return `${this.pid}||${this.thread}`;
  }
}
