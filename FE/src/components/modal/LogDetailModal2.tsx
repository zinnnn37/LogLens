// src/components/modal/LogDetailModal2.tsx
import { useState } from 'react';
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
import type { JiraIssueType, JiraIssuePriority } from '@/types/jira';

export interface JiraTicketFormData {
  summary: string;
  description: string;
  issueType: JiraIssueType;
  priority: JiraIssuePriority;
}

export interface LogDetailModal2Props {
  log: LogData;
  onGoBack: () => void;
  onSubmit: (formData: JiraTicketFormData) => void;
}

// 이슈 타입
const JIRA_ISSUE_TYPES: { value: JiraIssueType; label: string }[] = [
  { value: 'Bug', label: '버그 (Bug)' },
  { value: 'Task', label: '태스크 (Task)' },
  { value: 'Story', label: '스토리 (Story)' },
  { value: 'Epic', label: '에픽 (Epic)' },
];

// 우선 순위
const JIRA_PRIORITIES: { value: JiraIssuePriority; label: string }[] = [
  { value: 'Highest', label: 'Highest' },
  { value: 'High', label: 'High' },
  { value: 'Medium', label: 'Medium' },
  { value: 'Low', label: 'Low' },
  { value: 'Lowest', label: 'Lowest' },
];

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

const LogDetailModal2 = ({ log, onGoBack, onSubmit }: LogDetailModal2Props) => {
  // 폼 상태 관리
  const [summary, setSummary] = useState(
    `[${log.logLevel}] ${log.message.substring(0, 50)}...`,
  );
  const [description, setDescription] = useState(
    `[로그 상세 정보]\n- TraceID: ${log.traceId}\n- Level: ${
      log.logLevel
    }\n- System: ${log.sourceType}\n- Date: ${new Date(log.timestamp).toLocaleString('ko-KR', { timeZone: 'UTC' })}\n\n[Message]\n${log.message}`,
  );
  const [issueType, setIssueType] = useState<JiraIssueType>('Bug');
  const [priority, setPriority] = useState<JiraIssuePriority>('Medium');

  const handleSubmit = () => {
    const formData: JiraTicketFormData = {
      summary,
      description,
      issueType,
      priority,
    };
    onSubmit(formData);
  };

  return (
    <div className="flex flex-col space-y-6">
      <div className="border-b pb-4">
        <h2 className="text-lg font-semibold">{log.traceId} Jira 티켓 발행</h2>
        <p className="text-sm text-gray-500">{new Date().toLocaleString()}</p>
      </div>

      <div className="space-y-4">
        <FormSection label="제목" htmlFor="jira-title">
          <Input
            id="jira-title"
            placeholder="이슈 제목을 입력하세요"
            className="font-mono"
            value={summary}
            onChange={e => setSummary(e.target.value)}
          />
        </FormSection>

        <FormSection label="설명" htmlFor="jira-desc">
          <Textarea
            id="jira-desc"
            placeholder="이슈 설명을 입력하세요"
            rows={10}
            className="resize-none font-mono"
            value={description}
            onChange={e => setDescription(e.target.value)}
          />
        </FormSection>

        <FormSection label="이슈타입" htmlFor="jira-issue-type">
          <Select
            value={issueType}
            onValueChange={val => setIssueType(val as JiraIssueType)}
          >
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

        <FormSection label="우선순위" htmlFor="jira-priority">
          <Select
            value={priority}
            onValueChange={val => setPriority(val as JiraIssuePriority)}
          >
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

      <div className="flex justify-end space-x-2 pt-4">
        <Button variant="outline" onClick={onGoBack}>
          이전
        </Button>
        <Button
          onClick={handleSubmit}
          className="bg-[#0052CC] hover:bg-[#0747A6]"
        >
          발행하기
        </Button>
      </div>
    </div>
  );
};

export default LogDetailModal2;
