var __create = Object.create;
var __getProtoOf = Object.getPrototypeOf;
var __defProp = Object.defineProperty;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
function __accessProp(key) {
  return this[key];
}
var __toESMCache_node;
var __toESMCache_esm;
var __toESM = (mod, isNodeMode, target) => {
  var canCache = mod != null && typeof mod === "object";
  if (canCache) {
    var cache = isNodeMode ? __toESMCache_node ??= new WeakMap : __toESMCache_esm ??= new WeakMap;
    var cached = cache.get(mod);
    if (cached)
      return cached;
  }
  target = mod != null ? __create(__getProtoOf(mod)) : {};
  const to = isNodeMode || !mod || !mod.__esModule || !__hasOwnProp.call(mod, "default") ? __defProp(target, "default", { value: mod, enumerable: true }) : target;
  if (mod && typeof mod === "object" || typeof mod === "function") {
    for (let key of __getOwnPropNames(mod))
      if (!__hasOwnProp.call(to, key))
        __defProp(to, key, {
          get: __accessProp.bind(mod, key),
          enumerable: true
        });
  }
  if (canCache)
    cache.set(mod, to);
  return to;
};
var __commonJS = (cb, mod) => () => (mod || cb((mod = { exports: {} }).exports, mod), mod.exports);
var __returnValue = (v) => v;
function __exportSetter(name, newValue) {
  this[name] = __returnValue.bind(null, newValue);
}
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {
      get: all[name],
      enumerable: true,
      configurable: true,
      set: __exportSetter.bind(all, name)
    });
};

