/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { omit } from 'lodash';

import {
  createFeatureSelector,
} from '@ngrx/store';

import {
  AddBand,
  AddPointsToSubBand,
  AddSubBand,
  ChangeDefaultFillColor,
  ChangeDefaultResourceColor,
  ChangeLabelFontSize,
  ChangeLabelFontStyle,
  ChangeLabelWidth,
  RemoveBandsOrPointsForSource,
  RemoveSubBand,
  SelectBand,
  SelectPoint,
  SortBands,
  TimelineAction,
  TimelineActionTypes,
  UpdateBand,
  UpdateSubBand,
} from './../actions/timeline';

import {
  bandById,
  getMaxTimeRange,
  getPoint,
  updateSelectedBandIds,
  updateSelectedPoint,
  updateSortOrder,
  updateTimeRanges,
} from './../shared/util';

import {
  RavenCompositeBand,
  RavenDefaultSettings,
  RavenPoint,
  RavenSubBand,
  RavenTimeRange,
} from './../shared/models';

// Timeline State Interface.
export interface TimelineState {
  bands: RavenCompositeBand[];
  colorPalette: string[];
  currentTimeCursor: boolean;
  dateFormat: string;
  defaultSettings: RavenDefaultSettings;
  maxTimeRange: RavenTimeRange;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  tooltip: boolean;
  viewTimeRange: RavenTimeRange;
}

// Timeline Initial State.
export const initialState: TimelineState = {
  bands: [],
  colorPalette: [
    '#000000', // black
    '#ff0000', // red
    '#00ff00', // green
    '#0000ff', // blue
    '#ffa500', // orange
    '#ffff00', // yellow
  ],
  currentTimeCursor: false,
  dateFormat: 'Day-Month-Year',
  defaultSettings: {
    fillColor: '#000000',
    labelFontSize: 9,
    labelFontStyle: 'Georgia',
    labelWidth: 100,
    resourceColor: '#000000',
  },
  maxTimeRange: { end: 0, start: 0 },
  selectedBandId: '',
  selectedPoint: null,
  selectedSubBandId: '',
  tooltip: true,
  viewTimeRange: { end: 0, start: 0 },
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: TimelineState = initialState, action: TimelineAction): TimelineState {
  switch (action.type) {
    case TimelineActionTypes.AddBand:
      return addBand(state, action);
    case TimelineActionTypes.AddPointsToSubBand:
      return addPointsToSubBand(state, action);
    case TimelineActionTypes.AddSubBand:
      return addSubBand(state, action);
    case TimelineActionTypes.RemoveBandsOrPointsForSource:
      return removeBandsOrPointsForSource(state, action);
    case TimelineActionTypes.RemoveSubBand:
      return removeSubBand(state, action);
    case TimelineActionTypes.SelectBand:
      return selectBand(state, action);
    case TimelineActionTypes.SelectPoint:
      return selectPoint(state, action);
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.UpdateBand:
      return updateBand(state, action);
    case TimelineActionTypes.UpdateSubBand:
      return updateSubBand(state, action);
    case TimelineActionTypes.UpdateTimeline:
      return { ...state, ...action.update };
    case TimelineActionTypes.ChangeCurrentTimeCursor:
      return { ...state, currentTimeCursor: action.currentTimeCursor };
    case TimelineActionTypes.ChangeDateFormat:
      return { ...state, dateFormat: action.dateFormat };
    case TimelineActionTypes.ChangeTooltip:
      return { ...state, tooltip: action.tooltip };
    case TimelineActionTypes.ChangeLabelFontSize:
      return changeLabelFontSize(state, action);
    case TimelineActionTypes.ChangeLabelWidth:
      return changeLabelWidth(state, action);
    case TimelineActionTypes.ChangeLabelFontStyle:
      return changeLabelFontStyle(state, action);
    case TimelineActionTypes.ChangeDefaultFillColor:
      return changeDefaultFillColor (state, action);
    case TimelineActionTypes.ChangeDefaultResourceColor:
      return changeDefaultResourceColor (state, action);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'AddBand' action.
 */
export function addBand(state: TimelineState, action: AddBand): TimelineState {
  const bands = state.bands.concat({
    ...action.band,
    containerId: '0',
    sortOrder: state.bands.filter(b => b.containerId === '0').length,
    subBands: action.band.subBands.map(subBand => {
      if (action.sourceId) {
        return {
          ...subBand,
          parentUniqueId: action.band.id,
          sourceIds: {
            ...subBand.sourceIds,
            [action.sourceId]: action.sourceId,
          },
        };
      } else {
        return {
          ...subBand,
          parentUniqueId: action.band.id,
        };
      }
    }),
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddPointsToSubBand' action.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function addPointsToSubBand(state: TimelineState, action: AddPointsToSubBand): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            const points = (subBand as any).points.concat(action.points);
            const maxTimeRange = getMaxTimeRange(points);

            return {
              ...subBand,
              maxTimeRange,
              points,
              sourceIds: {
                ...subBand.sourceIds,
                [action.sourceId]: action.sourceId,
              },
            };
          }

          return subBand;
        }),
      };
    }

    return band;
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddSubBand' action.
 */
