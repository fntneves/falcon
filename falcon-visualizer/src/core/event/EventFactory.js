import Event from './Event';
import SocketEvent from './SocketEvent';

export default class EventFactory {
  static build(fields) {
    switch (fields.type) {
      case 'ACCEPT':
      case 'CONNECT':
      case 'SND':
      case 'RCV':
      case 'SHUTDOWN':
      case 'CLOSE':
        return new SocketEvent(fields);

      default:
        return new Event(fields);
    }
  }
}