// node_modules/react/cjs/react.development.js
var require_react_development = __commonJS(function(exports, module) {
  (function() {
    function defineDeprecationWarning(methodName, info) {
      Object.defineProperty(Component.prototype, methodName, {
        get: function() {
          console.warn("%s(...) is deprecated in plain JavaScript React classes. %s", info[0], info[1]);
        }
      });
    }
    function getIteratorFn(maybeIterable) {
      if (maybeIterable === null || typeof maybeIterable !== "object")
        return null;
      maybeIterable = MAYBE_ITERATOR_SYMBOL && maybeIterable[MAYBE_ITERATOR_SYMBOL] || maybeIterable["@@iterator"];
      return typeof maybeIterable === "function" ? maybeIterable : null;
    }
    function warnNoop(publicInstance, callerName) {
      publicInstance = (publicInstance = publicInstance.constructor) && (publicInstance.displayName || publicInstance.name) || "ReactClass";
      var warningKey = publicInstance + "." + callerName;
      didWarnStateUpdateForUnmountedComponent[warningKey] || (console.error("Can't call %s on a component that is not yet mounted. This is a no-op, but it might indicate a bug in your application. Instead, assign to `this.state` directly or define a `state = {};` class property with the desired state in the %s component.", callerName, publicInstance), didWarnStateUpdateForUnmountedComponent[warningKey] = true);
    }
    function Component(props, context, updater) {
      this.props = props;
      this.context = context;
      this.refs = emptyObject;
      this.updater = updater || ReactNoopUpdateQueue;
    }
    function ComponentDummy() {}
    function PureComponent(props, context, updater) {
      this.props = props;
      this.context = context;
      this.refs = emptyObject;
      this.updater = updater || ReactNoopUpdateQueue;
    }
    function noop() {}
    function testStringCoercion(value) {
      return "" + value;
    }
    function checkKeyStringCoercion(value) {
      try {
        testStringCoercion(value);
        var JSCompiler_inline_result = false;
      } catch (e) {
        JSCompiler_inline_result = true;
      }
      if (JSCompiler_inline_result) {
        JSCompiler_inline_result = console;
        var JSCompiler_temp_const = JSCompiler_inline_result.error;
        var JSCompiler_inline_result$jscomp$0 = typeof Symbol === "function" && Symbol.toStringTag && value[Symbol.toStringTag] || value.constructor.name || "Object";
        JSCompiler_temp_const.call(JSCompiler_inline_result, "The provided key is an unsupported type %s. This value must be coerced to a string before using it here.", JSCompiler_inline_result$jscomp$0);
        return testStringCoercion(value);
      }
    }
    function getComponentNameFromType(type) {
      if (type == null)
        return null;
      if (typeof type === "function")
        return type.$$typeof === REACT_CLIENT_REFERENCE ? null : type.displayName || type.name || null;
      if (typeof type === "string")
        return type;
      switch (type) {
        case REACT_FRAGMENT_TYPE:
          return "Fragment";
        case REACT_PROFILER_TYPE:
          return "Profiler";
        case REACT_STRICT_MODE_TYPE:
          return "StrictMode";
        case REACT_SUSPENSE_TYPE:
          return "Suspense";
        case REACT_SUSPENSE_LIST_TYPE:
          return "SuspenseList";
        case REACT_ACTIVITY_TYPE:
          return "Activity";
      }
      if (typeof type === "object")
        switch (typeof type.tag === "number" && console.error("Received an unexpected object in getComponentNameFromType(). This is likely a bug in React. Please file an issue."), type.$$typeof) {
          case REACT_PORTAL_TYPE:
            return "Portal";
          case REACT_CONTEXT_TYPE:
            return type.displayName || "Context";
          case REACT_CONSUMER_TYPE:
            return (type._context.displayName || "Context") + ".Consumer";
          case REACT_FORWARD_REF_TYPE:
            var innerType = type.render;
            type = type.displayName;
            type || (type = innerType.displayName || innerType.name || "", type = type !== "" ? "ForwardRef(" + type + ")" : "ForwardRef");
            return type;
          case REACT_MEMO_TYPE:
            return innerType = type.displayName || null, innerType !== null ? innerType : getComponentNameFromType(type.type) || "Memo";
          case REACT_LAZY_TYPE:
            innerType = type._payload;
            type = type._init;
            try {
              return getComponentNameFromType(type(innerType));
            } catch (x) {}
        }
      return null;
    }
    function getTaskName(type) {
      if (type === REACT_FRAGMENT_TYPE)
        return "<>";
      if (typeof type === "object" && type !== null && type.$$typeof === REACT_LAZY_TYPE)
        return "<...>";
      try {
        var name = getComponentNameFromType(type);
        return name ? "<" + name + ">" : "<...>";
      } catch (x) {
        return "<...>";
      }
    }
    function getOwner() {
      var dispatcher = ReactSharedInternals.A;
      return dispatcher === null ? null : dispatcher.getOwner();
    }
    function UnknownOwner() {
      return Error("react-stack-top-frame");
    }
    function hasValidKey(config) {
      if (hasOwnProperty.call(config, "key")) {
        var getter = Object.getOwnPropertyDescriptor(config, "key").get;
        if (getter && getter.isReactWarning)
          return false;
      }
      return config.key !== undefined;
    }
    function defineKeyPropWarningGetter(props, displayName) {
      function warnAboutAccessingKey() {
        specialPropKeyWarningShown || (specialPropKeyWarningShown = true, console.error("%s: `key` is not a prop. Trying to access it will result in `undefined` being returned. If you need to access the same value within the child component, you should pass it as a different prop. (https://react.dev/link/special-props)", displayName));
      }
      warnAboutAccessingKey.isReactWarning = true;
      Object.defineProperty(props, "key", {
        get: warnAboutAccessingKey,
        configurable: true
      });
    }
    function elementRefGetterWithDeprecationWarning() {
      var componentName = getComponentNameFromType(this.type);
      didWarnAboutElementRef[componentName] || (didWarnAboutElementRef[componentName] = true, console.error("Accessing element.ref was removed in React 19. ref is now a regular prop. It will be removed from the JSX Element type in a future release."));
      componentName = this.props.ref;
      return componentName !== undefined ? componentName : null;
    }
    function ReactElement(type, key, props, owner, debugStack, debugTask) {
      var refProp = props.ref;
      type = {
        $$typeof: REACT_ELEMENT_TYPE,
        type,
        key,
        props,
        _owner: owner
      };
      (refProp !== undefined ? refProp : null) !== null ? Object.defineProperty(type, "ref", {
        enumerable: false,
        get: elementRefGetterWithDeprecationWarning
      }) : Object.defineProperty(type, "ref", { enumerable: false, value: null });
      type._store = {};
      Object.defineProperty(type._store, "validated", {
        configurable: false,
        enumerable: false,
        writable: true,
        value: 0
      });
      Object.defineProperty(type, "_debugInfo", {
        configurable: false,
        enumerable: false,
        writable: true,
        value: null
      });
      Object.defineProperty(type, "_debugStack", {
        configurable: false,
        enumerable: false,
        writable: true,
        value: debugStack
      });
      Object.defineProperty(type, "_debugTask", {
        configurable: false,
        enumerable: false,
        writable: true,
        value: debugTask
      });
      Object.freeze && (Object.freeze(type.props), Object.freeze(type));
      return type;
    }
    function cloneAndReplaceKey(oldElement, newKey) {
      newKey = ReactElement(oldElement.type, newKey, oldElement.props, oldElement._owner, oldElement._debugStack, oldElement._debugTask);
      oldElement._store && (newKey._store.validated = oldElement._store.validated);
      return newKey;
    }
    function validateChildKeys(node) {
      isValidElement(node) ? node._store && (node._store.validated = 1) : typeof node === "object" && node !== null && node.$$typeof === REACT_LAZY_TYPE && (node._payload.status === "fulfilled" ? isValidElement(node._payload.value) && node._payload.value._store && (node._payload.value._store.validated = 1) : node._store && (node._store.validated = 1));
    }
    function isValidElement(object) {
      return typeof object === "object" && object !== null && object.$$typeof === REACT_ELEMENT_TYPE;
    }
    function escape(key) {
      var escaperLookup = { "=": "=0", ":": "=2" };
      return "$" + key.replace(/[=:]/g, function(match) {
        return escaperLookup[match];
      });
    }
    function getElementKey(element, index) {
      return typeof element === "object" && element !== null && element.key != null ? (checkKeyStringCoercion(element.key), escape("" + element.key)) : index.toString(36);
    }
    function resolveThenable(thenable) {
      switch (thenable.status) {
        case "fulfilled":
          return thenable.value;
        case "rejected":
          throw thenable.reason;
        default:
          switch (typeof thenable.status === "string" ? thenable.then(noop, noop) : (thenable.status = "pending", thenable.then(function(fulfilledValue) {
            thenable.status === "pending" && (thenable.status = "fulfilled", thenable.value = fulfilledValue);
          }, function(error) {
            thenable.status === "pending" && (thenable.status = "rejected", thenable.reason = error);
          })), thenable.status) {
            case "fulfilled":
              return thenable.value;
            case "rejected":
              throw thenable.reason;
          }
      }
      throw thenable;
    }
    function mapIntoArray(children, array, escapedPrefix, nameSoFar, callback) {
      var type = typeof children;
      if (type === "undefined" || type === "boolean")
        children = null;
      var invokeCallback = false;
      if (children === null)
        invokeCallback = true;
      else
        switch (type) {
          case "bigint":
          case "string":
          case "number":
            invokeCallback = true;
            break;
          case "object":
            switch (children.$$typeof) {
              case REACT_ELEMENT_TYPE:
              case REACT_PORTAL_TYPE:
                invokeCallback = true;
                break;
              case REACT_LAZY_TYPE:
                return invokeCallback = children._init, mapIntoArray(invokeCallback(children._payload), array, escapedPrefix, nameSoFar, callback);
            }
        }
      if (invokeCallback) {
        invokeCallback = children;
        callback = callback(invokeCallback);
        var childKey = nameSoFar === "" ? "." + getElementKey(invokeCallback, 0) : nameSoFar;
        isArrayImpl(callback) ? (escapedPrefix = "", childKey != null && (escapedPrefix = childKey.replace(userProvidedKeyEscapeRegex, "$&/") + "/"), mapIntoArray(callback, array, escapedPrefix, "", function(c) {
          return c;
        })) : callback != null && (isValidElement(callback) && (callback.key != null && (invokeCallback && invokeCallback.key === callback.key || checkKeyStringCoercion(callback.key)), escapedPrefix = cloneAndReplaceKey(callback, escapedPrefix + (callback.key == null || invokeCallback && invokeCallback.key === callback.key ? "" : ("" + callback.key).replace(userProvidedKeyEscapeRegex, "$&/") + "/") + childKey), nameSoFar !== "" && invokeCallback != null && isValidElement(invokeCallback) && invokeCallback.key == null && invokeCallback._store && !invokeCallback._store.validated && (escapedPrefix._store.validated = 2), callback = escapedPrefix), array.push(callback));
        return 1;
      }
      invokeCallback = 0;
      childKey = nameSoFar === "" ? "." : nameSoFar + ":";
      if (isArrayImpl(children))
        for (var i = 0;i < children.length; i++)
          nameSoFar = children[i], type = childKey + getElementKey(nameSoFar, i), invokeCallback += mapIntoArray(nameSoFar, array, escapedPrefix, type, callback);
      else if (i = getIteratorFn(children), typeof i === "function")
        for (i === children.entries && (didWarnAboutMaps || console.warn("Using Maps as children is not supported. Use an array of keyed ReactElements instead."), didWarnAboutMaps = true), children = i.call(children), i = 0;!(nameSoFar = children.next()).done; )
          nameSoFar = nameSoFar.value, type = childKey + getElementKey(nameSoFar, i++), invokeCallback += mapIntoArray(nameSoFar, array, escapedPrefix, type, callback);
      else if (type === "object") {
        if (typeof children.then === "function")
          return mapIntoArray(resolveThenable(children), array, escapedPrefix, nameSoFar, callback);
        array = String(children);
        throw Error("Objects are not valid as a React child (found: " + (array === "[object Object]" ? "object with keys {" + Object.keys(children).join(", ") + "}" : array) + "). If you meant to render a collection of children, use an array instead.");
      }
      return invokeCallback;
    }
    function mapChildren(children, func, context) {
      if (children == null)
        return children;
      var result = [], count = 0;
      mapIntoArray(children, result, "", "", function(child) {
        return func.call(context, child, count++);
      });
      return result;
    }
    function lazyInitializer(payload) {
      if (payload._status === -1) {
        var ioInfo = payload._ioInfo;
        ioInfo != null && (ioInfo.start = ioInfo.end = performance.now());
        ioInfo = payload._result;
        var thenable = ioInfo();
        thenable.then(function(moduleObject) {
          if (payload._status === 0 || payload._status === -1) {
            payload._status = 1;
            payload._result = moduleObject;
            var _ioInfo = payload._ioInfo;
            _ioInfo != null && (_ioInfo.end = performance.now());
            thenable.status === undefined && (thenable.status = "fulfilled", thenable.value = moduleObject);
          }
        }, function(error) {
          if (payload._status === 0 || payload._status === -1) {
            payload._status = 2;
            payload._result = error;
            var _ioInfo2 = payload._ioInfo;
            _ioInfo2 != null && (_ioInfo2.end = performance.now());
            thenable.status === undefined && (thenable.status = "rejected", thenable.reason = error);
          }
        });
        ioInfo = payload._ioInfo;
        if (ioInfo != null) {
          ioInfo.value = thenable;
          var displayName = thenable.displayName;
          typeof displayName === "string" && (ioInfo.name = displayName);
        }
        payload._status === -1 && (payload._status = 0, payload._result = thenable);
      }
      if (payload._status === 1)
        return ioInfo = payload._result, ioInfo === undefined && console.error(`lazy: Expected the result of a dynamic import() call. Instead received: %s

Your code should look like: 
  const MyComponent = lazy(() => import('./MyComponent'))

Did you accidentally put curly braces around the import?`, ioInfo), "default" in ioInfo || console.error(`lazy: Expected the result of a dynamic import() call. Instead received: %s

Your code should look like: 
  const MyComponent = lazy(() => import('./MyComponent'))`, ioInfo), ioInfo.default;
      throw payload._result;
    }
    function resolveDispatcher() {
      var dispatcher = ReactSharedInternals.H;
      dispatcher === null && console.error(`Invalid hook call. Hooks can only be called inside of the body of a function component. This could happen for one of the following reasons:
1. You might have mismatching versions of React and the renderer (such as React DOM)
2. You might be breaking the Rules of Hooks
3. You might have more than one copy of React in the same app
See https://react.dev/link/invalid-hook-call for tips about how to debug and fix this problem.`);
      return dispatcher;
    }
    function releaseAsyncTransition() {
      ReactSharedInternals.asyncTransitions--;
    }
    function enqueueTask(task) {
      if (enqueueTaskImpl === null)
        try {
          var requireString = ("require" + Math.random()).slice(0, 7);
          enqueueTaskImpl = (module && module[requireString]).call(module, "timers").setImmediate;
        } catch (_err) {
          enqueueTaskImpl = function(callback) {
            didWarnAboutMessageChannel === false && (didWarnAboutMessageChannel = true, typeof MessageChannel === "undefined" && console.error("This browser does not have a MessageChannel implementation, so enqueuing tasks via await act(async () => ...) will fail. Please file an issue at https://github.com/facebook/react/issues if you encounter this warning."));
            var channel = new MessageChannel;
            channel.port1.onmessage = callback;
            channel.port2.postMessage(undefined);
          };
        }
      return enqueueTaskImpl(task);
    }
    function aggregateErrors(errors) {
      return 1 < errors.length && typeof AggregateError === "function" ? new AggregateError(errors) : errors[0];
    }
    function popActScope(prevActQueue, prevActScopeDepth) {
      prevActScopeDepth !== actScopeDepth - 1 && console.error("You seem to have overlapping act() calls, this is not supported. Be sure to await previous act() calls before making a new one. ");
      actScopeDepth = prevActScopeDepth;
    }
    function recursivelyFlushAsyncActWork(returnValue, resolve, reject) {
      var queue = ReactSharedInternals.actQueue;
      if (queue !== null)
        if (queue.length !== 0)
          try {
            flushActQueue(queue);
            enqueueTask(function() {
              return recursivelyFlushAsyncActWork(returnValue, resolve, reject);
            });
            return;
          } catch (error) {
            ReactSharedInternals.thrownErrors.push(error);
          }
        else
          ReactSharedInternals.actQueue = null;
      0 < ReactSharedInternals.thrownErrors.length ? (queue = aggregateErrors(ReactSharedInternals.thrownErrors), ReactSharedInternals.thrownErrors.length = 0, reject(queue)) : resolve(returnValue);
    }
    function flushActQueue(queue) {
      if (!isFlushing) {
        isFlushing = true;
        var i = 0;
        try {
          for (;i < queue.length; i++) {
            var callback = queue[i];
            do {
              ReactSharedInternals.didUsePromise = false;
              var continuation = callback(false);
              if (continuation !== null) {
                if (ReactSharedInternals.didUsePromise) {
                  queue[i] = callback;
                  queue.splice(0, i);
                  return;
                }
                callback = continuation;
              } else
                break;
            } while (1);
          }
          queue.length = 0;
        } catch (error) {
          queue.splice(0, i + 1), ReactSharedInternals.thrownErrors.push(error);
        } finally {
          isFlushing = false;
        }
      }
    }
    typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ !== "undefined" && typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStart === "function" && __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStart(Error());
    var REACT_ELEMENT_TYPE = Symbol.for("react.transitional.element"), REACT_PORTAL_TYPE = Symbol.for("react.portal"), REACT_FRAGMENT_TYPE = Symbol.for("react.fragment"), REACT_STRICT_MODE_TYPE = Symbol.for("react.strict_mode"), REACT_PROFILER_TYPE = Symbol.for("react.profiler"), REACT_CONSUMER_TYPE = Symbol.for("react.consumer"), REACT_CONTEXT_TYPE = Symbol.for("react.context"), REACT_FORWARD_REF_TYPE = Symbol.for("react.forward_ref"), REACT_SUSPENSE_TYPE = Symbol.for("react.suspense"), REACT_SUSPENSE_LIST_TYPE = Symbol.for("react.suspense_list"), REACT_MEMO_TYPE = Symbol.for("react.memo"), REACT_LAZY_TYPE = Symbol.for("react.lazy"), REACT_ACTIVITY_TYPE = Symbol.for("react.activity"), MAYBE_ITERATOR_SYMBOL = Symbol.iterator, didWarnStateUpdateForUnmountedComponent = {}, ReactNoopUpdateQueue = {
      isMounted: function() {
        return false;
      },
      enqueueForceUpdate: function(publicInstance) {
        warnNoop(publicInstance, "forceUpdate");
      },
      enqueueReplaceState: function(publicInstance) {
        warnNoop(publicInstance, "replaceState");
      },
      enqueueSetState: function(publicInstance) {
        warnNoop(publicInstance, "setState");
      }
    }, assign = Object.assign, emptyObject = {};
    Object.freeze(emptyObject);
    Component.prototype.isReactComponent = {};
    Component.prototype.setState = function(partialState, callback) {
      if (typeof partialState !== "object" && typeof partialState !== "function" && partialState != null)
        throw Error("takes an object of state variables to update or a function which returns an object of state variables.");
      this.updater.enqueueSetState(this, partialState, callback, "setState");
    };
    Component.prototype.forceUpdate = function(callback) {
      this.updater.enqueueForceUpdate(this, callback, "forceUpdate");
    };
    var deprecatedAPIs = {
      isMounted: [
        "isMounted",
        "Instead, make sure to clean up subscriptions and pending requests in componentWillUnmount to prevent memory leaks."
      ],
      replaceState: [
        "replaceState",
        "Refactor your code to use setState instead (see https://github.com/facebook/react/issues/3236)."
      ]
    };
    for (fnName in deprecatedAPIs)
      deprecatedAPIs.hasOwnProperty(fnName) && defineDeprecationWarning(fnName, deprecatedAPIs[fnName]);
    ComponentDummy.prototype = Component.prototype;
    deprecatedAPIs = PureComponent.prototype = new ComponentDummy;
    deprecatedAPIs.constructor = PureComponent;
    assign(deprecatedAPIs, Component.prototype);
    deprecatedAPIs.isPureReactComponent = true;
    var isArrayImpl = Array.isArray, REACT_CLIENT_REFERENCE = Symbol.for("react.client.reference"), ReactSharedInternals = {
      H: null,
      A: null,
      T: null,
      S: null,
      actQueue: null,
      asyncTransitions: 0,
      isBatchingLegacy: false,
      didScheduleLegacyUpdate: false,
      didUsePromise: false,
      thrownErrors: [],
      getCurrentStack: null,
      recentlyCreatedOwnerStacks: 0
    }, hasOwnProperty = Object.prototype.hasOwnProperty, createTask = console.createTask ? console.createTask : function() {
      return null;
    };
    deprecatedAPIs = {
      react_stack_bottom_frame: function(callStackForError) {
        return callStackForError();
      }
    };
    var specialPropKeyWarningShown, didWarnAboutOldJSXRuntime;
    var didWarnAboutElementRef = {};
    var unknownOwnerDebugStack = deprecatedAPIs.react_stack_bottom_frame.bind(deprecatedAPIs, UnknownOwner)();
    var unknownOwnerDebugTask = createTask(getTaskName(UnknownOwner));
    var didWarnAboutMaps = false, userProvidedKeyEscapeRegex = /\/+/g, reportGlobalError = typeof reportError === "function" ? reportError : function(error) {
      if (typeof window === "object" && typeof window.ErrorEvent === "function") {
        var event = new window.ErrorEvent("error", {
          bubbles: true,
          cancelable: true,
          message: typeof error === "object" && error !== null && typeof error.message === "string" ? String(error.message) : String(error),
          error
        });
        if (!window.dispatchEvent(event))
          return;
      } else if (typeof process === "object" && typeof process.emit === "function") {
        process.emit("uncaughtException", error);
        return;
      }
      console.error(error);
    }, didWarnAboutMessageChannel = false, enqueueTaskImpl = null, actScopeDepth = 0, didWarnNoAwaitAct = false, isFlushing = false, queueSeveralMicrotasks = typeof queueMicrotask === "function" ? function(callback) {
      queueMicrotask(function() {
        return queueMicrotask(callback);
      });
    } : enqueueTask;
    deprecatedAPIs = Object.freeze({
      __proto__: null,
      c: function(size) {
        return resolveDispatcher().useMemoCache(size);
      }
    });
    var fnName = {
      map: mapChildren,
      forEach: function(children, forEachFunc, forEachContext) {
        mapChildren(children, function() {
          forEachFunc.apply(this, arguments);
        }, forEachContext);
      },
      count: function(children) {
        var n = 0;
        mapChildren(children, function() {
          n++;
        });
        return n;
      },
      toArray: function(children) {
        return mapChildren(children, function(child) {
          return child;
        }) || [];
      },
      only: function(children) {
        if (!isValidElement(children))
          throw Error("React.Children.only expected to receive a single React element child.");
        return children;
      }
    };
    exports.Activity = REACT_ACTIVITY_TYPE;
    exports.Children = fnName;
    exports.Component = Component;
    exports.Fragment = REACT_FRAGMENT_TYPE;
    exports.Profiler = REACT_PROFILER_TYPE;
    exports.PureComponent = PureComponent;
    exports.StrictMode = REACT_STRICT_MODE_TYPE;
    exports.Suspense = REACT_SUSPENSE_TYPE;
    exports.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE = ReactSharedInternals;
    exports.__COMPILER_RUNTIME = deprecatedAPIs;
    exports.act = function(callback) {
      var prevActQueue = ReactSharedInternals.actQueue, prevActScopeDepth = actScopeDepth;
      actScopeDepth++;
      var queue = ReactSharedInternals.actQueue = prevActQueue !== null ? prevActQueue : [], didAwaitActCall = false;
      try {
        var result = callback();
      } catch (error) {
        ReactSharedInternals.thrownErrors.push(error);
      }
      if (0 < ReactSharedInternals.thrownErrors.length)
        throw popActScope(prevActQueue, prevActScopeDepth), callback = aggregateErrors(ReactSharedInternals.thrownErrors), ReactSharedInternals.thrownErrors.length = 0, callback;
      if (result !== null && typeof result === "object" && typeof result.then === "function") {
        var thenable = result;
        queueSeveralMicrotasks(function() {
          didAwaitActCall || didWarnNoAwaitAct || (didWarnNoAwaitAct = true, console.error("You called act(async () => ...) without await. This could lead to unexpected testing behaviour, interleaving multiple act calls and mixing their scopes. You should - await act(async () => ...);"));
        });
        return {
          then: function(resolve, reject) {
            didAwaitActCall = true;
            thenable.then(function(returnValue) {
              popActScope(prevActQueue, prevActScopeDepth);
              if (prevActScopeDepth === 0) {
                try {
                  flushActQueue(queue), enqueueTask(function() {
                    return recursivelyFlushAsyncActWork(returnValue, resolve, reject);
                  });
                } catch (error$0) {
                  ReactSharedInternals.thrownErrors.push(error$0);
                }
                if (0 < ReactSharedInternals.thrownErrors.length) {
                  var _thrownError = aggregateErrors(ReactSharedInternals.thrownErrors);
                  ReactSharedInternals.thrownErrors.length = 0;
                  reject(_thrownError);
                }
              } else
                resolve(returnValue);
            }, function(error) {
              popActScope(prevActQueue, prevActScopeDepth);
              0 < ReactSharedInternals.thrownErrors.length ? (error = aggregateErrors(ReactSharedInternals.thrownErrors), ReactSharedInternals.thrownErrors.length = 0, reject(error)) : reject(error);
            });
          }
        };
      }
      var returnValue$jscomp$0 = result;
      popActScope(prevActQueue, prevActScopeDepth);
      prevActScopeDepth === 0 && (flushActQueue(queue), queue.length !== 0 && queueSeveralMicrotasks(function() {
        didAwaitActCall || didWarnNoAwaitAct || (didWarnNoAwaitAct = true, console.error("A component suspended inside an `act` scope, but the `act` call was not awaited. When testing React components that depend on asynchronous data, you must await the result:\n\nawait act(() => ...)"));
      }), ReactSharedInternals.actQueue = null);
      if (0 < ReactSharedInternals.thrownErrors.length)
        throw callback = aggregateErrors(ReactSharedInternals.thrownErrors), ReactSharedInternals.thrownErrors.length = 0, callback;
      return {
        then: function(resolve, reject) {
          didAwaitActCall = true;
          prevActScopeDepth === 0 ? (ReactSharedInternals.actQueue = queue, enqueueTask(function() {
            return recursivelyFlushAsyncActWork(returnValue$jscomp$0, resolve, reject);
          })) : resolve(returnValue$jscomp$0);
        }
      };
    };
    exports.cache = function(fn) {
      return function() {
        return fn.apply(null, arguments);
      };
    };
    exports.cacheSignal = function() {
      return null;
    };
    exports.captureOwnerStack = function() {
      var getCurrentStack = ReactSharedInternals.getCurrentStack;
      return getCurrentStack === null ? null : getCurrentStack();
    };
    exports.cloneElement = function(element, config, children) {
      if (element === null || element === undefined)
        throw Error("The argument must be a React element, but you passed " + element + ".");
      var props = assign({}, element.props), { key, _owner: owner } = element;
      if (config != null) {
        var JSCompiler_inline_result;
        a: {
          if (hasOwnProperty.call(config, "ref") && (JSCompiler_inline_result = Object.getOwnPropertyDescriptor(config, "ref").get) && JSCompiler_inline_result.isReactWarning) {
            JSCompiler_inline_result = false;
            break a;
          }
          JSCompiler_inline_result = config.ref !== undefined;
        }
        JSCompiler_inline_result && (owner = getOwner());
        hasValidKey(config) && (checkKeyStringCoercion(config.key), key = "" + config.key);
        for (propName in config)
          !hasOwnProperty.call(config, propName) || propName === "key" || propName === "__self" || propName === "__source" || propName === "ref" && config.ref === undefined || (props[propName] = config[propName]);
      }
      var propName = arguments.length - 2;
      if (propName === 1)
        props.children = children;
      else if (1 < propName) {
        JSCompiler_inline_result = Array(propName);
        for (var i = 0;i < propName; i++)
          JSCompiler_inline_result[i] = arguments[i + 2];
        props.children = JSCompiler_inline_result;
      }
      props = ReactElement(element.type, key, props, owner, element._debugStack, element._debugTask);
      for (key = 2;key < arguments.length; key++)
        validateChildKeys(arguments[key]);
      return props;
    };
    exports.createContext = function(defaultValue) {
      defaultValue = {
        $$typeof: REACT_CONTEXT_TYPE,
        _currentValue: defaultValue,
        _currentValue2: defaultValue,
        _threadCount: 0,
        Provider: null,
        Consumer: null
      };
      defaultValue.Provider = defaultValue;
      defaultValue.Consumer = {
        $$typeof: REACT_CONSUMER_TYPE,
        _context: defaultValue
      };
      defaultValue._currentRenderer = null;
      defaultValue._currentRenderer2 = null;
      return defaultValue;
    };
    exports.createElement = function(type, config, children) {
      for (var i = 2;i < arguments.length; i++)
        validateChildKeys(arguments[i]);
      i = {};
      var key = null;
      if (config != null)
        for (propName in didWarnAboutOldJSXRuntime || !("__self" in config) || "key" in config || (didWarnAboutOldJSXRuntime = true, console.warn("Your app (or one of its dependencies) is using an outdated JSX transform. Update to the modern JSX transform for faster performance: https://react.dev/link/new-jsx-transform")), hasValidKey(config) && (checkKeyStringCoercion(config.key), key = "" + config.key), config)
          hasOwnProperty.call(config, propName) && propName !== "key" && propName !== "__self" && propName !== "__source" && (i[propName] = config[propName]);
      var childrenLength = arguments.length - 2;
      if (childrenLength === 1)
        i.children = children;
      else if (1 < childrenLength) {
        for (var childArray = Array(childrenLength), _i = 0;_i < childrenLength; _i++)
          childArray[_i] = arguments[_i + 2];
        Object.freeze && Object.freeze(childArray);
        i.children = childArray;
      }
      if (type && type.defaultProps)
        for (propName in childrenLength = type.defaultProps, childrenLength)
          i[propName] === undefined && (i[propName] = childrenLength[propName]);
      key && defineKeyPropWarningGetter(i, typeof type === "function" ? type.displayName || type.name || "Unknown" : type);
      var propName = 1e4 > ReactSharedInternals.recentlyCreatedOwnerStacks++;
      return ReactElement(type, key, i, getOwner(), propName ? Error("react-stack-top-frame") : unknownOwnerDebugStack, propName ? createTask(getTaskName(type)) : unknownOwnerDebugTask);
    };
    exports.createRef = function() {
      var refObject = { current: null };
      Object.seal(refObject);
      return refObject;
    };
    exports.forwardRef = function(render) {
      render != null && render.$$typeof === REACT_MEMO_TYPE ? console.error("forwardRef requires a render function but received a `memo` component. Instead of forwardRef(memo(...)), use memo(forwardRef(...)).") : typeof render !== "function" ? console.error("forwardRef requires a render function but was given %s.", render === null ? "null" : typeof render) : render.length !== 0 && render.length !== 2 && console.error("forwardRef render functions accept exactly two parameters: props and ref. %s", render.length === 1 ? "Did you forget to use the ref parameter?" : "Any additional parameter will be undefined.");
      render != null && render.defaultProps != null && console.error("forwardRef render functions do not support defaultProps. Did you accidentally pass a React component?");
      var elementType = { $$typeof: REACT_FORWARD_REF_TYPE, render }, ownName;
      Object.defineProperty(elementType, "displayName", {
        enumerable: false,
        configurable: true,
        get: function() {
          return ownName;
        },
        set: function(name) {
          ownName = name;
          render.name || render.displayName || (Object.defineProperty(render, "name", { value: name }), render.displayName = name);
        }
      });
      return elementType;
    };
    exports.isValidElement = isValidElement;
    exports.lazy = function(ctor) {
      ctor = { _status: -1, _result: ctor };
      var lazyType = {
        $$typeof: REACT_LAZY_TYPE,
        _payload: ctor,
        _init: lazyInitializer
      }, ioInfo = {
        name: "lazy",
        start: -1,
        end: -1,
        value: null,
        owner: null,
        debugStack: Error("react-stack-top-frame"),
        debugTask: console.createTask ? console.createTask("lazy()") : null
      };
      ctor._ioInfo = ioInfo;
      lazyType._debugInfo = [{ awaited: ioInfo }];
      return lazyType;
    };
    exports.memo = function(type, compare) {
      type == null && console.error("memo: The first argument must be a component. Instead received: %s", type === null ? "null" : typeof type);
      compare = {
        $$typeof: REACT_MEMO_TYPE,
        type,
        compare: compare === undefined ? null : compare
      };
      var ownName;
      Object.defineProperty(compare, "displayName", {
        enumerable: false,
        configurable: true,
        get: function() {
          return ownName;
        },
        set: function(name) {
          ownName = name;
          type.name || type.displayName || (Object.defineProperty(type, "name", { value: name }), type.displayName = name);
        }
      });
      return compare;
    };
    exports.startTransition = function(scope) {
      var prevTransition = ReactSharedInternals.T, currentTransition = {};
      currentTransition._updatedFibers = new Set;
      ReactSharedInternals.T = currentTransition;
      try {
        var returnValue = scope(), onStartTransitionFinish = ReactSharedInternals.S;
        onStartTransitionFinish !== null && onStartTransitionFinish(currentTransition, returnValue);
        typeof returnValue === "object" && returnValue !== null && typeof returnValue.then === "function" && (ReactSharedInternals.asyncTransitions++, returnValue.then(releaseAsyncTransition, releaseAsyncTransition), returnValue.then(noop, reportGlobalError));
      } catch (error) {
        reportGlobalError(error);
      } finally {
        prevTransition === null && currentTransition._updatedFibers && (scope = currentTransition._updatedFibers.size, currentTransition._updatedFibers.clear(), 10 < scope && console.warn("Detected a large number of updates inside startTransition. If this is due to a subscription please re-write it to use React provided hooks. Otherwise concurrent mode guarantees are off the table.")), prevTransition !== null && currentTransition.types !== null && (prevTransition.types !== null && prevTransition.types !== currentTransition.types && console.error("We expected inner Transitions to have transferred the outer types set and that you cannot add to the outer Transition while inside the inner.This is a bug in React."), prevTransition.types = currentTransition.types), ReactSharedInternals.T = prevTransition;
      }
    };
    exports.unstable_useCacheRefresh = function() {
      return resolveDispatcher().useCacheRefresh();
    };
    exports.use = function(usable) {
      return resolveDispatcher().use(usable);
    };
    exports.useActionState = function(action, initialState, permalink) {
      return resolveDispatcher().useActionState(action, initialState, permalink);
    };
    exports.useCallback = function(callback, deps) {
      return resolveDispatcher().useCallback(callback, deps);
    };
    exports.useContext = function(Context) {
      var dispatcher = resolveDispatcher();
      Context.$$typeof === REACT_CONSUMER_TYPE && console.error("Calling useContext(Context.Consumer) is not supported and will cause bugs. Did you mean to call useContext(Context) instead?");
      return dispatcher.useContext(Context);
    };
    exports.useDebugValue = function(value, formatterFn) {
      return resolveDispatcher().useDebugValue(value, formatterFn);
    };
    exports.useDeferredValue = function(value, initialValue) {
      return resolveDispatcher().useDeferredValue(value, initialValue);
    };
    exports.useEffect = function(create, deps) {
      create == null && console.warn("React Hook useEffect requires an effect callback. Did you forget to pass a callback to the hook?");
      return resolveDispatcher().useEffect(create, deps);
    };
    exports.useEffectEvent = function(callback) {
      return resolveDispatcher().useEffectEvent(callback);
    };
    exports.useId = function() {
      return resolveDispatcher().useId();
    };
    exports.useImperativeHandle = function(ref, create, deps) {
      return resolveDispatcher().useImperativeHandle(ref, create, deps);
    };
    exports.useInsertionEffect = function(create, deps) {
      create == null && console.warn("React Hook useInsertionEffect requires an effect callback. Did you forget to pass a callback to the hook?");
      return resolveDispatcher().useInsertionEffect(create, deps);
    };
    exports.useLayoutEffect = function(create, deps) {
      create == null && console.warn("React Hook useLayoutEffect requires an effect callback. Did you forget to pass a callback to the hook?");
      return resolveDispatcher().useLayoutEffect(create, deps);
    };
    exports.useMemo = function(create, deps) {
      return resolveDispatcher().useMemo(create, deps);
    };
    exports.useOptimistic = function(passthrough, reducer) {
      return resolveDispatcher().useOptimistic(passthrough, reducer);
    };
    exports.useReducer = function(reducer, initialArg, init) {
      return resolveDispatcher().useReducer(reducer, initialArg, init);
    };
    exports.useRef = function(initialValue) {
      return resolveDispatcher().useRef(initialValue);
    };
    exports.useState = function(initialState) {
      return resolveDispatcher().useState(initialState);
    };
    exports.useSyncExternalStore = function(subscribe, getSnapshot, getServerSnapshot) {
      return resolveDispatcher().useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
    };
    exports.useTransition = function() {
      return resolveDispatcher().useTransition();
    };
    exports.version = "19.2.4";
    typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ !== "undefined" && typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStop === "function" && __REACT_DEVTOOLS_GLOBAL_HOOK__.registerInternalModuleStop(Error());
  })();
});

