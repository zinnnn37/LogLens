import { useMemo as T } from "react";
class h {
  static stack = [];
  static isPatched = !1;
  static OriginalPromise = null;
  static originalSetTimeout = globalThis.setTimeout;
  static originalSetInterval = globalThis.setInterval;
  static init() {
    if (this.isPatched) {
      console.warn("[LogLens] Already initialized");
      return;
    }
    this.OriginalPromise = Promise, this.patchPromise(), this.patchTimer(), this.isPatched = !0, console.log("[LogLens] Initialized - Auto trace propagation enabled");
  }
  static isEnabled() {
    return this.isPatched;
  }
  static async runAsync(t, e) {
    this.stack.push(t);
    try {
      return await e();
    } finally {
      this.stack.pop();
    }
  }
  static run(t, e) {
    this.stack.push(t);
    try {
      return e();
    } finally {
      this.stack.pop();
    }
  }
  static current() {
    return this.stack[this.stack.length - 1] || null;
  }
  static getTraceId() {
    return this.current()?.traceId || null;
  }
  static getDepth() {
    return this.stack.length;
  }
  static reset() {
    this.stack = [], this.isPatched = !1, globalThis.setTimeout = this.originalSetTimeout, globalThis.setInterval = this.originalSetInterval;
  }
  static patchPromise() {
    if (!this.OriginalPromise)
      throw new Error("[LogLens] OriginalPromise not initialized");
    const t = this.OriginalPromise, e = this;
    class n extends t {
      constructor(r) {
        const o = e.current();
        super((a, l) => {
          r(
            o ? (u) => e.run(o, () => a(u)) : a,
            o ? (u) => {
              u && typeof u == "object" && (u.__traceId = o.traceId), e.run(o, () => l(u));
            } : l
          );
        });
      }
      then(r, o) {
        const a = e.current();
        return super.then(
          r && a ? (l) => e.run(a, () => r(l)) : r ?? void 0,
          o && a ? (l) => e.run(a, () => o(l)) : o ?? void 0
        );
      }
    }
    globalThis.Promise = n, n.resolve = function(i) {
      return new n((r) => r(i));
    }, n.reject = function(i) {
      return new n((r, o) => o(i));
    }, n.all = function(i) {
      return new n((r, o) => {
        t.all(i).then(r, o);
      });
    }, n.race = function(i) {
      return new n((r, o) => {
        t.race(i).then(r, o);
      });
    }, n.allSettled = function(i) {
      return new n((r) => {
        t.allSettled(i).then(r);
      });
    }, t.any && (n.any = function(i) {
      return new n((r, o) => {
        t.any(i).then(r, o);
      });
    }), console.log("[LogLens] Promise patched - TraceId propagation enabled");
  }
  static patchTimer() {
    const t = this;
    this.originalSetTimeout = globalThis.setTimeout, this.originalSetInterval = globalThis.setInterval, globalThis.setTimeout = function(e, n, ...i) {
      const r = t.current(), o = function(...a) {
        if (r) {
          t.stack.push(r);
          try {
            return e(...a);
          } catch (l) {
            const u = l;
            throw u.__traceId = r.traceId, l;
          } finally {
            t.stack.pop();
          }
        }
        return e(...a);
      };
      return t.originalSetTimeout(o, n, ...i);
    }, globalThis.setInterval = function(e, n, ...i) {
      const r = t.current(), o = function(...a) {
        if (r) {
          t.stack.push(r);
          try {
            return e(...a);
          } catch (l) {
            const u = l;
            throw u.__traceId = r.traceId, l;
          } finally {
            t.stack.pop();
          }
        }
        return e(...a);
      };
      return t.originalSetInterval(o, n, ...i);
    }, console.log("[LogLens] Timers patched - TraceId propagation enabled");
  }
  static getStatus() {
    return {
      isPatched: this.isPatched,
      stackDepth: this.stack.length,
      currentTraceId: this.getTraceId()
    };
  }
}
class I {
  static COLORS = {
    INFO: "\x1B[30m",
    // black
    WARN: "\x1B[33m",
    // yellow
    ERROR: "\x1B[91m",
    // red
    RESET: "\x1B[0m",
    GRAY: "\x1B[90m",
    GREEN: "\x1B[32m",
    BOLD: "\x1B[1m"
  };
  static LEVEL_WIDTH = 5;
  static toConsole(t) {
    const e = Math.max(0, h.getDepth() - 1), n = "|".repeat(e), i = "→", r = this.COLORS[t.level], o = this.COLORS.RESET, a = this.COLORS.GRAY, l = `${r}[${t.level.padStart(
      this.LEVEL_WIDTH
    )}]${o}`, u = this.getDurationColor(
      t.executionTimeMs
    ), m = t.executionTimeMs !== null ? `${u}(${t.executionTimeMs.toString().padStart(5)}ms)${o}` : "", c = t.logger || t.message || "anonymous", d = t.message ? `${c}: ${a}${t.message}${o}` : c;
    return `${l} ${n}${i} ${d} ${m}`;
  }
  /**
   * Duration 기반 색상 선택
   */
  static getDurationColor(t) {
    return t === null ? this.COLORS.GRAY : t > 1e3 ? this.COLORS.ERROR : t > 100 ? this.COLORS.WARN : this.COLORS.GREEN;
  }
}
class g {
  static logs = [];
  static config = {
    maxLogs: 1e3,
    autoFlush: {
      enabled: !1,
      interval: 6e4,
      endpoint: ""
    }
  };
  static flushTimer = null;
  /**
   * 설정 초기화
   */
  static init(t) {
    t === null ? this.config = {
      maxLogs: 1e3,
      autoFlush: {
        enabled: !1,
        interval: 6e4,
        endpoint: ""
      }
    } : this.config = { ...this.config, ...t }, this.config.autoFlush?.enabled && this.startAutoFlush();
  }
  /**
   * 로그 추가
   */
  static addLog(t) {
    this.logs.push(t), console.log("[LogCollector] New log added:"), console.log(this.logs);
    const e = I.toConsole(t);
    console.log(e), this.logs.length > (this.config.maxLogs || 1e3) && this.logs.shift();
  }
  /**
   * 모든 로그 조회
   */
  static getLogs() {
    return [...this.logs];
  }
  /**
   * 특정 TraceID의 로그만 조회
   */
  static getLogsByTraceId(t) {
    return this.logs.filter((e) => e.traceId === t);
  }
  /**
   * 로그 전송
   */
  static async flush() {
    if (this.logs.length === 0)
      return;
    const t = this.config.autoFlush?.endpoint;
    if (!t) {
      console.warn("[LogCollector] No endpoint configured for flush");
      return;
    }
    const e = [...this.logs];
    this.logs = [];
    try {
      const n = await fetch(t, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ logs: e })
      });
      if (!n.ok)
        throw new Error(`Failed to send logs: ${n.status}`);
      console.log(`[LogCollector] Successfully sent ${e.length} logs`);
    } catch (n) {
      console.error("[LogCollector] Failed to flush logs:", n), this.logs.unshift(...e);
    }
  }
  /**
   * 로그 초기화
   */
  static clear() {
    this.logs = [];
  }
  /**
   * 자동 전송 시작
   */
  static startAutoFlush() {
    if (this.flushTimer)
      return;
    const t = this.config.autoFlush?.interval || 6e4;
    this.flushTimer = setInterval(() => {
      this.flush();
    }, t), console.log(`[LogCollector] Auto flush started (interval: ${t}ms)`);
  }
  /**
   * 자동 전송 중지
   */
  static stopAutoFlush() {
    this.flushTimer && (clearInterval(this.flushTimer), this.flushTimer = null, console.log("[LogCollector] Auto flush stopped"));
  }
  /**
   * 현재 상태 조회 (디버깅용)
   */
  static getStatus() {
    return {
      logCount: this.logs.length,
      maxLogs: this.config.maxLogs,
      autoFlushEnabled: this.config.autoFlush?.enabled || !1,
      endpoint: this.config.autoFlush?.endpoint || "not configured"
    };
  }
}
function C(s) {
  const t = s || w();
  return {
    info: (e, n) => {
      g.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "INFO",
        logger: t,
        message: e,
        layer: "FRONT",
        request: null,
        response: n || null,
        executionTimeMs: null
      });
    },
    warn: (e, n) => {
      g.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "WARN",
        logger: t,
        message: e,
        layer: "FRONT",
        request: null,
        response: n || null,
        executionTimeMs: null
      });
    },
    error: (e, n) => {
      g.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "ERROR",
        logger: t,
        message: n?.stack || e,
        layer: "FRONT",
        request: null,
        response: null,
        executionTimeMs: null
      });
    }
  };
}
function w() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let n = 3; n < Math.min(e.length, 6); n++) {
      const i = e[n];
      let r = i.match(/at\s+([A-Z]\w+)/);
      if (r || (r = i.match(/^([A-Z]\w+)@/)), r && r[1]) {
        const o = r[1];
        if (![
          "useLogger",
          "extractComponentName",
          "renderWithHooks",
          "updateFunctionComponent",
          "beginWork",
          "performUnitOfWork",
          "workLoop",
          "Object",
          "Module",
          "mountMemo",
          "useMemo"
        ].includes(o))
          return o;
      }
    }
    return "anonymous";
  } catch {
    return "anonymous";
  }
}
class O {
  static isInitialized = !1;
  static errorHandler = null;
  static rejectionHandler = null;
  static init() {
    if (this.isInitialized) {
      console.warn("[LogLens] ErrorCapture already initialized");
      return;
    }
    this.errorHandler = (t) => {
      const n = t.error?.__traceId || h.getTraceId(), i = {
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: n,
        level: "ERROR",
        logger: "ErrorCapture",
        message: `Uncaught error: ${t.error?.message || t.message}
${t.error?.stack || ""}`,
        layer: "FRONT",
        request: null,
        response: {
          filename: t.filename,
          lineno: t.lineno,
          colno: t.colno
        },
        executionTimeMs: null
      };
      g.addLog(i);
    }, this.rejectionHandler = (t) => {
      const n = t.reason?.__traceId || h.getTraceId(), i = {
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: n,
        level: "ERROR",
        logger: "ErrorCapture",
        message: `Unhandled promise rejection: ${t.reason?.message || t.reason}
${t.reason?.stack || ""}`,
        layer: "FRONT",
        request: null,
        response: {
          reason: t.reason
        },
        executionTimeMs: null
      };
      g.addLog(i);
    }, window.addEventListener("error", this.errorHandler), window.addEventListener("unhandledrejection", this.rejectionHandler), this.isInitialized = !0, console.log("[LogLens] ErrorCapture initialized");
  }
  static isEnabled() {
    return this.isInitialized;
  }
  static reset() {
    this.errorHandler && (window.removeEventListener("error", this.errorHandler), this.errorHandler = null), this.rejectionHandler && (window.removeEventListener("unhandledrejection", this.rejectionHandler), this.rejectionHandler = null), this.isInitialized = !1;
  }
}
const F = {
  info: (s, t) => {
    const e = h.getTraceId();
    g.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "INFO",
      logger: "loglens",
      message: s,
      layer: "FRONT",
      request: null,
      response: t || null,
      executionTimeMs: null
    });
  },
  warn: (s, t) => {
    const e = h.getTraceId();
    g.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "WARN",
      logger: "loglens",
      message: s,
      layer: "FRONT",
      request: null,
      response: t || null,
      executionTimeMs: null
    });
  },
  error: (s, t) => {
    const e = h.getTraceId();
    g.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "ERROR",
      logger: "loglens",
      message: t?.stack || s,
      layer: "FRONT",
      request: null,
      response: null,
      executionTimeMs: null
    });
  }
};
function S() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let n = 3; n < Math.min(e.length, 8); n++) {
      const i = e[n];
      let r = i.match(/at\s+(?:Object\.)?(\w+)\s*\(/) || i.match(/at\s+(\w+)\s/) || i.match(/^(\w+)@/);
      if (r && r[1]) {
        const o = r[1];
        if (![
          "withLogLens",
          "extractFunctionName",
          "runAsync",
          "run",
          "Object",
          "Module",
          "eval",
          "anonymous",
          "Function",
          "mountMemo",
          "useMemo"
        ].includes(o))
          return o;
      }
    }
    return "anonymous";
  } catch {
    return "anonymous";
  }
}
function L(s, t, e, n) {
  return {
    "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
    traceId: s,
    level: e,
    logger: t,
    message: n,
    layer: "FRONT"
  };
}
function R(s, t, e) {
  g.addLog({
    ...L(s, t, "INFO", `${t} called`),
    request: {
      method: t,
      parameters: e.length > 0 ? e : void 0
    },
    response: null,
    executionTimeMs: null
  });
}
function p(s, t, e, n) {
  g.addLog({
    ...L(
      s,
      t,
      "INFO",
      `${t} completed`
    ),
    request: {
      method: t
    },
    response: e,
    executionTimeMs: n
  });
}
function f(s, t, e, n) {
  g.addLog({
    ...L(
      s,
      t,
      "ERROR",
      e?.stack || e?.message || String(e)
    ),
    request: {
      method: t
    },
    response: null,
    executionTimeMs: n
  });
}
function y(s, t) {
  const e = t?.logger || (s.name && s.name !== "anonymous" ? s.name : null) || S(), n = s.constructor.name === "AsyncFunction";
  return ((...i) => {
    const r = crypto.randomUUID(), o = !t?.skipLog, a = Date.now();
    if (o && R(r, e, i), n)
      return h.runAsync({ traceId: r }, async () => {
        try {
          const c = await s(...i), d = Date.now() - a;
          return o && p(r, e, c, d), c;
        } catch (c) {
          const d = Date.now() - a;
          throw f(r, e, c, d), c;
        }
      });
    let l, u = null;
    if (h.run({ traceId: r }, () => {
      try {
        l = s(...i);
      } catch (c) {
        u = c;
      }
    }), u) {
      const c = Date.now() - a;
      throw f(r, e, u, c), u;
    }
    if (l && typeof l.then == "function")
      return l.then((c) => {
        const d = Date.now() - a;
        return o && p(r, e, c, d), c;
      }).catch((c) => {
        const d = Date.now() - a;
        throw f(r, e, c, d), c;
      });
    const m = Date.now() - a;
    return o && p(r, e, l, m), l;
  });
}
function E(s, t, e = []) {
  return T(
    () => y(s, t),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    e
  );
}
const k = (s) => {
  h.init(), g.init(s?.logCollector || null), s?.captureErrors && O.init();
};
export {
  O as ErrorCapture,
  h as LightZone,
  k as initLogLens,
  F as loglens,
  E as useLogLens,
  C as useLogger,
  y as withLogLens
};
