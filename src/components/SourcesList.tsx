/**
 * SourcesList Component
 *
 * AI 분석에 사용된 로그 출처 목록을 표시하는 컴포넌트
 */

import { useState } from 'react';

import type { LogSource } from '@/types/validation';

import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import {
  FileText,
  Clock,
  Server,
  Code,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';

interface SourcesListProps {
  sources: LogSource[];
  /** 표시 모드: 'compact' (접을 수 있음), 'full' (항상 펼침) */
  mode?: 'compact' | 'full';
  /** 기본 펼침 상태 (mode='compact'일 때만 유효) */
  defaultExpanded?: boolean;
  /** 최대 표시 개수 (기본: 모두 표시) */
  maxDisplay?: number;
  className?: string;
}

/**
 * SourcesList - AI 분석 출처 로그 목록
 */
export const SourcesList = ({
  sources,
  mode = 'compact',
  defaultExpanded = false,
  maxDisplay,
  className = '',
}: SourcesListProps) => {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);

  if (!sources || sources.length === 0) {
    return null;
  }

  const displaySources = maxDisplay ? sources.slice(0, maxDisplay) : sources;
  const hasMore = maxDisplay && sources.length > maxDisplay;

  // 로그 레벨 색상 매핑
  const levelColorMap: Record<string, string> = {
    ERROR: 'bg-red-100 text-red-800 border-red-200',
    WARN: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    INFO: 'bg-blue-100 text-blue-800 border-blue-200',
  };

  const content = (
    <div className="space-y-2">
      {displaySources.map((source, index) => (
        <Card
          key={`${source.logId}-${index}`}
          className="p-3 transition-shadow hover:shadow-md"
        >
          <div className="flex items-start justify-between gap-3">
            {/* 로그 정보 */}
            <div className="min-w-0 flex-1">
              {/* 헤더: 로그 레벨 + 서비스명 */}
              <div className="mb-2 flex items-center gap-2">
                <Badge
                  variant="outline"
                  className={`${levelColorMap[source.level] || 'bg-gray-100 text-gray-800'} px-2 py-0.5 text-xs font-medium`}
                >
                  {source.level}
                </Badge>
                {source.serviceName && (
                  <div className="flex items-center gap-1 text-xs text-gray-600">
                    <Server className="h-3 w-3" />
                    <span className="font-medium">{source.serviceName}</span>
                  </div>
                )}
                {source.relevanceScore !== undefined && (
                  <Badge
                    variant="outline"
                    className="bg-purple-50 px-2 py-0.5 text-xs text-purple-700"
                  >
                    관련도: {(source.relevanceScore * 100).toFixed(0)}%
                  </Badge>
                )}
              </div>

              {/* 로그 메시지 */}
              <p className="mb-2 line-clamp-2 text-sm text-gray-900">
                {source.message}
              </p>

              {/* 메타데이터 */}
              <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500">
                {/* 타임스탬프 */}
                <div className="flex items-center gap-1">
                  <Clock className="h-3 w-3" />
                  <span>{formatTimestamp(source.timestamp)}</span>
                </div>

                {/* 클래스/메서드 */}
                {(source.className || source.methodName) && (
                  <div className="flex items-center gap-1">
                    <Code className="h-3 w-3" />
                    <span className="font-mono">
                      {source.className && (
                        <span className="text-gray-600">
                          {getShortClassName(source.className)}
                        </span>
                      )}
                      {source.className && source.methodName && (
                        <span className="text-gray-400">.</span>
                      )}
                      {source.methodName && (
                        <span className="text-gray-700">
                          {source.methodName}()
                        </span>
                      )}
                    </span>
                  </div>
                )}

                {/* 로그 ID */}
                <div className="flex items-center gap-1">
                  <FileText className="h-3 w-3" />
                  <span className="font-mono text-gray-400">
                    ID: {source.logId}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Card>
      ))}

      {hasMore && (
        <div className="py-2 text-center">
          <span className="text-sm text-gray-500">
            외 {sources.length - maxDisplay!}개 로그 더 있음
          </span>
        </div>
      )}
    </div>
  );

  if (mode === 'compact') {
    return (
      <div className={className}>
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="flex w-full items-center justify-between rounded-lg border border-gray-200 bg-gray-50 p-3 transition-colors hover:bg-gray-100"
        >
          <div className="flex items-center gap-2">
            <FileText className="h-4 w-4 text-gray-600" />
            <span className="text-sm font-medium text-gray-700">
              분석 출처 로그 ({sources.length}개)
            </span>
          </div>
          {isExpanded ? (
            <ChevronUp className="h-4 w-4 text-gray-600" />
          ) : (
            <ChevronDown className="h-4 w-4 text-gray-600" />
          )}
        </button>

        {isExpanded && <div className="mt-3">{content}</div>}
      </div>
    );
  }

  // Full 모드
  return (
    <div className={className}>
      <div className="mb-3 flex items-center gap-2">
        <FileText className="h-4 w-4 text-gray-600" />
        <h4 className="text-sm font-semibold text-gray-700">
          분석 출처 로그 ({sources.length}개)
        </h4>
      </div>
      {content}
    </div>
  );
};

/**
 * 타임스탬프 포맷팅 헬퍼
 */
const formatTimestamp = (timestamp: string): string => {
  try {
    const date = new Date(timestamp);
    return format(date, 'yyyy-MM-dd HH:mm:ss');
  } catch {
    return timestamp;
  }
};

/**
 * 클래스명 축약 헬퍼 (패키지명 제거)
 */
const getShortClassName = (className: string): string => {
  const parts = className.split('.');
  return parts[parts.length - 1] || className;
};

/**
 * 단일 로그 출처를 인라인으로 표시하는 간단한 컴포넌트
 */
export const SourceBadge = ({ source }: { source: LogSource }) => {
  const levelColorMap: Record<string, string> = {
    ERROR: 'bg-red-100 text-red-800',
    WARN: 'bg-yellow-100 text-yellow-800',
    INFO: 'bg-blue-100 text-blue-800',
  };

  return (
    <div className="inline-flex items-center gap-2 rounded-md border border-gray-200 bg-gray-50 px-3 py-1.5">
      <Badge
        className={`${levelColorMap[source.level] || 'bg-gray-100 text-gray-800'} px-2 py-0.5 text-xs`}
      >
        {source.level}
      </Badge>
      <span className="max-w-xs truncate text-xs text-gray-600">
        {source.message}
      </span>
      {source.relevanceScore !== undefined && (
        <span className="text-xs font-medium text-purple-600">
          {(source.relevanceScore * 100).toFixed(0)}%
        </span>
      )}
    </div>
  );
};