// node_modules/react/index.js
var require_react = __commonJS(function(exports, module) {
  var react_development = __toESM(require_react_development());
  if (false) {} else {
    module.exports = react_development;
  }
});

// src/lib/offline/offline-expense-queue.ts
var exports_offline_expense_queue = {};
__export(exports_offline_expense_queue, {
  claimForSync: () => claimForSync,
  clearForUser: () => clearForUser,
  countPending: () => countPending,
  deleteExpenseSilent: () => deleteExpenseSilent,
  enqueueExpense: () => enqueueExpense,
  listExpenses: () => listExpenses,
  removeExpense: () => removeExpense,
  subscribe: () => subscribe,
  updateExpense: () => updateExpense
});

// src/lib/offline/offline-sync-history.ts
var DB_NAME = "gf_offline_history";
var DB_VERSION = 1;
var STORE = "events";
var MAX_PER_USER = 100;
function isBrowser() {
  return typeof window !== "undefined" && typeof indexedDB !== "undefined";
}
function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        const s = db.createObjectStore(STORE, { keyPath: "id" });
        s.createIndex("user_id", "user_id", { unique: false });
        s.createIndex("created_at", "created_at", { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
async function tx(mode, fn) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const t = db.transaction(STORE, mode);
    const s = t.objectStore(STORE);
    let result;
    Promise.resolve(fn(s)).then((r) => {
      result = r;
    }).catch(reject);
    t.oncomplete = () => resolve(result);
    t.onerror = () => reject(t.error);
    t.onabort = () => reject(t.error);
  });
}
var listeners = new Set;
function emit() {
  for (const l of listeners) {
    try {
      l();
    } catch {}
  }
}
function genId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `hist-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
var FORBIDDEN_KEYS = [
  "token",
  "access_token",
  "refresh_token",
  "password",
  "senha",
  "session",
  "jwt",
  "secret",
  "cookie",
  "authorization"
];
function sanitizeMetadata(meta) {
  if (!meta)
    return;
  const out = {};
  for (const [k, v] of Object.entries(meta)) {
    const lower = k.toLowerCase();
    if (FORBIDDEN_KEYS.some((bad) => lower.includes(bad)))
      continue;
    if (v == null || typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
      out[k] = v ?? null;
    }
  }
  return out;
}
async function recordHistoryEvent(input) {
  if (!isBrowser() || !input.user_id)
    return;
  const event = {
    id: genId(),
    user_id: input.user_id,
    type: input.type,
    action: input.action,
    title: (input.title || "").slice(0, 200),
    amount: Number.isFinite(input.amount) ? input.amount : 0,
    created_at: Date.now(),
    metadata: sanitizeMetadata(input.metadata),
    error_message: input.error_message,
    technical_error: input.technical_error
  };
  try {
    await tx("readwrite", (s) => {
      s.add(event);
    });
    await trimUserHistory(input.user_id);
    emit();
  } catch {}
}
async function listHistory(userId) {
  if (!isBrowser() || !userId)
    return [];
  return tx("readonly", (s) => {
    return new Promise((resolve, reject) => {
      const out = [];
      const idx = s.index("user_id");
      const req = idx.openCursor(IDBKeyRange.only(userId));
      req.onsuccess = () => {
        const cur = req.result;
        if (cur) {
          out.push(cur.value);
          cur.continue();
        } else {
          out.sort((a, b) => b.created_at - a.created_at);
          resolve(out);
        }
      };
      req.onerror = () => reject(req.error);
    });
  });
}
async function trimUserHistory(userId) {
  const all = await listHistory(userId);
  if (all.length <= MAX_PER_USER)
    return;
  const toDelete = all.slice(MAX_PER_USER);
  await tx("readwrite", (s) => {
    for (const ev of toDelete)
      s.delete(ev.id);
  });
}

// src/lib/financial-limits.ts
var MAX_FINANCIAL_ENTRY_AMOUNT = 999999999.99;
function validateFinancialAmount(value) {
  if (typeof value !== "number")
    return { ok: false, code: "not_a_number" };
  if (Number.isNaN(value))
    return { ok: false, code: "not_a_number" };
  if (!Number.isFinite(value))
    return { ok: false, code: "not_finite" };
  if (value <= 0)
    return { ok: false, code: "too_small" };
  const rounded = Math.round(value * 100) / 100;
  if (Math.abs(rounded - value) > 0.000000001)
    return { ok: false, code: "too_many_decimals" };
  if (rounded > MAX_FINANCIAL_ENTRY_AMOUNT)
    return { ok: false, code: "too_large" };
  return { ok: true, value: rounded };
}

// src/lib/offline/offline-validation.ts
function isValidOfflineDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value))
    return false;
  const date = new Date(`${value}T12:00:00Z`);
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

// src/lib/offline/offline-expense-queue.ts
var DB_NAME2 = "gf_offline";
var DB_VERSION2 = 1;
var STORE2 = "expenses";
function isBrowser2() {
  return typeof window !== "undefined" && typeof indexedDB !== "undefined";
}
function openDb2() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME2, DB_VERSION2);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE2)) {
        const store = db.createObjectStore(STORE2, { keyPath: "local_id" });
        store.createIndex("user_id", "user_id", { unique: false });
        store.createIndex("status", "status", { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
async function tx2(mode, fn) {
  const db = await openDb2();
  return new Promise((resolve, reject) => {
    const t = db.transaction(STORE2, mode);
    const store = t.objectStore(STORE2);
    let result;
    Promise.resolve(fn(store)).then((r) => {
      result = r;
    }).catch((error) => {
      try {
        t.abort();
      } catch {}
      reject(error);
    });
    t.oncomplete = () => {
      db.close();
      resolve(result);
    };
    t.onerror = () => {
      db.close();
      reject(t.error);
    };
    t.onabort = () => {
      db.close();
      reject(t.error);
    };
  });
}
var listeners2 = new Set;
function emit2() {
  for (const l of listeners2) {
    try {
      l();
    } catch {}
  }
}
function subscribe(cb) {
  listeners2.add(cb);
  return () => listeners2.delete(cb);
}
function genId2() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `local-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
async function enqueueExpense(userId, input, actorId = userId) {
  if (!isBrowser2())
    throw new Error("offline queue unavailable");
  if (!userId || !actorId || !validateFinancialAmount(input.valor).ok || !isValidOfflineDate(input.data))
    throw new Error("Informe uma conta, data e valor válidos.");
  const now = Date.now();
  const item = {
    local_id: genId2(),
    user_id: userId,
    actor_id: actorId,
    input,
    descricao: (input.descricao || input.estabelecimento || "Gasto").trim(),
    valor: input.valor,
    data: input.data,
    forma_pagamento: input.formaPagamento,
    cartao_id: input.cartaoId,
    observacao: input.observacao,
    created_at: now,
    updated_at: now,
    status: "pending",
    attempts: 0
  };
  await tx2("readwrite", (s) => {
    s.add(item);
  });
  emit2();
  recordHistoryEvent({
    user_id: userId,
    type: "expense",
    action: "created_offline",
    title: item.descricao,
    amount: item.valor
  });
  return item;
}
async function listExpenses(userId, actorId = userId, allOwners = false) {
  if (!isBrowser2())
    return [];
  return tx2("readonly", (s) => {
    return new Promise((resolve, reject) => {
      const out = [];
      const idx = s.index("user_id");
      const req = allOwners ? s.openCursor() : idx.openCursor(IDBKeyRange.only(userId));
      req.onsuccess = () => {
        const cur = req.result;
        if (cur) {
          const v = cur.value;
          if (v.status !== "synced" && (v.actor_id ?? v.user_id) === actorId)
            out.push(v);
          cur.continue();
        } else {
          out.sort((a, b) => a.created_at - b.created_at);
          resolve(out);
        }
      };
      req.onerror = () => reject(req.error);
    });
  });
}
async function countPending(userId) {
  const all = await listExpenses(userId);
  return all.filter((e) => e.status !== "synced").length;
}
async function deleteExpenseSilent(localId, userId, attempt, actorId = userId) {
  await tx2("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const request = s.get(localId);
      request.onsuccess = () => {
        const current = request.result;
        if (current?.user_id === userId && (current.actor_id ?? current.user_id) === actorId && current.attempts === attempt && current.status === "syncing")
          s.delete(localId);
        resolve();
      };
      request.onerror = () => reject(request.error);
    });
  });
  emit2();
}
async function removeExpense(localId, userId, actorId = userId) {
  let snapshot;
  await tx2("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const g = s.get(localId);
      g.onsuccess = () => {
        snapshot = g.result;
        if (snapshot && (snapshot.user_id !== userId || (snapshot.actor_id ?? snapshot.user_id) !== actorId)) {
          reject(new Error("Este gasto pertence a outra conta."));
          return;
        }
        if (snapshot && (snapshot.status === "syncing" || snapshot.attempts > 0)) {
          reject(new Error("Este envio precisa ser conferido antes de remover. Tente sincronizar novamente."));
          return;
        }
        s.delete(localId);
        resolve();
      };
      g.onerror = () => reject(g.error);
    });
  });
  emit2();
  if (snapshot) {
    recordHistoryEvent({
      user_id: snapshot.user_id,
      type: "expense",
      action: "removed",
      title: snapshot.descricao,
      amount: snapshot.valor
    });
  }
}
async function updateExpense(localId, patch, userId, expectedAttempt, actorId = userId) {
  await tx2("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const req = s.get(localId);
      req.onsuccess = () => {
        const current = req.result;
        if (!current) {
          resolve();
          return;
        }
        if (current.user_id !== userId || (current.actor_id ?? current.user_id) !== actorId) {
          reject(new Error("Este gasto pertence a outra conta."));
          return;
        }
        if (expectedAttempt !== undefined && (current.attempts !== expectedAttempt || current.status !== "syncing")) {
          resolve();
          return;
        }
        if (patch.input && (!validateFinancialAmount(patch.input.valor).ok || !isValidOfflineDate(patch.input.data))) {
          reject(new Error("Informe uma data e valor válidos."));
          return;
        }
        if (patch.input && (current.status === "syncing" || current.attempts > 0)) {
          reject(new Error("Este envio precisa ser conferido antes de editar."));
          return;
        }
        const merged = {
          ...current,
          ...patch,
          local_id: current.local_id,
          user_id: current.user_id,
          actor_id: current.actor_id,
          attempts: current.attempts,
          input: patch.input ?? current.input,
          updated_at: Date.now()
        };
        const put = s.put(merged);
        put.onsuccess = () => resolve();
        put.onerror = () => reject(put.error);
      };
      req.onerror = () => reject(req.error);
    });
  });
  emit2();
}
async function clearForUser(userId) {
  await tx2("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const idx = s.index("user_id");
      const req = idx.openCursor(IDBKeyRange.only(userId));
      req.onsuccess = () => {
        const cur = req.result;
        if (cur) {
          cur.delete();
          cur.continue();
        } else {
          resolve();
        }
      };
      req.onerror = () => reject(req.error);
    });
  });
  emit2();
}
async function claimForSync(localId, userId, actorId = userId) {
  let claimed = null;
  await tx2("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const req = s.get(localId);
      req.onsuccess = () => {
        const cur = req.result;
        if (!cur || cur.user_id !== userId || (cur.actor_id ?? cur.user_id) !== actorId || cur.status === "synced")
          return resolve();
        if (cur.status === "syncing" && Date.now() - cur.updated_at < 120000)
          return resolve();
        cur.status = "syncing";
        cur.attempts += 1;
        cur.updated_at = Date.now();
        const put = s.put(cur);
        put.onsuccess = () => {
          claimed = cur;
          resolve();
        };
        put.onerror = () => reject(put.error);
      };
      req.onerror = () => reject(req.error);
    });
  });
  if (claimed)
    emit2();
  return claimed;
}

