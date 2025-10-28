import { useMemo as O } from "react";
function x(o, t) {
  const e = String(t.name);
  return function(...n) {
    console.log(`[LOG] ${e}`, n);
    const i = o.apply(this, n);
    return console.log(`[RESULT] ${e}`, i), i;
  };
}
class g {
  static stack = [];
  static isPatched = !1;
  static OriginalPromise = null;
  static init() {
    if (this.isPatched) {
      console.warn("[LogLens] Already initialized");
      return;
    }
    this.OriginalPromise = Promise, this.patchPromise(), this.isPatched = !0, console.log("[LogLens] Initialized - Auto trace propagation enabled");
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
    this.stack = [], this.isPatched = !1;
  }
  static patchPromise() {
    if (!this.OriginalPromise)
      throw new Error("[LogLens] OriginalPromise not initialized");
    const t = this.OriginalPromise, e = this;
    class n extends t {
      constructor(s) {
        const r = e.current();
        super((a, c) => {
          s(
            (u) => {
              if (r && e.current() !== r) {
                e.stack.push(r);
                try {
                  a(u);
                } finally {
                  e.stack.pop();
                }
              } else
                a(u);
            },
            (u) => {
              if (r && e.current() !== r) {
                e.stack.push(r);
                try {
                  c(u);
                } finally {
                  e.stack.pop();
                }
              } else
                c(u);
            }
          );
        });
      }
      then(s, r) {
        const a = e.current();
        return super.then(
          s ? (c) => {
            if (a && e.current() !== a) {
              e.stack.push(a);
              try {
                return s(c);
              } finally {
                e.stack.pop();
              }
            }
            return s(c);
          } : void 0,
          r ? (c) => {
            if (a && e.current() !== a) {
              e.stack.push(a);
              try {
                return r(c);
              } finally {
                e.stack.pop();
              }
            }
            return r(c);
          } : void 0
        );
      }
    }
    globalThis.Promise = n, n.resolve = function(i) {
      return new n((s) => s(i));
    }, n.reject = function(i) {
      return new n((s, r) => r(i));
    }, n.all = function(i) {
      return new n((s, r) => {
        t.all(i).then(s, r);
      });
    }, n.race = function(i) {
      return new n((s, r) => {
        t.race(i).then(s, r);
      });
    }, n.allSettled = function(i) {
      return new n((s) => {
        t.allSettled(i).then(s);
      });
    }, t.any && (n.any = function(i) {
      return new n((s, r) => {
        t.any(i).then(s, r);
      });
    }), console.log("[LogLens] Promise patched - TraceId auto-propagation active");
  }
  static getStatus() {
    return {
      isPatched: this.isPatched,
      stackDepth: this.stack.length,
      currentTraceId: this.getTraceId()
    };
  }
}
class y {
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
    const e = Math.max(0, g.getDepth() - 1), n = "|".repeat(e), i = "→", s = this.COLORS[t.level], r = this.COLORS.RESET, a = this.COLORS.GRAY, c = `${s}[${t.level.padStart(
      this.LEVEL_WIDTH
    )}]${r}`, u = this.getDurationColor(
      t.executionTimeMs
    ), m = t.executionTimeMs !== null ? `${u}(${t.executionTimeMs.toString().padStart(5)}ms)${r}` : "", l = t.logger || t.message || "anonymous", h = t.message ? `${l}: ${a}${t.message}${r}` : l;
    return `${c} ${n}${i} ${h} ${m}`;
  }
  /**
   * Duration 기반 색상 선택
   */
  static getDurationColor(t) {
    return t === null ? this.COLORS.GRAY : t > 1e3 ? this.COLORS.ERROR : t > 100 ? this.COLORS.WARN : this.COLORS.GREEN;
  }
}
class d {
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
    this.logs.push(t);
    const e = y.toConsole(t);
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
function T() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let n = 3; n < Math.min(e.length, 8); n++) {
      const i = e[n];
      let s = i.match(/at\s+(?:Object\.)?(\w+)\s*\(/) || i.match(/at\s+(\w+)\s/) || i.match(/^(\w+)@/);
      if (s && s[1]) {
        const r = s[1];
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
        ].includes(r))
          return r;
      }
    }
    return "anonymous";
  } catch {
    return "anonymous";
  }
}
function L(o, t, e, n) {
  return {
    "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
    traceId: o,
    level: e,
    logger: t,
    message: n,
    layer: "FRONT"
  };
}
function w(o, t, e) {
  d.addLog({
    ...L(o, t, "INFO", `${t} called`),
    request: {
      method: t,
      parameters: e.length > 0 ? e : void 0
    },
    response: null,
    executionTimeMs: null
  });
}
function f(o, t, e, n) {
  d.addLog({
    ...L(
      o,
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
function p(o, t, e, n) {
  d.addLog({
    ...L(
      o,
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
function R(o, t) {
  const e = t?.logger || (o.name && o.name !== "anonymous" ? o.name : null) || T(), n = o.constructor.name === "AsyncFunction";
  return ((...i) => {
    const s = crypto.randomUUID(), r = !t?.skipLog, a = Date.now();
    if (r && w(s, e, i), n)
      return g.runAsync({ traceId: s }, async () => {
        try {
          const l = await o(...i), h = Date.now() - a;
          return r && f(s, e, l, h), l;
        } catch (l) {
          const h = Date.now() - a;
          throw p(s, e, l, h), l;
        }
      });
    let c, u = null;
    if (g.run({ traceId: s }, () => {
      try {
        c = o(...i);
      } catch (l) {
        u = l;
      }
    }), u) {
      const l = Date.now() - a;
      throw p(s, e, u, l), u;
    }
    if (c && typeof c.then == "function")
      return c.then((l) => {
        const h = Date.now() - a;
        return r && f(s, e, l, h), l;
      }).catch((l) => {
        const h = Date.now() - a;
        throw p(s, e, l, h), l;
      });
    const m = Date.now() - a;
    return r && f(s, e, c, m), c;
  });
}
function k(o, t, e = []) {
  return O(
    () => R(o, t),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    e
  );
}
const F = {
  info: (o, t) => {
    const e = g.getTraceId();
    d.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "INFO",
      logger: "loglens",
      message: o,
      layer: "FRONT",
      request: null,
      response: t || null,
      executionTimeMs: null
    });
  },
  warn: (o, t) => {
    const e = g.getTraceId();
    d.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "WARN",
      logger: "loglens",
      message: o,
      layer: "FRONT",
      request: null,
      response: t || null,
      executionTimeMs: null
    });
  },
  error: (o, t) => {
    const e = g.getTraceId();
    d.addLog({
      "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "ERROR",
      logger: "loglens",
      message: t?.stack || o,
      layer: "FRONT",
      request: null,
      response: null,
      executionTimeMs: null
    });
  }
};
function C(o) {
  const t = o || I();
  return {
    info: (e, n) => {
      d.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: g.getTraceId() || null,
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
      d.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: g.getTraceId() || null,
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
      d.addLog({
        "@timestamp": (/* @__PURE__ */ new Date()).toISOString(),
        traceId: g.getTraceId() || null,
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
function I() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let n = 3; n < Math.min(e.length, 6); n++) {
      const i = e[n];
      let s = i.match(/at\s+([A-Z]\w+)/);
      if (s || (s = i.match(/^([A-Z]\w+)@/)), s && s[1]) {
        const r = s[1];
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
        ].includes(r))
          return r;
      }
    }
    return "anonymous";
  } catch {
    return "anonymous";
  }
}
export {
  g as LightZone,
  x as Log,
  d as LogCollector,
  y as LogFormatter,
  F as loglens,
  k as useLogLens,
  C as useLogger,
  R as withLogLens
};
