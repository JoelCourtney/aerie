/* global FalconBand, TimeScrollBar */

/**
 * Falcon Time Scroll Bar.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconTimeScrollBar extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeScrollBar
   */
  static get is() {
    return 'falcon-time-scroll-bar';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeScrollBar
   */
  static get properties() {
    return {
      /**
       * The current time range we are viewing in the timeline.
       */
      viewTimeRange: {
        notify: true,
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
        },
      },
    };
  }

  /**
   * Creates an instance of FalconTimeScrollBar.
   *
   * @memberof FalconTimeScrollBar
   */
  constructor() {
    super();

    // Member Vars.
    this.timeScrollBar = new TimeScrollBar({
      font: 'normal 9px Verdana',
      height: 15,
      label: '',
      // onFormatTimeTick: () => {},
      onUpdateView: this._onUpdateView.bind(this),
      timeAxis: this._timeAxis,
      updateOnDrag: false,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * CTL Callback. Called after a Time Scroll Bar drag.
   *
   * @param {Number} start
   * @param {Number} end
   *
   * @memberof FalconTimeScrollBar
   */
  _onUpdateView(start, end) {
    if (start !== 0 && end !== 0 && start < end) {
      this.set('viewTimeRange', { end, start });
    }
  }
}

customElements.define(FalconTimeScrollBar.is, FalconTimeScrollBar);