// src/lib/offline/offline-income-queue.ts
var exports_offline_income_queue = {};
__export(exports_offline_income_queue, {
  claimForSync: () => claimForSync2,
  countPending: () => countPending2,
  deleteIncomeSilent: () => deleteIncomeSilent,
  enqueueIncome: () => enqueueIncome,
  listIncomes: () => listIncomes,
  removeIncome: () => removeIncome,
  subscribe: () => subscribe2,
  updateIncome: () => updateIncome
});
var DB_NAME3 = "gf_offline_income";
var DB_VERSION3 = 1;
var STORE3 = "incomes";
function isBrowser3() {
  return typeof window !== "undefined" && typeof indexedDB !== "undefined";
}
function openDb3() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME3, DB_VERSION3);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE3)) {
        const store = db.createObjectStore(STORE3, { keyPath: "local_id" });
        store.createIndex("user_id", "user_id", { unique: false });
        store.createIndex("status", "status", { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
async function tx3(mode, fn) {
  const db = await openDb3();
  return new Promise((resolve, reject) => {
    const t = db.transaction(STORE3, mode);
    const store = t.objectStore(STORE3);
    let result;
    Promise.resolve(fn(store)).then((r) => {
      result = r;
    }).catch((error) => {
      try {
        t.abort();
      } catch {}
      reject(error);
    });
    t.oncomplete = () => {
      db.close();
      resolve(result);
    };
    t.onerror = () => {
      db.close();
      reject(t.error);
    };
    t.onabort = () => {
      db.close();
      reject(t.error);
    };
  });
}
var listeners3 = new Set;
function emit3() {
  for (const l of listeners3) {
    try {
      l();
    } catch {}
  }
}
function subscribe2(cb) {
  listeners3.add(cb);
  return () => listeners3.delete(cb);
}
function genId3() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `local-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
async function enqueueIncome(userId, input, actorId = userId) {
  if (!isBrowser3())
    throw new Error("offline queue unavailable");
  if (!userId || !actorId || !validateFinancialAmount(input.valor).ok || !isValidOfflineDate(input.data))
    throw new Error("Informe uma conta, data e valor válidos.");
  if (input.recorrente)
    throw new Error("A fila offline aceita apenas receita única.");
  const now = Date.now();
  const item = {
    local_id: genId3(),
    user_id: userId,
    actor_id: actorId,
    input,
    descricao: (input.descricao || "Receita").trim(),
    valor: input.valor,
    data: input.data,
    tipo: input.tipo,
    cliente_id: input.clienteId ?? null,
    created_at: now,
    updated_at: now,
    status: "pending",
    attempts: 0
  };
  await tx3("readwrite", (s) => {
    s.add(item);
  });
  emit3();
  recordHistoryEvent({
    user_id: userId,
    type: "income",
    action: "created_offline",
    title: item.descricao,
    amount: item.valor
  });
  return item;
}
async function listIncomes(userId, actorId = userId, allOwners = false) {
  if (!isBrowser3())
    return [];
  return tx3("readonly", (s) => {
    return new Promise((resolve, reject) => {
      const out = [];
      const idx = s.index("user_id");
      const req = allOwners ? s.openCursor() : idx.openCursor(IDBKeyRange.only(userId));
      req.onsuccess = () => {
        const cur = req.result;
        if (cur) {
          const v = cur.value;
          if (v.status !== "synced" && (v.actor_id ?? v.user_id) === actorId)
            out.push(v);
          cur.continue();
        } else {
          out.sort((a, b) => a.created_at - b.created_at);
          resolve(out);
        }
      };
      req.onerror = () => reject(req.error);
    });
  });
}
async function countPending2(userId) {
  const all = await listIncomes(userId);
  return all.filter((e) => e.status !== "synced").length;
}
async function deleteIncomeSilent(localId, userId, attempt, actorId = userId) {
  await tx3("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const request = s.get(localId);
      request.onsuccess = () => {
        const current = request.result;
        if (current?.user_id === userId && (current.actor_id ?? current.user_id) === actorId && current.attempts === attempt && current.status === "syncing")
          s.delete(localId);
        resolve();
      };
      request.onerror = () => reject(request.error);
    });
  });
  emit3();
}
async function removeIncome(localId, userId, actorId = userId) {
  let snapshot;
  await tx3("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const g = s.get(localId);
      g.onsuccess = () => {
        snapshot = g.result;
        if (snapshot && (snapshot.user_id !== userId || (snapshot.actor_id ?? snapshot.user_id) !== actorId)) {
          reject(new Error("Esta receita pertence a outra conta."));
          return;
        }
        if (snapshot && (snapshot.status === "syncing" || snapshot.attempts > 0)) {
          reject(new Error("Este envio precisa ser conferido antes de remover. Tente sincronizar novamente."));
          return;
        }
        s.delete(localId);
        resolve();
      };
      g.onerror = () => reject(g.error);
    });
  });
  emit3();
  if (snapshot) {
    recordHistoryEvent({
      user_id: snapshot.user_id,
      type: "income",
      action: "removed",
      title: snapshot.descricao,
      amount: snapshot.valor
    });
  }
}
async function updateIncome(localId, patch, userId, expectedAttempt, actorId = userId) {
  await tx3("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const req = s.get(localId);
      req.onsuccess = () => {
        const current = req.result;
        if (!current) {
          resolve();
          return;
        }
        if (current.user_id !== userId || (current.actor_id ?? current.user_id) !== actorId) {
          reject(new Error("Esta receita pertence a outra conta."));
          return;
        }
        if (expectedAttempt !== undefined && (current.attempts !== expectedAttempt || current.status !== "syncing")) {
          resolve();
          return;
        }
        if (patch.input && (!validateFinancialAmount(patch.input.valor).ok || !isValidOfflineDate(patch.input.data))) {
          reject(new Error("Informe uma data e valor válidos."));
          return;
        }
        if (patch.input && (current.status === "syncing" || current.attempts > 0)) {
          reject(new Error("Este envio precisa ser conferido antes de editar."));
          return;
        }
        const merged = {
          ...current,
          ...patch,
          local_id: current.local_id,
          user_id: current.user_id,
          actor_id: current.actor_id,
          attempts: current.attempts,
          input: patch.input ?? current.input,
          updated_at: Date.now()
        };
        const put = s.put(merged);
        put.onsuccess = () => resolve();
        put.onerror = () => reject(put.error);
      };
      req.onerror = () => reject(req.error);
    });
  });
  emit3();
}
async function claimForSync2(localId, userId, actorId = userId) {
  let claimed = null;
  await tx3("readwrite", (s) => {
    return new Promise((resolve, reject) => {
      const req = s.get(localId);
      req.onsuccess = () => {
        const cur = req.result;
        if (!cur || cur.user_id !== userId || (cur.actor_id ?? cur.user_id) !== actorId || cur.status === "synced")
          return resolve();
        if (cur.status === "syncing" && Date.now() - cur.updated_at < 120000)
          return resolve();
        cur.status = "syncing";
        cur.attempts += 1;
        cur.updated_at = Date.now();
        const put = s.put(cur);
        put.onsuccess = () => {
          claimed = cur;
          resolve();
        };
        put.onerror = () => reject(put.error);
      };
      req.onerror = () => reject(req.error);
    });
  });
  if (claimed)
    emit3();
  return claimed;
}

// src/lib/offline/use-offline-sync.tsx
var import_react = __toESM(require_react(), 1);

// tests/fixtures/offline-accounts/store.ts
async function addGastoAwait(input, owner, id, actor) {
  return save("expense", input, owner, id, actor);
}
async function addReceitaAwait(input, owner, id, actor) {
  return save("income", input, owner, id, actor);
}
async function save(type, input, owner, id, actor) {
  const w = window;
  await new Promise((resolve) => setTimeout(resolve, 50));
  if (w.deny || w.networkFailure)
    return { ok: false, error: w.deny ? "permission denied" : "network failure" };
  w.writes.push({ type, input, owner, id, actor });
  return { ok: true };
}

// src/lib/offline/offline-error-messages.ts
var GENERIC = "Não foi possível sincronizar esta pendência. Revise os dados e tente novamente.";
var CONNECTION = "Não foi possível sincronizar agora. Verifique sua conexão e tente novamente.";
var PERMISSION = "Não foi possível sincronizar por falta de permissão. Entre novamente na conta e tente de novo.";
var FK_CATEGORIA = "Não foi possível sincronizar porque a categoria usada não existe mais. Edite a pendência e escolha outra categoria.";
var FK_CARTAO = "Não foi possível sincronizar porque o cartão selecionado não existe mais. Edite a pendência e escolha outro cartão ou forma de pagamento.";
var FK_CLIENTE = "Não foi possível sincronizar porque o cliente selecionado não está mais disponível. Edite a pendência e escolha outro cliente.";
function normalizeOfflineError(raw) {
  const technical = raw instanceof Error ? raw.message : typeof raw === "string" ? raw : raw == null ? "" : (() => {
    try {
      return JSON.stringify(raw);
    } catch {
      return String(raw);
    }
  })();
  const t = technical.toLowerCase();
  if (t.includes("failed to fetch") || t.includes("networkerror") || t.includes("network error") || t.includes("network request failed") || t.includes("load failed") || t.includes("offline") || t.includes("err_internet_disconnected") || t.includes("err_network") || t.includes("timeout") || t.includes("timed out")) {
    return { friendly: CONNECTION, technical };
  }
  if (t.includes("row-level security") || t.includes("row level security") || t.includes("rls") || t.includes("permission denied") || t.includes("not authorized") || t.includes("unauthorized") || t.includes("jwt") || t.includes("invalid token") || t.includes("token expired") || t.includes("auth session missing") || t.includes("42501")) {
    return { friendly: PERMISSION, technical };
  }
  const isFk = t.includes("foreign key") || t.includes("violates foreign key") || t.includes("23503") || t.includes("is not present in table");
  if (isFk) {
    if (t.includes("categoria") || t.includes("category") || t.includes("categoria_id")) {
      return { friendly: FK_CATEGORIA, technical };
    }
    if (t.includes("cartao") || t.includes("cartão") || t.includes("card") || t.includes("cartao_id") || t.includes("card_id")) {
      return { friendly: FK_CARTAO, technical };
    }
    if (t.includes("cliente") || t.includes("client") || t.includes("cliente_id")) {
      return { friendly: FK_CLIENTE, technical };
    }
    return { friendly: GENERIC, technical };
  }
  return { friendly: GENERIC, technical };
}

// src/lib/offline/sync-attempt.ts
async function withSyncAttempt(key, work) {
  const run = async () => {
    const controller = new AbortController;
    let timer;
    const timeout = new Promise((_, reject) => {
      timer = setTimeout(() => {
        controller.abort();
        reject(new Error("Tempo de sincronização esgotado; tente novamente."));
      }, 60000);
    });
    try {
      await Promise.race([work(controller.signal), timeout]);
    } finally {
      clearTimeout(timer);
      controller.abort();
    }
  };
  if (typeof navigator !== "undefined" && navigator.locks) {
    await navigator.locks.request(key, { ifAvailable: true }, async (lock) => {
      if (lock)
        await run();
    });
  } else
    await run();
}

// src/lib/offline/use-offline-sync.tsx
var running = new Map;
function syncAllForUser(actorId) {
  if (!actorId || typeof navigator !== "undefined" && navigator.onLine === false)
    return Promise.resolve({ synced: 0, failed: 0 });
  const existing = running.get(actorId);
  if (existing)
    return existing;
  const task = (async () => {
    let synced = 0, failed = 0;
    for (const candidate of await listExpenses(actorId, actorId, true)) {
      if (typeof navigator !== "undefined" && navigator.onLine === false)
        break;
      try {
        await withSyncAttempt("gf:expense:" + candidate.local_id, async (signal) => {
          const item = await claimForSync(candidate.local_id, candidate.user_id, actorId);
          if (!item || signal.aborted)
            return;
          try {
            const result = await addGastoAwait(item.input, item.user_id, item.local_id, actorId, signal);
            if (signal.aborted)
              return;
            if (!result.ok)
              throw new Error(result.error || "Falha ao sincronizar");
            await deleteExpenseSilent(item.local_id, item.user_id, item.attempts, actorId);
            synced++;
            recordHistoryEvent({
              user_id: item.user_id,
              type: "expense",
              action: "synced",
              title: item.descricao,
              amount: item.valor
            });
          } catch (error) {
            if (signal.aborted)
              return;
            const norm = normalizeOfflineError(error);
            await updateExpense(item.local_id, { status: "failed", error_message: norm.friendly, technical_error: norm.technical }, item.user_id, item.attempts, actorId);
            failed++;
            recordHistoryEvent({
              user_id: item.user_id,
              type: "expense",
              action: "failed",
              title: item.descricao,
              amount: item.valor,
              error_message: norm.friendly,
              technical_error: norm.technical
            });
          }
        });
      } catch {
        failed++;
      }
    }
    return { synced, failed };
  })().finally(() => running.delete(actorId));
  running.set(actorId, task);
  return task;
}

// src/lib/offline/use-offline-income-sync.tsx
var import_react2 = __toESM(require_react(), 1);
var running2 = new Map;
function syncAllIncomesForUser(actorId) {
  if (!actorId || typeof navigator !== "undefined" && navigator.onLine === false)
    return Promise.resolve({ synced: 0, failed: 0 });
  const existing = running2.get(actorId);
  if (existing)
    return existing;
  const task = (async () => {
    let synced = 0, failed = 0;
    for (const candidate of await listIncomes(actorId, actorId, true)) {
      if (typeof navigator !== "undefined" && navigator.onLine === false)
        break;
      try {
        await withSyncAttempt("gf:income:" + candidate.local_id, async (signal) => {
          const item = await claimForSync2(candidate.local_id, candidate.user_id, actorId);
          if (!item || signal.aborted)
            return;
          try {
            const result = await addReceitaAwait(item.input, item.user_id, item.local_id, actorId, signal);
            if (signal.aborted)
              return;
            if (!result.ok)
              throw new Error(result.error || "Falha ao sincronizar");
            await deleteIncomeSilent(item.local_id, item.user_id, item.attempts, actorId);
            synced++;
            recordHistoryEvent({
              user_id: item.user_id,
              type: "income",
              action: "synced",
              title: item.descricao,
              amount: item.valor
            });
          } catch (error) {
            if (signal.aborted)
              return;
            const norm = normalizeOfflineError(error);
            await updateIncome(item.local_id, { status: "failed", error_message: norm.friendly, technical_error: norm.technical }, item.user_id, item.attempts, actorId);
            failed++;
            recordHistoryEvent({
              user_id: item.user_id,
              type: "income",
              action: "failed",
              title: item.descricao,
              amount: item.valor,
              error_message: norm.friendly,
              technical_error: norm.technical
            });
          }
        });
      } catch {
        failed++;
      }
    }
    return { synced, failed };
  })().finally(() => running2.delete(actorId));
  running2.set(actorId, task);
  return task;
}

// tests/fixtures/offline-accounts/entry.ts
Object.assign(window, {
  expenses: exports_offline_expense_queue,
  queue: exports_offline_expense_queue,
  incomes: exports_offline_income_queue,
  syncAllForUser,
  syncAllIncomesForUser,
  writes: [],
  deny: false,
  networkFailure: false
});
