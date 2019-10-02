import SVG from 'svg.js';

const topPadding = 20;
const threadPadding = 150;
const clockPadding = 50;
const eventRadius = 10;
const drawThreadTimeline = Symbol('drawThreadTimeline');
const drawClockEvents = Symbol('drawClockEvents');
const calculateThreadPosition = Symbol('calculateThreadPosition');
const calculateNextClockPosition = Symbol('calculateNextClockPosition');
const generateEventId = Symbol('generateEventId');
const generateTimelineId = Symbol('generateTimelineId');
const backInTime = Symbol('backInTime');
const updateRunningTimelines = Symbol('updateRunningTimelines');
const generateThreadColor = Symbol('generateThreadColor');
const colorPatterns = ['#D32F2F', '#43A047', '#7B1FA2', '#1E88E5', '#7E57C2', '#C0CA33', '#8D6E63'].reverse();

export default class TraceDrawer {
  constructor(drawing, universe) {
    this.elementsPerClock = {};
    this.drewThreads = [];
    this.closedThreads = [];
    this.drewClocks = 0;
    this.drawing = drawing;
    this.universe = universe;
    this.processColors = {};
  }

  /**
   * Magic happens here.
   */
  nextClock() {
    this[drawClockEvents](this.drewClocks);
    window.scrollTo(document.documentElement.scrollLeft, document.body.scrollHeight);
  }

  /**
   * Magic happens here.
   */
  previousClock() {
    this[backInTime]();
  }

  /**
   * Clear all events.
   */
  reset() {
    this.drewThreads = [];
    this.drewClocks = 0;
    this.drawing.clear();
  }

  [drawThreadTimeline](thread, clockPosition = 0, color) {
    if (this.drewThreads.includes(thread)) {
      return this[calculateThreadPosition](thread);
    }

    this.drewThreads.push(thread);
    const threadPosition = this[calculateThreadPosition](thread);
    this.drawing.width(this.drewThreads.length * (threadPadding * 4));
    const threadLineY1 = clockPosition > 0 ? clockPosition : clockPosition + topPadding;
    const threadLineY2 = threadLineY1 + (topPadding / 2);
    this.drawing.line(threadPosition, threadLineY1, threadPosition, threadLineY2)
        .stroke({ width: 1, color: color || '#000000' })
        .attr({ 'stroke-dasharray': '5, 5' })
        .id(TraceDrawer[generateTimelineId](thread));

    const threadLabel = this.drawing.plain(thread.split('||')[1]).font({ fill: color || '#000000' });
    const threadLabelBox = threadLabel.bbox();
    threadLabel.move(threadPosition - threadLabelBox.cx, 0);

    return threadPosition;
  }

