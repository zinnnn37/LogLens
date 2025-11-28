/**
 * ValidationBadge Component
 *
 * AI 분석 결과의 유효성 검증 정보를 표시하는 컴포넌트
 */

import type { ValidationInfo } from '@/types/validation';
import {
  getConfidenceLevel,
  DATA_QUALITY_CONFIG,
  SAMPLING_STRATEGY_LABELS,
} from '@/types/validation';

import { Badge } from '@/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { CheckCircle2, AlertCircle, Info } from 'lucide-react';

interface ValidationBadgeProps {
  validation: ValidationInfo;
  /** 표시 모드: 'compact' (뱃지만), 'full' (전체 정보) */
  mode?: 'compact' | 'full';
  className?: string;
}

/**
 * ValidationBadge - AI 분석 유효성 정보 표시
 */
export const ValidationBadge = ({
  validation,
  mode = 'compact',
  className = '',
}: ValidationBadgeProps) => {
  const confidenceLevel = getConfidenceLevel(validation.confidence);
  const qualityConfig = DATA_QUALITY_CONFIG[validation.dataQuality];

  // 신뢰도 색상 매핑
  const confidenceColorMap: Record<string, string> = {
    green: 'bg-green-100 text-green-800 border-green-200',
    blue: 'bg-blue-100 text-blue-800 border-blue-200',
    yellow: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    red: 'bg-red-100 text-red-800 border-red-200',
  };

  const qualityColorMap: Record<string, string> = {
    green: 'bg-green-50 text-green-700',
    yellow: 'bg-yellow-50 text-yellow-700',
    red: 'bg-red-50 text-red-700',
  };

  // 아이콘 선택
  const ConfidenceIcon =
    confidenceLevel.level === 'high'
      ? CheckCircle2
      : confidenceLevel.level === 'medium'
        ? Info
        : AlertCircle;

  if (mode === 'compact') {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div className={`inline-flex items-center gap-2 ${className}`}>
              <Badge
                variant="outline"
                className={`${confidenceColorMap[confidenceLevel.color]} px-2 py-1 font-medium`}
              >
                <ConfidenceIcon className="mr-1 h-3 w-3" />
                신뢰도 {validation.confidence}%
              </Badge>
              <Badge
                variant="outline"
                className={`${qualityColorMap[qualityConfig.color]} px-2 py-1`}
              >
                품질: {qualityConfig.label}
              </Badge>
            </div>
          </TooltipTrigger>
          <TooltipContent side="bottom" className="max-w-sm">
            <div className="space-y-2 text-sm">
              <div>
                <strong>샘플 수:</strong> {validation.sampleCount}개 로그
              </div>
              <div>
                <strong>전략:</strong>{' '}
                {SAMPLING_STRATEGY_LABELS[validation.samplingStrategy] ||
                  validation.samplingStrategy}
              </div>
              <div>
                <strong>커버리지:</strong> {validation.coverage}
              </div>
              {validation.limitation && (
                <div className="text-yellow-600">
                  <strong>제한사항:</strong> {validation.limitation}
                </div>
              )}
            </div>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  // Full 모드: 전체 정보 표시
  return (
    <div className={`rounded-lg border bg-gray-50 p-4 ${className}`}>
      <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold text-gray-700">
        <Info className="h-4 w-4" />
        분석 유효성 검증
      </h4>

      <div className="mb-3 grid grid-cols-2 gap-3">
        {/* 신뢰도 */}
        <div className="flex flex-col">
          <span className="mb-1 text-xs text-gray-500">신뢰도</span>
          <div className="flex items-center gap-2">
            <Badge
              className={`${confidenceColorMap[confidenceLevel.color]} px-2 py-1`}
            >
              <ConfidenceIcon className="mr-1 h-3 w-3" />
              {validation.confidence}%
            </Badge>
            <span className="text-xs text-gray-600">
              {confidenceLevel.label}
            </span>
          </div>
        </div>

        {/* 데이터 품질 */}
        <div className="flex flex-col">
          <span className="mb-1 text-xs text-gray-500">데이터 품질</span>
          <div className="flex items-center gap-2">
            <Badge
              className={`${qualityColorMap[qualityConfig.color]} px-2 py-1`}
            >
              {qualityConfig.label}
            </Badge>
            <span className="text-xs text-gray-600">
              {qualityConfig.description}
            </span>
          </div>
        </div>

        {/* 샘플 수 */}
        <div className="flex flex-col">
          <span className="mb-1 text-xs text-gray-500">분석 샘플 수</span>
          <span className="text-sm font-medium text-gray-900">
            {validation.sampleCount}개 로그
          </span>
        </div>

        {/* 샘플링 전략 */}
        <div className="flex flex-col">
          <span className="mb-1 text-xs text-gray-500">샘플링 전략</span>
          <span className="text-sm font-medium text-gray-900">
            {SAMPLING_STRATEGY_LABELS[validation.samplingStrategy] ||
              validation.samplingStrategy}
          </span>
        </div>
      </div>

      {/* 커버리지 */}
      <div className="mb-3">
        <span className="mb-1 block text-xs text-gray-500">분석 커버리지</span>
        <p className="text-sm text-gray-700">{validation.coverage}</p>
      </div>

      {/* 제한사항 */}
      {validation.limitation && (
        <div className="rounded-md border border-yellow-200 bg-yellow-50 p-3">
          <div className="flex items-start gap-2">
            <AlertCircle className="mt-0.5 h-4 w-4 flex-shrink-0 text-yellow-600" />
            <div>
              <span className="mb-1 block text-xs font-semibold text-yellow-800">
                제한사항
              </span>
              <p className="text-xs text-yellow-700">{validation.limitation}</p>
            </div>
          </div>
        </div>
      )}

      {/* 추가 참고사항 */}
      {validation.note && (
        <div className="mt-3 text-xs text-gray-500 italic">
          {validation.note}
        </div>
      )}
    </div>
  );
};

/**
 * 신뢰도만 표시하는 간단한 인라인 뱃지
 */
export const ConfidenceBadge = ({ confidence }: { confidence: number }) => {
  const { level, label, color } = getConfidenceLevel(confidence);
  const Icon =
    level === 'high' ? CheckCircle2 : level === 'medium' ? Info : AlertCircle;

  const colorMap: Record<string, string> = {
    green: 'bg-green-100 text-green-800',
    blue: 'bg-blue-100 text-blue-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    red: 'bg-red-100 text-red-800',
  };

  return (
    <Badge
      variant="outline"
      className={`${colorMap[color]} px-2 py-1 font-medium`}
    >
      <Icon className="mr-1 h-3 w-3" />
      {confidence}% {label}
    </Badge>
  );
};