export function addSubBand(state: TimelineState, action: AddSubBand): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.concat({
          ...action.subBand,
          parentUniqueId: band.id,
          sourceIds: {
            ...action.subBand.sourceIds,
            [action.sourceId]: action.sourceId,
          },
        }),
      };
    }
    return band;
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/** Reduction Helper. Called when reducing the 'ChangeDefaultFillColor' action.
 *  Adds color to colorPalette if not exists already
 */
export function changeDefaultFillColor (state: TimelineState, action: ChangeDefaultFillColor): TimelineState {
  const colors = state.colorPalette.slice(0);
  if (!colors.includes(action.defaultFillColor)) {
    colors.push(action.defaultFillColor);
  }
  return { ...state, defaultSettings: { ...state.defaultSettings, fillColor: action.defaultFillColor }, colorPalette: colors };
}

/** Reduction Helper. Called when reducing the 'ChangeDefaultResourceColor' action.
 *  Adds color to colorPalette if not exists already
 */
export function changeDefaultResourceColor(state: TimelineState, action: ChangeDefaultResourceColor): TimelineState {
  const colors = state.colorPalette.slice(0);
      if (!colors.includes(action.defaultResourceColor)) {
        colors.push(action.defaultResourceColor);
      }
      return { ...state, defaultSettings: { ...state.defaultSettings, resourceColor: action.defaultResourceColor }, colorPalette: colors };
}

export function changeLabelFontSize (state: TimelineState, action: ChangeLabelFontSize): TimelineState {
  return { ...state, defaultSettings: { ...state.defaultSettings, labelFontSize: action.labelFontSize}};
}

export function changeLabelFontStyle (state: TimelineState, action: ChangeLabelFontStyle): TimelineState {
  return { ...state, defaultSettings: { ...state.defaultSettings, labelFontStyle: action.labelFontStyle}};
}

export function changeLabelWidth (state: TimelineState, action: ChangeLabelWidth): TimelineState {
  return { ...state, defaultSettings: { ...state.defaultSettings, labelWidth: action.labelWidth}};
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBandsOrPointsForSource' action.
 * Removes all bands or points that reference the given source.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function removeBandsOrPointsForSource(state: TimelineState, action: RemoveBandsOrPointsForSource): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.reduce((subBands: RavenSubBand[], subBand: RavenSubBand) => {
        const subBandHasSource = subBand.sourceIds[action.sourceId];
        const sourceIdsCount = Object.keys(subBand.sourceIds).length;

        if (!subBandHasSource) {
          subBands.push(subBand);
        } else if (subBandHasSource && sourceIdsCount > 1) {
          subBands.push({
            ...subBand,
            points: (subBand as any).points.filter((point: any) => point.sourceId !== action.sourceId),
            sourceIds: omit(subBand.sourceIds, action.sourceId),
          });
        }

        return subBands;
      }, []),
    }))
    .filter(
    band => band.subBands.length !== 0,
  );

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(bands, state.selectedBandId, state.selectedSubBandId),
    ...updateSelectedPoint(state.selectedPoint, action.sourceId, null),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveSubBand' action.
 */
export function removeSubBand(state: TimelineState, action: RemoveSubBand): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(subBand => subBand.id !== action.subBandId),
    }))
    .filter(
    band => band.subBands.length !== 0,
  );

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(bands, state.selectedBandId, state.selectedSubBandId),
    ...updateSelectedPoint(state.selectedPoint, null, action.subBandId),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectBand' action.
 */
export function selectBand(state: TimelineState, action: SelectBand): TimelineState {
  const selectedBandId = action.bandId === state.selectedBandId ? '' : action.bandId;
  const band = bandById(state.bands, selectedBandId);

  return {
    ...state,
    selectedBandId,
    selectedSubBandId: band && band.subBands.length && selectedBandId !== '' ? band.subBands[0].id : '',
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectPoint' action.
 * Make sure if a point is already selected that we de-select it if it's clicked again.
 */
export function selectPoint(state: TimelineState, action: SelectPoint): TimelineState {
  const alreadySelected = state.selectedPoint && state.selectedPoint.uniqueId === action.pointId;

  return {
    ...state,
    selectedPoint: alreadySelected ? null : getPoint(state.bands, action.bandId, action.subBandId, action.pointId),
  };
}

/**
 * Reduction Helper. Called when reducing the 'NewSortOrder' action.
 */
export function sortBands(state: TimelineState, action: SortBands): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.sort[band.id]) {
        return {
          ...band,
          containerId: action.sort[band.id].containerId,
          sortOrder: action.sort[band.id].sortOrder,
        };
      }

      return band;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'UpdateBand' action.
 */
export function updateBand(state: TimelineState, action: UpdateBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          ...action.update,
        };
      }

      return band;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'UpdateSubBand' action.
 */
export function updateSubBand(state: TimelineState, action: UpdateSubBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              return {
                ...subBand,
                ...action.update,
              };
            }
            return subBand;
          }),
        };
      }

      return band;
    }),
  };
}

/**
 * Timeline state selector helper.
 */
export const getTimelineState = createFeatureSelector<TimelineState>('timeline');

/**
 * Create selector helper for selecting state slice.
 *
 * Every reducer module exports selector functions, however child reducers
 * have no knowledge of the overall state tree. To make them usable, we
 * need to make new selectors that wrap them.
 *
 * The createSelector function creates very efficient selectors that are memoized and
 * only recompute when arguments change. The created selectors can also be composed
 * together to select different pieces of state.
 */
// TODO: Add more specific selectors if needed.
