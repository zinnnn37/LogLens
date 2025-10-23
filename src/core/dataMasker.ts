// src/core/dataMasker.ts

import type { MaskConfig } from './types';

/**
 * 데이터 마스킹 처리 클래스
 * - 민감한 필드를 자동으로 감지하여 마스킹
 * - 환경변수 또는 설정 파일로 추가 패턴 지정 가능
 */
class DataMasker {
  private static instance: DataMasker;

  // 기본 민감 필드 패턴 (키 이름 기반)
  private static readonly DEFAULT_SENSITIVE_KEY_PATTERNS: RegExp[] = [
    /^.*(password|passwd|pwd|pass|pw|secret).*$/i,
    /^.*(token|auth|authorization|bearer).*$/i,
    /^.*(api[_-]?key|access[_-]?key|secret[_-]?key).*$/i,
    /^.*(번호|number|no|ssn|security).*$/i,
    /^.*(card|cvv|cvc|pin).*$/i,
  ];

  // 기본 민감 값 패턴 (값 자체 기반)
  private static readonly DEFAULT_SENSITIVE_VALUE_PATTERNS: RegExp[] = [
    /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, // 이메일
    /^\d{6}-[1-4]\d{6}$/, // 주민등록번호
    /^\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}$/, // 카드번호
    /^(\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}$/, // 전화번호
    /^\d{10,14}$/, // 계좌번호 (10-14자리)
  ];

  private sensitiveKeyPatterns: RegExp[] = [];
  private sensitiveValuePatterns: RegExp[] = [];
  private excludePatterns: RegExp[] = [];

  private constructor() {
    this.sensitiveKeyPatterns = [...DataMasker.DEFAULT_SENSITIVE_KEY_PATTERNS];
    this.sensitiveValuePatterns = [
      ...DataMasker.DEFAULT_SENSITIVE_VALUE_PATTERNS,
    ];
  }

  /**
   * Singleton
   */
  static getInstance(): DataMasker {
    if (!this.instance) {
      this.instance = new DataMasker();
    }
    return this.instance;
  }

  /**
   * 초기화 (환경변수 + 설정 파일)
   */
  static async initialize(config?: MaskConfig): Promise<DataMasker> {
    const masker = DataMasker.getInstance();

    // 1. 환경변수에서 로드
    masker.loadFromEnv();

    // 2. 설정 객체에서 로드
    if (config) {
      masker.loadFromConfig(config);
    }

    // 3. 설정 파일에서 로드 (선택적)
    const configPath =
      typeof process !== 'undefined'
        ? process.env?.DATA_MASK_CONFIG
        : undefined;

    if (configPath) {
      await masker.loadFromFile(configPath);
    }

    return masker;
  }

  /**
   * 환경변수에서 패턴 로드
   */
  private loadFromEnv(): void {
    // Node.js 환경이 아니면 무시
    if (typeof process === 'undefined') return;

    const sensitive = process.env?.DATA_MASK_SENSITIVE;
    if (sensitive) {
      sensitive.split(',').forEach((field) => {
        const trimmed = field.trim();
        if (trimmed) {
          this.sensitiveKeyPatterns.push(new RegExp(`^${trimmed}$`, 'i'));
        }
      });
    }

    // 제외 필드
    const exclude = process.env?.DATA_MASK_EXCLUDE;
    if (exclude) {
      exclude.split(',').forEach((field) => {
        const trimmed = field.trim();
        if (trimmed) {
          this.excludePatterns.push(new RegExp(`^${trimmed}$`, 'i'));
        }
      });
    }
  }

  /**
   * 설정 객체에서 로드
   */
  private loadFromConfig(config: MaskConfig): void {
    // 정확 매칭 필드
    if (config.sensitive) {
      config.sensitive.forEach((field) => {
        this.sensitiveKeyPatterns.push(new RegExp(`^${field}$`, 'i'));
      });
    }

    // 패턴 매칭
    if (config.sensitivePatterns) {
      config.sensitivePatterns.forEach((pattern) => {
        this.sensitiveKeyPatterns.push(new RegExp(pattern, 'i'));
      });
    }

    // 제외 필드
    if (config.exclude) {
      config.exclude.forEach((field) => {
        this.excludePatterns.push(new RegExp(`^${field}$`, 'i'));
      });
    }

    // 제외 패턴
    if (config.excludePatterns) {
      config.excludePatterns.forEach((pattern) => {
        this.excludePatterns.push(new RegExp(pattern, 'i'));
      });
    }
  }

  /**
   * 설정 파일에서 로드 (Node.js 환경)
   */
  private async loadFromFile(path: string): Promise<void> {
    try {
      if (typeof require !== 'undefined') {
        const fs = require('fs').promises;
        const content = await fs.readFile(path, 'utf-8');
        const config: MaskConfig = JSON.parse(content);
        this.loadFromConfig(config);
        console.log(`[DataMasker] Loaded config from ${path}`);
      }
    } catch (error) {
      // 파일 없으면 무시
    }
  }

  /**
   * 객체 마스킹
   */
  mask(obj: any): any {
    return this.maskRecursive(obj, new Set());
  }

  /**
   * JSON 문자열로 마스킹
   */
  maskToJson(obj: any): string {
    return JSON.stringify(this.mask(obj));
  }

  /**
   * 재귀적으로 마스킹 처리
   */
  private maskRecursive(obj: any, visited: Set<any>): any {
    // null, undefined
    if (obj == null) return obj;

    // 순환 참조 방지
    if (typeof obj === 'object' && visited.has(obj)) {
      return '[Circular]';
    }

    // 원시 타입
    if (typeof obj !== 'object') {
      if (typeof obj === 'string' && this.isSensitiveValue(obj)) {
        return this.maskValue(obj);
      }
      return obj;
    }

    // 방문 표시
    visited.add(obj);

    // 배열
    if (Array.isArray(obj)) {
      return obj.map((item) => this.maskRecursive(item, visited));
    }

    // 객체
    const result: any = {};
    for (const key in obj) {
      if (!Object.prototype.hasOwnProperty.call(obj, key)) continue;

      const value = obj[key];

      // 제외 패턴 체크
      if (this.shouldExclude(key)) {
        result[key] = '<excluded>';
      }
      // 민감 키 체크
      else if (this.isSensitiveKey(key)) {
        result[key] = '****';
      }
      // 민감 값 체크
      else if (typeof value === 'string' && this.isSensitiveValue(value)) {
        result[key] = this.maskValue(value);
      }
      // 재귀
      else if (typeof value === 'object' && value !== null) {
        result[key] = this.maskRecursive(value, visited);
      } else {
        result[key] = value;
      }
    }

    return result;
  }

  /**
   * 키가 민감한지 체크
   */
  private isSensitiveKey(key: string): boolean {
    return this.sensitiveKeyPatterns.some((pattern) => pattern.test(key));
  }

  /**
   * 값이 민감한지 체크
   */
  private isSensitiveValue(value: string): boolean {
    // 너무 짧으면 무시 (false positive 방지)
    if (value.length < 6) return false;

    return this.sensitiveValuePatterns.some((pattern) => pattern.test(value));
  }

  /**
   * 제외할 키인지 체크
   */
  private shouldExclude(key: string): boolean {
    return this.excludePatterns.some((pattern) => pattern.test(key));
  }

  /**
   * 값 마스킹 처리
   */
  private maskValue(value: string): string {
    // 이메일
    if (/@/.test(value) && value.includes('.')) {
      const [local, domain] = value.split('@');
      if (local.length > 0) {
        return `${local[0]}***@${domain}`;
      }
    }

    // 주민등록번호 - 연도만 노출
    if (/^\d{6}-[1-4]\d{6}$/.test(value)) {
      return value.substring(0, 2) + '****-*******';
    }

    // 카드번호
    if (/^\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}$/.test(value)) {
      return value.replace(/\d(?=[\d\s-]*\d{4}$)/g, '*');
    }

    // 전화번호
    if (
      /^(\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{4}$/.test(
        value,
      )
    ) {
      const digitsOnly = value.replace(/\D/g, '');
      if (digitsOnly.length >= 8) {
        const masked = '*'.repeat(digitsOnly.length - 4) + digitsOnly.slice(-4);
        return masked;
      }
    }

    // 기본 - 전체 마스킹
    return '****';
  }

  /**
   * 런타임에 패턴 추가
   */
  addSensitiveKey(pattern: string | RegExp): this {
    const regex =
      pattern instanceof RegExp ? pattern : new RegExp(`^${pattern}$`, 'i');
    this.sensitiveKeyPatterns.push(regex);
    return this;
  }

  /**
   * 런타임에 제외 패턴 추가
   */
  addExclude(pattern: string | RegExp): this {
    const regex =
      pattern instanceof RegExp ? pattern : new RegExp(`^${pattern}$`, 'i');
    this.excludePatterns.push(regex);
    return this;
  }

  /**
   * 현재 설정 상태 조회 (디버깅용)
   */
  getStatus() {
    return {
      sensitiveKeyPatterns: this.sensitiveKeyPatterns.length,
      sensitiveValuePatterns: this.sensitiveValuePatterns.length,
      excludePatterns: this.excludePatterns.length,
    };
  }
}

export { DataMasker };