  [drawClockEvents](clock) {
    const events = this.universe.at(clock);
    const clockPosition = this[calculateNextClockPosition]();

    // Draw clock label.
    this.drawing.height(clockPosition + clockPadding);
    this.drawing.plain(`${clock}`).move(0, clockPosition - eventRadius);

    events.forEach((event) => {
      const eventGroup = this.drawing.group();
      // Draw a new timeline if the thread does not exist and get its position.
      const threadColor = this[generateThreadColor](event.pid);
      const threadPosition = this[drawThreadTimeline](event.getThreadIdentifier(), event.type === 'START' ? clockPosition : 0, threadColor);

      // Draw event.
      const eventShape = eventGroup.circle(eventRadius * 2)
        .fill(threadColor || '#000000')
        .move(threadPosition - eventRadius, clockPosition - eventRadius)
        .id(TraceDrawer[generateEventId](event.id));
      const eventShapeBox = eventShape.bbox();
      const eventLabel = eventGroup.plain(`${event.type}`);
      eventLabel.move(eventShapeBox.x + (3 * eventRadius), eventShapeBox.y - ((eventRadius - eventLabel.font('size')) / 2));
      // const eventLabelBox = eventLabel.bbox();
      // if (['SND', 'RCV'].includes(event.type)) {
      //   const eventLabelSyscallExit = eventGroup.plain(`${event.data.data.syscall_exit}`);
      //   eventLabelSyscallExit.move(eventLabelBox.x2 + 3
      // eventShapeBox.y + nextSubLabelOffset + ((eventRadius +
      // eventLabelSyscallExit.font('size')) / 2));
      //   nextSubLabelOffset += eventLabelSyscallExit.bbox().height;

        // Draw src and dst information
        // const labelChannelId = event.type === 'SND'
        //   ? `${event.data.src}:${event.data.src_port}`
        //   : `${event.data.dst}:${event.data.dst_port}`;
        // const eventLabelChannelId = eventGroup.plain(labelChannelId);
        // eventLabelChannelId.move(eventLabelBox.x2 + 3, eventShapeBox.y +
        // nextSubLabelOffset + ((eventRadius + eventLabelChannelId.font('size')) / 2));
        // nextSubLabelOffset += eventLabelChannelId.bbox().height;
      // }

      if (event.type === 'LOG') {
        const titleMessage = eventGroup.element('title').words(event.data.data.message);
        titleMessage.addTo(eventGroup);
      }

      // Draw connector.
      let dependencies = [];
      if (event.hasDependency()) {
        if (['SND', 'RCV'].includes(event.type)) {
          dependencies = event.dependencies;
        } else {
          dependencies.push(event.dependency);
        }
      }

      dependencies.forEach((dependency) => {
        const dependencyShape = SVG.get(TraceDrawer[generateEventId](dependency));
        const dependencyShapeBox = dependencyShape.bbox();
        const connectorShape = this.drawing.line(
          dependencyShapeBox.cx, dependencyShapeBox.cy,
          dependencyShapeBox.cx, dependencyShapeBox.cy,
        ).stroke({ width: 1 });
        connectorShape.attr({ x2: eventShapeBox.cx, y2: eventShapeBox.cy });
        connectorShape.back();
      });

      // Update Timeline.
      const timelineShape = SVG.get(TraceDrawer[generateTimelineId](event.getThreadIdentifier()));
      timelineShape.attr({ x2: eventShapeBox.cx, y2: eventShapeBox.cy });

      if (event.type === 'END') {
        this.closedThreads.push(event.getThreadIdentifier());
      }

      this[updateRunningTimelines](eventShapeBox.cy);
    });

    this.drewClocks += 1;
  }

  [backInTime]() {
    // TODO: Remove elements in a single clock.
    console.log(this);
  }

  [updateRunningTimelines](y) {
    this.drewThreads
      .filter(thread => !this.closedThreads.includes(thread))
      .forEach((thread) => {
        const timelineShape = SVG.get(TraceDrawer[generateTimelineId](thread));
        timelineShape.attr({ y2: y });
      });
  }

  static [calculateThreadPosition](index) {
    return (index * threadPadding) + (threadPadding / 2);
  }

  [calculateThreadPosition](thread) {
    const index = this.universe.getThreadOrderedIndex(thread);
    return index >= 0 ? TraceDrawer[calculateThreadPosition](index) : null;
  }

  [calculateNextClockPosition]() {
    return topPadding + (this.drewClocks * clockPadding) + (clockPadding / 2);
  }

  static [generateEventId](eventId) {
    return `event${eventId}`;
  }

  static [generateTimelineId](eventId) {
    return `timeline${eventId}`;
  }

  /* eslint no-bitwise: 0 */
  /* eslint no-restricted-syntax: 1 */
  [generateThreadColor](threadId) {
    if (this.processColors[threadId] === undefined) {
      this.processColors[threadId] = colorPatterns.pop();
    }

    return this.processColors[threadId];

    // let hash = 0;
    // let color = '#';
    // const id = `${threadId}`;

    // for (const char of id) {
    //   hash = char.charCodeAt(0) + ((hash << 5) - hash);
    // }

    // for (let i = 0; i < 3; i += 1) {
    //   const value = (hash >> (i * 8)) & 0xFF;
    //   color += (`00${value.toString(16)}`).substr(-2);
    // }
    // console.log(color);
    // return color;
  }
}
