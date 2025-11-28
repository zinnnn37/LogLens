/**
 * Chat/Chatbot Related Types
 */

import type { LogSource, ValidationInfo } from './validation';

/**
 * 관련 로그 정보 (Chatbot 응답에 포함)
 */
export interface RelatedLog {
  /** 로그 ID */
  logId: number;
  /** 로그 메시지 */
  message: string;
  /** 로그 레벨 */
  level: string;
  /** 타임스탬프 */
  timestamp: string;
  /** 서비스 이름 */
  serviceName?: string;
  /** 관련성 점수 (0.0 ~ 1.0) */
  relevanceScore?: number;
}

/**
 * Chatbot API 응답
 * V2 Chatbot 엔드포인트의 응답 타입
 */
export interface ChatResponse {
  /** AI가 생성한 답변 */
  answer: string;
  /** 캐시된 응답 여부 */
  fromCache: boolean;
  /** 관련 로그 목록 (기존 방식) */
  relatedLogs: RelatedLog[];
  /** 답변 생성 시각 (ISO 8601 형식) */
  answeredAt: string;

  // V2 필드 (RAG 검증용)
  /** 답변 생성에 사용된 로그 출처 */
  sources?: LogSource[];
  /** 답변의 유효성 검증 정보 */
  validation?: ValidationInfo;
}

/**
 * Chatbot 질문 요청
 */
export interface ChatRequest {
  /** 사용자 질문 */
  question: string;
  /** 프로젝트 UUID */
  projectUuid: string;
  /** 대화 히스토리 (선택) */
  chatHistory?: ChatMessage[];
}

/**
 * 대화 메시지 (히스토리용)
 */
export interface ChatMessage {
  /** 역할 (user 또는 assistant) */
  role: 'user' | 'assistant';
  /** 메시지 내용 */
  content: string;
  /** 타임스탬프 */
  timestamp?: string;
}

/**
 * SSE 스트리밍 응답 이벤트
 */
export interface ChatStreamEvent {
  /** 이벤트 타입 */
  type: 'answer' | 'sources' | 'validation' | 'error' | 'done';
  /** 이벤트 데이터 */
  data: string | LogSource[] | ValidationInfo | Record<string, unknown>;
}
