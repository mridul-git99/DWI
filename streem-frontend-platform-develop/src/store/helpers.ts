import { TypedUseSelectorHook, useSelector } from 'react-redux';
import { RootState } from './types';

type GenFun = (...args: any[]) => any;

/**
 * This function takes the action dispatched from the component and send to the redux store to perform state update
 * @param type Name of hte action that has to be performed by redux
 * @param payload Payload data that has to be modified in the store
 */

// function overload for actions without any payload
export function actionSpreader<T extends string>(type: T): { type: T };
// function overload for actions with payload
export function actionSpreader<T extends string, P extends any>(
  type: T,
  payload: P,
): { type: T; payload: P };
// actual actionSpreader function with payload as optional
export function actionSpreader<T extends string, P extends any>(type: T, payload?: P) {
  return { type, payload };
}

/**
 * Typed selector for redux store as per the application state defined in the root reducer
 */
export const useTypedSelector: TypedUseSelectorHook<RootState> = useSelector;

/**
 * A helper function to generate actions & actionsEnum for a given set of action types.
 *
 * This function takes a map of action types and an optional prefix to generate actions and actions enum.
 *
 * @param actionTypes the map of action types
 *
 * Note: The value of the action type can be either be one of the following:
 * - a function that returns a value that represents the payload type
 * - a string or number that represents the payload type
 * - undefined or null
 *
 * @param prefix an optional prefix to prepend to each action enum
 *
 * @returns an object containing the generated actions and actionsEnum
 *
 * @example
 *
 * const actions = {
 *  incrementByOne: undefined, \\ action with no payload, same as null or () => undefined or () => null
 *  incrementBy: 1, \\ action with payload type `number`, same as () => 1
 *  decrementBy: {} as number, \\ will behave same as incrementBy
 * }
 * const { actions, actionsEnum } = generateActions(actions, '@@counter/');
 *
 * console.log(actions.incrementByOne()); \\ { type: '@@counter/incrementByOne' }
 * `Note`: argument type of incrementBy/decrementBy is inferred as number
 * console.log(actions.incrementBy(2)); \\ { type: '@@counter/incrementBy', payload: 2 }
 * console.log(actions.decrementBy(4)); \\ { type: '@@counter/decrementBy', payload: 4 }
 *
 */
export function generateActions<
  T extends Record<string, GenFun | undefined | null | number | string | object>,
  U extends string,
>(actionTypes: T, prefix: U) {
  type Actions = {
    [K in keyof T]: T[K] extends undefined | null
      ? () => { type: `${U}${string & K}` }
      : T[K] extends () => infer R
      ? R extends undefined | null | void
        ? () => { type: `${U}${string & K}` }
        : (payload: R) => { type: `${U}${string & K}`; payload: R }
      : T[K] extends string | number | object
      ? (payload: T[K]) => { type: `${U}${string & K}`; payload: T[K] }
      : never;
  };

  const actions = {} as Actions;
  const actionsEnum = {} as { [K in keyof T]: `${U}${string & K}` };

  (Object.keys(actionTypes) as Array<keyof T>).forEach((key) => {
    let actionValue = actionTypes[key];
    if (typeof actionValue === 'function') {
      actionValue = actionValue();
    }
    if (!actionValue) {
      actions[key] = (() => actionSpreader(actionsEnum[key])) as unknown as Actions[keyof Actions];
    } else {
      actions[key] = ((payload: typeof actionValue) =>
        actionSpreader(actionsEnum[key], payload)) as unknown as Actions[keyof Actions];
    }
    actionsEnum[key] = `${prefix}${key as string}`;
  });

  return { actions, actionsEnum };
}
