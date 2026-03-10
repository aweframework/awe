import {useCallback, useRef, useState} from 'react';
import {useDispatch} from 'react-redux';
import {updateModelWithDependencies} from "../redux/thunks/components";
import {initialSuggestAction, suggestAction} from "../redux/thunks/suggest";

const useSuggest = (autocompleteRef, setSuggestions, value, setValue, props) => {
  const dispatch = useDispatch();
  const abortControllerRef = useRef(new AbortController());
  const [suggesting, setSuggesting] = useState(false);
  const { address, serverAction, targetAction, checkTarget, strict = true } = props;

  const onChange = useCallback( (e) => {
    // Update suggest
    if (value?.value !== (e.value?.value || e.value)) {
      setValue(e.value);
    }

    // Store suggest data
    if (e.originalEvent?.type === "click") {
      dispatch(updateModelWithDependencies(address, {
        values: [e.value].flat().map(item => ({ ...item, selected: true }))
      }));
    }
  }, [value]);

  const onClear = useCallback(() => {
      dispatch(updateModelWithDependencies(address, { values: [] }));
  }, [value]);

  const onKeyPress = (e) => {
    if (e.code === "Space" && e.target.value === "") {
      autocompleteRef.current?.onDropdownClick?.();
    }
  };

  const onSuggest = ({originalEvent, query}) => {
    suggest(originalEvent, query);
  };

  /**
   * Suggest a value
   * @param {object} event Event
   * @param {string} text Suggestion value
   * @memberOf Components
   */
  const suggest = useCallback(async (event, text) => {
    try {
      // Cancel previous fetch
      if (suggesting) {
        abortControllerRef.current.abort();
        abortControllerRef.current = new AbortController();
      }

      // Fetch server action
      const {signal} = abortControllerRef.current;
      setSuggesting(true);
      setSuggestions(await dispatch(suggestAction(event, text, {address, serverAction, targetAction, strict, signal})));
      setSuggesting(false);
    } catch (error) {
      // Ignore AbortError - it happens when a previous request is cancelled
      if (error?.name !== 'AbortError') {
        console.error('Error in suggest:', error);
      }
      setSuggesting(false);
    }
  }, [dispatch, suggesting, address, serverAction, targetAction, strict]);

  /**
   * Initial suggest
   * @param {object} component Component
   * @param {string} query   Suggestion text
   * @memberOf Components
   */
  const initialSuggest = useCallback(async (query)  => {
    await dispatch(initialSuggestAction(query, {address, serverAction, targetAction, checkTarget}));

    // Hide autocomplete
    autocompleteRef?.current?.hide?.();
  }, [dispatch, address, serverAction, targetAction, checkTarget, autocompleteRef]);

  return  { onChange, onClear, onKeyPress, onSuggest, suggest, initialSuggest };
};

export default useSuggest;
