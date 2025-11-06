// src/components/modal/LogDetailModal2.tsx
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import type { LogData } from '@/types/log';

export interface LogDetailModal2Props {
  log: LogData;
  onGoBack: () => void;
  onSubmit: (formData: JiraTicketFormData) => void;
}

export interface JiraTicketFormData {
  issueType: string;
  priority: string;
}

// 이슈 타입
const JIRA_ISSUE_TYPES = [
  { value: 'bug', label: '버그 (Bug)' },
  { value: 'task', label: '태스크 (Task)' },
  { value: 'story', label: '스토리 (Story)' },
  { value: 'epic', label: '에픽 (Epic)' },
];

// 우선순위
const JIRA_PRIORITIES = [
  { value: 'highest', label: 'Highest' },
  { value: 'high', label: 'High' },
  { value: 'medium', label: 'Medium' },
  { value: 'low', label: 'Low' },
  { value: 'lowest', label: 'Lowest' },
];


/**
 * 폼 섹션 래퍼
 */
const FormSection = ({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) => (
  <div className="grid grid-cols-4 items-center gap-4">
    <Label htmlFor={htmlFor} className="text-right text-sm font-medium">
      {label}
    </Label>
    <div className="col-span-3">{children}</div>
  </div>
);

/**
 * Jira 티켓 발행 모달
 */
const LogDetailModal2 = ({ log, onGoBack, onSubmit }: LogDetailModal2Props) => {
  const handleSubmit = () => {
  //  TODO : 수정예정
    const formData: JiraTicketFormData = {
      issueType: 'bug',
      priority: 'medium',
    };
    onSubmit(formData);
  };

  return (
    <div className="flex flex-col space-y-6">
      {/* 헤더 */}
      <div className="border-b pb-4">
        <h2 className="text-lg font-semibold">{log.traceId} Jira 티켓 발행</h2>
        <p className="text-sm text-gray-500">
          {new Date().toLocaleString()} 
        </p>
      </div>

      {/* 입력 폼 */}
      <div className="space-y-4">
        {/* 제목 */}
        <FormSection label="제목" htmlFor="jira-title">
          <Input
            id="jira-title"
            placeholder="자동으로 생성됨"
            className="font-mono"
            defaultValue={`[${log.logLevel}] ${log.message.substring(0, 50)}...`}
          />
        </FormSection>

        {/* 설명 */}
        <FormSection label="설명" htmlFor="jira-desc">
          <Textarea
            id="jira-desc"
            placeholder="자동으로 생성됨"
            rows={4}
            className="font-mono"
            defaultValue={`[로그 상세 정보]\n- TraceID: ${log.traceId
              }\n- Level: ${log.logLevel}\n- System: ${log.sourceType
              }\n- Date: ${new Date(
                log.timestamp,
              ).toLocaleString()}\n\n[Message]\n${log.message}`}
          />
        </FormSection>

        {/* 이슈타입 */}
        <FormSection label="이슈타입" htmlFor="jira-issue-type">
          <Select defaultValue="bug">
            <SelectTrigger id="jira-issue-type">
              <SelectValue placeholder="선택..." />
            </SelectTrigger>
            <SelectContent>
              {JIRA_ISSUE_TYPES.map(type => (
                <SelectItem key={type.value} value={type.value}>
                  {type.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormSection>

        {/* 우선순위 (원본과 동일) */}
        <FormSection label="우선순위" htmlFor="jira-priority">
          <Select defaultValue="medium">
            <SelectTrigger id="jira-priority">
              <SelectValue placeholder="선택..." />
            </SelectTrigger>
            <SelectContent>
              {JIRA_PRIORITIES.map(p => (
                <SelectItem key={p.value} value={p.value}>
                  {p.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FormSection>

      </div>

      {/* 푸터 */}
      <div className="flex justify-end space-x-2 pt-4">
        <Button variant="outline" onClick={onGoBack}>
          이전
        </Button>
        <Button onClick={handleSubmit}>발행하기</Button>
      </div>
    </div>
  );
};

export default LogDetailModal2;