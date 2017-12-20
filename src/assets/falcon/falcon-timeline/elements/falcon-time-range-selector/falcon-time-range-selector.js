/* global FalconPolymerUtils, TimeUnit */

/**
 * Falcon Time Range Selector.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 */
class FalconTimeRangeSelector extends Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeRangeSelector
   */
  static get is() {
    return 'falcon-time-range-selector';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeRangeSelector
   */
  static get properties() {
    return {
      /**
       * The largest time range calculated from every band in the timeline.
       */
      maxTimeRange: {
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
        },
      },

      /**
       * What zoom we are at.
       */
      selectedZoom: {
        type: String,
        value: 'All',
      },

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
   * Get the observers of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeRangeSelector
   */
  static get observers() {
    return [
      '_selectedZoomChanged(selectedZoom)',
      '_viewTimeRangeChanged(viewTimeRange)',
    ];
  }

  /**
   * Observer. Called when the selectedZoom property changes.
   *
   * @memberof FalconTimeRangeSelector
   */
  _selectedZoomChanged() {
    if (this.selectedZoom === '') {
      this._deselectAllButtons();
    }
  }

  /**
   * Observer. Called when the viewTimeRange property changes.
   *
   * @memberof FalconTimeRangeSelector
   */
  _viewTimeRangeChanged() {
    switch (this.viewTimeRange.end) {
      case this.maxTimeRange.end:
        this.selectedZoom = 'All';
        break;
      case this.maxTimeRange.start + TimeUnit.YEAR:
        this.selectedZoom = '1y';
        break;
      case this.maxTimeRange.start + TimeUnit.THREE_MONTH:
        this.selectedZoom = '3m';
        break;
      case this.maxTimeRange.start + TimeUnit.FOUR_WEEK:
        this.selectedZoom = '4w';
        break;
      case this.maxTimeRange.start + TimeUnit.WEEK:
        this.selectedZoom = '1w';
        break;
      case this.maxTimeRange.start + TimeUnit.DAY:
        this.selectedZoom = '1d';
        break;
      case this.maxTimeRange.start + TimeUnit.HOUR:
        this.selectedZoom = '1h';
        break;
      case this.maxTimeRange.start + TimeUnit.MINUTE:
        this.selectedZoom = '1m';
        break;
      case this.maxTimeRange.start + TimeUnit.SECOND:
        this.selectedZoom = '1s';
        break;
      default:
        this.selectedZoom = '';
        break;
    }
  }

  /**
   * DOM Event Handler. Called when user clicks a zoom button.
   *
   * @param {any} e
   * @memberof FalconTimeRangeSelector
   */
  _onClickZoom(e) {
    this._deselectAllButtons();

    const el = this._getElement(e).parentElement;

    if (el) {
      const rectEl = el.children[0];
      const textEl = el.children[1];
      const textValue = textEl.childNodes[0].nodeValue;

      this.selectedZoom = textValue;
      rectEl.setAttribute('fill', '#e6ebf5');
      textEl.setAttribute('style', 'font-weight:bold;color:#000000;fill:#000000;');

      this._updateViewTimeRange();
    }
  }

  /**
   * Helper to deselect all zoom buttons.
   *
   * @memberof FalconTimeRangeSelector
   */
  _deselectAllButtons() {
    const buttons = Polymer.dom(this.root).querySelector('.falcon-time-range-selector-buttons');

    buttons.children.forEach((el) => {
      if (el.nodeName === 'g') {
        const rectEl = el.children[0];
        const textEl = el.children[1];

        rectEl.setAttribute('fill', '#f7f7f7');
        textEl.setAttribute('style', 'font-weight:normal;color:#333333;fill:#333333;');
      }
    });
  }

  /**
   * Helper to update the viewTimeRange with the selectedZoom range.
   *
   * @memberof FalconTimeRangeSelector
   */
  _updateViewTimeRange() {
    switch (this.selectedZoom) {
      case 'All':
        this.set('viewTimeRange', { end: this.maxTimeRange.end, start: this.maxTimeRange.start });
        break;
      case '1y':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.YEAR, start: this.maxTimeRange.start });
        break;
      case '3m':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.THREE_MONTH, start: this.maxTimeRange.start });
        break;
      case '4w':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.FOUR_WEEK, start: this.maxTimeRange.start });
        break;
      case '1w':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.WEEK, start: this.maxTimeRange.start });
        break;
      case '1d':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.DAY, start: this.maxTimeRange.start });
        break;
      case '1h':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.HOUR, start: this.maxTimeRange.start });
        break;
      case '1m':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.MINUTE, start: this.maxTimeRange.start });
        break;
      case '1s':
        this.set('viewTimeRange', { end: this.maxTimeRange.start + TimeUnit.SECOND, start: this.maxTimeRange.start });
        break;
      default:
        break;
    }
  }
}

customElements.define(FalconTimeRangeSelector.is, FalconTimeRangeSelector);
