import { useMemo as w } from "react";
class h {
  static stack = [];
  static isPatched = !1;
  static OriginalPromise = null;
  static originalSetTimeout;
  static originalSetInterval;
  static isProduction = !1;
  static init(t = !1) {
    if (this.isProduction = t, this.isPatched) {
      this.isProduction || console.warn("[LogLens] Already initialized");
      return;
    }
    this.OriginalPromise = Promise, this.patchPromise(), this.patchTimer(), this.isPatched = !0, this.isProduction || console.log("[LogLens] Initialized - Auto trace propagation enabled");
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
    this.stack = [], this.isPatched = !1, this.originalSetTimeout && (globalThis.setTimeout = this.originalSetTimeout), this.originalSetInterval && (globalThis.setInterval = this.originalSetInterval);
  }
  static patchPromise() {
    if (!this.OriginalPromise)
      throw new Error("[LogLens] OriginalPromise not initialized");
    const t = this.OriginalPromise, e = this;
    class s extends t {
      constructor(n) {
        const r = e.current();
        super((a, l) => {
          n(
            r ? (u) => e.run(r, () => a(u)) : a,
            r ? (u) => {
              u && typeof u == "object" && (u.__traceId = r.traceId), e.run(r, () => l(u));
            } : l
          );
        });
      }
      then(n, r) {
        const a = e.current();
        return super.then(
          n && a ? (l) => e.run(a, () => n(l)) : n ?? void 0,
          r && a ? (l) => e.run(a, () => r(l)) : r ?? void 0
        );
      }
    }
    globalThis.Promise = s, s.resolve = function(i) {
      return new s((n) => n(i));
    }, s.reject = function(i) {
      return new s((n, r) => r(i));
    }, s.all = function(i) {
      return new s((n, r) => {
        t.all(i).then(n, r);
      });
    }, s.race = function(i) {
      return new s((n, r) => {
        t.race(i).then(n, r);
      });
    }, s.allSettled = function(i) {
      return new s((n) => {
        t.allSettled(i).then(n);
      });
    }, t.any && (s.any = function(i) {
      return new s((n, r) => {
        t.any(i).then(n, r);
      });
    }), this.isProduction || console.log("[LogLens] Promise patched - TraceId propagation enabled");
  }
  static patchTimer() {
    const t = this;
    this.originalSetTimeout = globalThis.setTimeout.bind(globalThis), this.originalSetInterval = globalThis.setInterval.bind(globalThis), globalThis.setTimeout = function(e, s, ...i) {
      const n = t.current(), r = function(...a) {
        if (n) {
          t.stack.push(n);
          try {
            return e(...a);
          } catch (l) {
            const u = l;
            throw u.__traceId = n.traceId, l;
          } finally {
            t.stack.pop();
          }
        }
        return e(...a);
      };
      return t.originalSetTimeout(r, s, ...i);
    }, globalThis.setInterval = function(e, s, ...i) {
      const n = t.current(), r = function(...a) {
        if (n) {
          t.stack.push(n);
          try {
            return e(...a);
          } catch (l) {
            const u = l;
            throw u.__traceId = n.traceId, l;
          } finally {
            t.stack.pop();
          }
        }
        return e(...a);
      };
      return t.originalSetInterval(r, s, ...i);
    }, this.isProduction || console.log("[LogLens] Timers patched - TraceId propagation enabled");
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
    const e = Math.max(0, h.getDepth() - 1), s = "|".repeat(e), i = "→", n = this.COLORS[t.level], r = this.COLORS.RESET, a = this.COLORS.GRAY, l = `${n}[${t.level.padStart(
      this.LEVEL_WIDTH
    )}]${r}`, u = this.getDurationColor(
      t.executionTimeMs
    ), p = t.executionTimeMs !== null ? `${u}(${t.executionTimeMs.toString().padStart(5)}ms)${r}` : "", c = t.logger || t.message || "anonymous", g = t.message ? `${c}: ${a}${t.message}${r}` : c;
    return `${l} ${s}${i} ${g} ${p}`;
  }
  /**
   * Duration 기반 색상 선택
   */
  static getDurationColor(t) {
    return t === null ? this.COLORS.GRAY : t > 1e3 ? this.COLORS.ERROR : t > 100 ? this.COLORS.WARN : this.COLORS.GREEN;
  }
}
const m = {
  info: (o, t) => {
    const e = h.getTraceId();
    d.addLog({
      timestamp: (/* @__PURE__ */ new Date()).toISOString(),
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
    const e = h.getTraceId();
    d.addLog({
      timestamp: (/* @__PURE__ */ new Date()).toISOString(),
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
    const e = h.getTraceId();
    d.addLog({
      timestamp: (/* @__PURE__ */ new Date()).toISOString(),
      traceId: e || null,
      level: "ERROR",
      logger: "loglens",
      message: t?.stack || o,
      layer: "FRONT",
      request: null,
      response: null,
      executionTimeMs: null
    });
  },
  // 추가
  send: async () => d.send(),
  flush: async () => d.flush(),
  getLogs: () => d.getLogs(),
  clear: () => {
    d.clear();
  }
};
async function E() {
  return (await (await fetch("https://api.ipify.org?format=json")).json()).ip || null;
}
class f {
  static instance;
  // 기본 민감 필드 패턴 (키 이름 기반)
  static DEFAULT_SENSITIVE_KEY_PATTERNS = [
    /^.*(password|passwd|pwd|pass|pw|secret).*$/i,
    /^.*(token|auth|authorization|bearer).*$/i,
    /^.*(api[_-]?key|access[_-]?key|secret[_-]?key).*$/i,
    /^.*(번호|number|no|ssn|security).*$/i,
    /^.*(card|cvv|cvc|pin).*$/i
  ];
  // 기본 민감 값 패턴 (값 자체 기반)
  static DEFAULT_SENSITIVE_VALUE_PATTERNS = [
    /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
    // 이메일
    /^\d{6}-[1-4]\d{6}$/,
    // 주민등록번호
    /^\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}$/,
    // 카드번호
    /^(\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}$/,
    // 전화번호
    /^\d{10,14}$/
    // 계좌번호 (10-14자리)
  ];
  sensitiveKeyPatterns = [];
  sensitiveValuePatterns = [];
  excludePatterns = [];
  constructor() {
    this.sensitiveKeyPatterns = [...f.DEFAULT_SENSITIVE_KEY_PATTERNS], this.sensitiveValuePatterns = [
      ...f.DEFAULT_SENSITIVE_VALUE_PATTERNS
    ];
  }
  /**
   * Singleton
   */
  static getInstance() {
    return this.instance || (this.instance = new f()), this.instance;
  }
  /**
   * 초기화 (환경변수 + 설정 파일)
   */
  static async initialize(t) {
    const e = f.getInstance();
    e.loadFromEnv(), t && e.loadFromConfig(t);
    const s = typeof process < "u" ? process.env?.DATA_MASK_CONFIG : void 0;
    return s && await e.loadFromFile(s), e;
  }
  /**
   * 환경변수에서 패턴 로드
   */
  loadFromEnv() {
    if (typeof process > "u") return;
    const t = process.env?.DATA_MASK_SENSITIVE;
    t && t.split(",").forEach((s) => {
      const i = s.trim();
      i && this.sensitiveKeyPatterns.push(new RegExp(`^${i}$`, "i"));
    });
    const e = process.env?.DATA_MASK_EXCLUDE;
    e && e.split(",").forEach((s) => {
      const i = s.trim();
      i && this.excludePatterns.push(new RegExp(`^${i}$`, "i"));
    });
  }
  /**
   * 설정 객체에서 로드
   */
  loadFromConfig(t) {
    t.sensitive && t.sensitive.forEach((e) => {
      this.sensitiveKeyPatterns.push(new RegExp(`^${e}$`, "i"));
    }), t.sensitivePatterns && t.sensitivePatterns.forEach((e) => {
      this.sensitiveKeyPatterns.push(new RegExp(e, "i"));
    }), t.exclude && t.exclude.forEach((e) => {
      this.excludePatterns.push(new RegExp(`^${e}$`, "i"));
    }), t.excludePatterns && t.excludePatterns.forEach((e) => {
      this.excludePatterns.push(new RegExp(e, "i"));
    });
  }
  /**
   * 설정 파일에서 로드 (Node.js 환경)
   */
  async loadFromFile(t) {
    try {
      if (typeof require < "u") {
        const s = await require("fs").promises.readFile(t, "utf-8"), i = JSON.parse(s);
        this.loadFromConfig(i), console.log(`[DataMasker] Loaded config from ${t}`);
      }
    } catch {
    }
  }
  /**
   * 객체 마스킹
   */
  mask(t) {
    return this.maskRecursive(t, /* @__PURE__ */ new Set());
  }
  /**
   * JSON 문자열로 마스킹
   */
  maskToJson(t) {
    return JSON.stringify(this.mask(t));
  }
  /**
   * 재귀적으로 마스킹 처리
   */
  maskRecursive(t, e) {
    if (t == null) return t;
    if (typeof t == "object" && e.has(t))
      return "[Circular]";
    if (typeof t != "object")
      return typeof t == "string" && this.isSensitiveValue(t) ? this.maskValue(t) : t;
    if (e.add(t), Array.isArray(t))
      return t.map((i) => this.maskRecursive(i, e));
    const s = {};
    for (const i in t) {
      if (!Object.prototype.hasOwnProperty.call(t, i)) continue;
      const n = t[i];
      this.shouldExclude(i) ? s[i] = "<excluded>" : this.isSensitiveKey(i) ? s[i] = "****" : typeof n == "string" && this.isSensitiveValue(n) ? s[i] = this.maskValue(n) : typeof n == "object" && n !== null ? s[i] = this.maskRecursive(n, e) : s[i] = n;
    }
    return s;
  }
  /**
   * 키가 민감한지 체크
   */
  isSensitiveKey(t) {
    return this.sensitiveKeyPatterns.some((e) => e.test(t));
  }
  /**
   * 값이 민감한지 체크
   */
  isSensitiveValue(t) {
    return t.length < 6 ? !1 : this.sensitiveValuePatterns.some((e) => e.test(t));
  }
  /**
   * 제외할 키인지 체크
   */
  shouldExclude(t) {
    return this.excludePatterns.some((e) => e.test(t));
  }
  /**
   * 값 마스킹 처리
   */
  maskValue(t) {
    if (/@/.test(t) && t.includes(".")) {
      const [e, s] = t.split("@");
      if (e.length > 0)
        return `${e[0]}***@${s}`;
    }
    if (/^\d{6}-[1-4]\d{6}$/.test(t))
      return t.substring(0, 2) + "****-*******";
    if (/^\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}$/.test(t))
      return t.replace(/\d(?=[\d\s-]*\d{4}$)/g, "*");
    if (/^(\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}$/.test(
      t
    )) {
      const e = t.replace(/\D/g, "");
      if (e.length >= 8)
        return "*".repeat(e.length - 4) + e.slice(-4);
    }
    return "****";
  }
  /**
   * 런타임에 패턴 추가
   */
  addSensitiveKey(t) {
    const e = t instanceof RegExp ? t : new RegExp(`^${t}$`, "i");
    return this.sensitiveKeyPatterns.push(e), this;
  }
  /**
   * 런타임에 제외 패턴 추가
   */
  addExclude(t) {
    const e = t instanceof RegExp ? t : new RegExp(`^${t}$`, "i");
    return this.excludePatterns.push(e), this;
  }
  /**
   * 현재 설정 상태 조회 (디버깅용)
   */
  getStatus() {
    return {
      sensitiveKeyPatterns: this.sensitiveKeyPatterns.length,
      sensitiveValuePatterns: this.sensitiveValuePatterns.length,
      excludePatterns: this.excludePatterns.length
    };
  }
}
class d {
  static masker;
  static logs = [];
  static config = {
    maxLogs: 1e3,
    autoFlush: {
      enabled: !0,
      interval: 3e4,
      endpoint: ""
    },
    isProduction: !1
  };
  static flushTimer = null;
  static isSending = !1;
  // 전송 중 상태 플래그
  /**
   * 설정 초기화
   */
  static async init(t) {
    this.masker = await f.initialize(), t === null ? this.config = {
      maxLogs: 1e3,
      autoFlush: {
        enabled: !1,
        interval: 3e4,
        endpoint: ""
      }
    } : this.config = { ...this.config, ...t }, this.config.autoFlush?.enabled && this.startAutoFlush();
  }
  /**
   * 로그 추가
   */
  static addLog(t) {
    if (this.logs.push(t), this.config.isProduction)
      t.level === "ERROR" && t.logger !== "ErrorCapture" && (console.error("[LogLens Error]", t.message), t.response && console.error("Context:", t.response));
    else {
      console.log("[LogCollector] New log added:");
      const e = I.toConsole(t);
      console.log(e);
    }
    this.logs.length > (this.config.maxLogs || 1e3) && this.logs.shift();
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
    if (!this.config.autoFlush?.enabled) {
      m.warn("[LogCollector] Auto flush is disabled");
      return;
    }
    const t = this.config.autoFlush?.endpoint;
    if (!t) {
      m.warn("[LogCollector] No endpoint configured for flush");
      return;
    }
    await this.sendLogs(t);
  }
  /**
   * 수동 전송 (설정 무시하고 강제 전송)
   */
  static async send() {
    if (this.logs.length === 0) {
      m.warn("[LogCollector] No logs to send");
      return;
    }
    const t = this.config.autoFlush?.endpoint;
    if (!t) {
      m.warn("[LogCollector] No endpoint provided for manual send");
      return;
    }
    await this.sendLogs(t);
  }
  /**
   * 로그 전송 로직
   */
  static async sendLogs(t) {
    if (this.isSending) {
      this.config.isProduction || console.warn("[LogCollector] Already sending logs");
      return;
    }
    this.isSending = !0;
    const e = [...this.logs];
    this.logs = [];
    const s = e.map((n) => this.masker.mask(n)), i = await E();
    try {
      const n = await fetch(t, {
        method: "POST",
        headers: {
          "LogLens-IP": i || "",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ logs: s })
      });
      if (!n.ok)
        throw new Error(`Failed to send logs: ${n.status}`);
      this.config.isProduction || console.log(
        `[LogCollector] Successfully sent ${e.length} logs`
      );
    } catch (n) {
      console.error("[LogCollector] Failed to send logs:", n), this.logs.unshift(...e);
    } finally {
      this.isSending = !1;
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
    }, t), this.config.isProduction || console.log(
      `[LogCollector] Auto flush started (interval: ${t}ms)`
    );
  }
  /**
   * 자동 전송 중지
   */
  static stopAutoFlush() {
    this.flushTimer && (clearInterval(this.flushTimer), this.flushTimer = null, this.config.isProduction || console.log("[LogCollector] Auto flush stopped"));
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
class S {
  static isInitialized = !1;
  static errorHandler = null;
  static rejectionHandler = null;
  static isProduction = !1;
  static init(t = !1) {
    if (this.isProduction = t, this.isInitialized) {
      this.isProduction || console.warn("[LogLens] ErrorCapture already initialized");
      return;
    }
    this.errorHandler = (e) => {
      const i = e.error?.__traceId || h.getTraceId(), n = {
        timestamp: (/* @__PURE__ */ new Date()).toISOString(),
        traceId: i,
        level: "ERROR",
        logger: "ErrorCapture",
        message: `Uncaught error: ${e.error?.message || e.message}
${e.error?.stack || ""}`,
        layer: "FRONT",
        request: null,
        response: {
          filename: e.filename,
          lineno: e.lineno,
          colno: e.colno
        },
        executionTimeMs: null
      };
      d.addLog(n);
    }, this.rejectionHandler = (e) => {
      const i = e.reason?.__traceId || h.getTraceId(), n = {
        timestamp: (/* @__PURE__ */ new Date()).toISOString(),
        traceId: i,
        level: "ERROR",
        logger: "ErrorCapture",
        message: `Unhandled promise rejection: ${e.reason?.message || e.reason}
${e.reason?.stack || ""}`,
        layer: "FRONT",
        request: null,
        response: {
          reason: e.reason
        },
        executionTimeMs: null
      };
      d.addLog(n);
    }, window.addEventListener("error", this.errorHandler), window.addEventListener("unhandledrejection", this.rejectionHandler), this.isInitialized = !0, this.isProduction || console.log("[LogLens] ErrorCapture initialized");
  }
  static isEnabled() {
    return this.isInitialized;
  }
  static reset() {
    this.errorHandler && (window.removeEventListener("error", this.errorHandler), this.errorHandler = null), this.rejectionHandler && (window.removeEventListener("unhandledrejection", this.rejectionHandler), this.rejectionHandler = null), this.isInitialized = !1;
  }
}
function P() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let s = 3; s < Math.min(e.length, 8); s++) {
      const i = e[s];
      let n = i.match(/at\s+(?:Object\.)?(\w+)\s*\(/) || i.match(/at\s+(\w+)\s/) || i.match(/^(\w+)@/);
      if (n && n[1]) {
        const r = n[1];
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
function y(o, t, e, s) {
  return {
    timestamp: (/* @__PURE__ */ new Date()).toISOString(),
    traceId: o,
    level: e,
    logger: t,
    message: s,
    layer: "FRONT"
  };
}
function x(o, t, e) {
  d.addLog({
    ...y(o, t, "INFO", `${t} called`),
    request: {
      method: t,
      parameters: e.length > 0 ? e : void 0
    },
    response: null,
    executionTimeMs: null
  });
}
function L(o, t, e, s) {
  d.addLog({
    ...y(
      o,
      t,
      "INFO",
      `${t} completed`
    ),
    request: {
      method: t
    },
    response: e,
    executionTimeMs: s
  });
}
function T(o, t, e, s) {
  d.addLog({
    ...y(
      o,
      t,
      "ERROR",
      e?.stack || e?.message || String(e)
    ),
    request: {
      method: t
    },
    response: null,
    executionTimeMs: s
  });
}
function R(o, t) {
  const e = t?.logger || (o.name && o.name !== "anonymous" ? o.name : null) || P(), s = o.constructor.name === "AsyncFunction";
  return ((...i) => {
    const n = crypto.randomUUID(), r = !t?.skipLog, a = Date.now();
    if (r && x(n, e, i), s)
      return h.runAsync({ traceId: n }, async () => {
        try {
          const c = await o(...i), g = Date.now() - a;
          return r && L(n, e, c, g), c;
        } catch (c) {
          const g = Date.now() - a;
          throw T(n, e, c, g), c;
        }
      });
    let l, u = null;
    if (h.run({ traceId: n }, () => {
      try {
        l = o(...i);
      } catch (c) {
        u = c;
      }
    }), u) {
      const c = Date.now() - a;
      throw T(n, e, u, c), u;
    }
    if (l && typeof l.then == "function")
      return l.then((c) => {
        const g = Date.now() - a;
        return r && L(n, e, c, g), c;
      }).catch((c) => {
        const g = Date.now() - a;
        throw T(n, e, c, g), c;
      });
    const p = Date.now() - a;
    return r && L(n, e, l, p), l;
  });
}
function $(o) {
  const t = o || O();
  return {
    info: (e, s) => {
      d.addLog({
        timestamp: (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "INFO",
        logger: t,
        message: e,
        layer: "FRONT",
        request: null,
        response: s || null,
        executionTimeMs: null
      });
    },
    warn: (e, s) => {
      d.addLog({
        timestamp: (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "WARN",
        logger: t,
        message: e,
        layer: "FRONT",
        request: null,
        response: s || null,
        executionTimeMs: null
      });
    },
    error: (e, s) => {
      d.addLog({
        timestamp: (/* @__PURE__ */ new Date()).toISOString(),
        traceId: h.getTraceId() || null,
        level: "ERROR",
        logger: t,
        message: s?.stack || e,
        layer: "FRONT",
        request: null,
        response: null,
        executionTimeMs: null
      });
    }
  };
}
function O() {
  try {
    const t = new Error().stack;
    if (!t) return "anonymous";
    const e = t.split(`
`);
    for (let s = 3; s < Math.min(e.length, 6); s++) {
      const i = e[s];
      let n = i.match(/at\s+([A-Z]\w+)/);
      if (n || (n = i.match(/^([A-Z]\w+)@/)), n && n[1]) {
        const r = n[1];
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
function k(o, t, e = []) {
  return w(
    () => R(o, t),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    e
  );
}
const v = {
  maxLogs: 1e3,
  autoFlushInterval: 6e4,
  autoFlushEnabled: !0,
  captureErrors: !1,
  isProduction: !1
}, C = (o) => {
  const t = {
    ...v,
    ...o
  };
  h.init(t.isProduction);
  const e = t.domain.trim(), i = `${e.endsWith("/") ? e.slice(0, -1) : e}/api/logs/frontend`;
  t.isProduction || console.log("[LogLens] Log collection enabled. Endpoint:", i), d.init({
    maxLogs: t.maxLogs,
    autoFlush: {
      enabled: t.autoFlushEnabled,
      interval: t.autoFlushInterval,
      endpoint: i
    },
    isProduction: t.isProduction
  }), t.captureErrors && S.init(t.isProduction);
};
export {
  C as initLogLens,
  m as loglens,
  k as useLogLens,
  $ as useLogger,
  R as withLogLens
};
