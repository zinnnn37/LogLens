import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';
import { ApiError } from '@/types/api';
import { useProjectStore } from '@/stores/projectStore';
import { fetchProjects } from '@/services/projectService';
import { connectJiraIntegration } from '@/services/jiraService';
import type {
  JiraIntegrationModalProps,
  JiraFormData,
  JiraFormErrors,
  JiraConnectRequest,
} from '@/types/jira';

export const JiraIntegrationModal = ({
  open,
  onOpenChange,
  projectUuid,
}: JiraIntegrationModalProps) => {
  const [formData, setFormData] = useState<JiraFormData>({
    jiraUrl: '',
    jiraEmail: '',
    jiraApiToken: '',
    jiraProjectKey: '',
  });

  const [errors, setErrors] = useState<JiraFormErrors>({});
  const [isConnecting, setIsConnecting] = useState(false);

  const setProjectsInStore = useProjectStore(state => state.setProjects);

  const validateForm = (): boolean => {
    const newErrors: JiraFormErrors = {};

    if (!formData.jiraUrl.trim()) {
      newErrors.jiraUrl = 'Jira 도메인을 입력해주세요.';
    } else if (!formData.jiraUrl.includes('.atlassian.net')) {
      newErrors.jiraUrl =
        '올바른 Jira 도메인 형식이 아닙니다. (예: your-domain.atlassian.net)';
    }

    if (!formData.jiraEmail.trim()) {
      newErrors.jiraEmail = '이메일 주소를 입력해주세요.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.jiraEmail)) {
      newErrors.jiraEmail = '올바른 이메일 형식이 아닙니다.';
    }

    if (!formData.jiraApiToken.trim()) {
      newErrors.jiraApiToken = 'API 토큰을 입력해주세요.';
    }

    if (!formData.jiraProjectKey.trim()) {
      newErrors.jiraProjectKey = '프로젝트 키를 입력해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Jira 연동 API 호출
   */
  const handleConnect = async () => {
    if (!validateForm()) {
      return;
    }

    setIsConnecting(true);
    try {
      const requestBody: JiraConnectRequest = {
        projectUuid,
        jiraUrl: formData.jiraUrl,
        jiraEmail: formData.jiraEmail,
        jiraApiToken: formData.jiraApiToken,
        jiraProjectKey: formData.jiraProjectKey,
      };

      await connectJiraIntegration(requestBody);

      toast.success('Jira 연동에 성공했습니다.');
      onOpenChange(false);

      const updatedProjects = await fetchProjects();
      setProjectsInStore(updatedProjects);

      // reset
      setFormData({
        jiraUrl: '',
        jiraEmail: '',
        jiraApiToken: '',
        jiraProjectKey: '',
      });
      setErrors({});
    } catch (error) {
      if (error instanceof ApiError && error.response) {
        toast.error(error.response.message);
      } else {
        toast.error('연동에 실패했습니다. 입력 정보를 확인해주세요.');
      }
      console.error('Jira connection failed:', error);
    } finally {
      setIsConnecting(false);
    }
  };

  const handleInputChange = (field: keyof JiraFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isConnecting) {
      void handleConnect();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Jira API 연동</DialogTitle>
          <DialogDescription>
            Jira와 연동하기 위한 정보를 입력해주세요.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit}>
          <div className="space-y-6 py-4">
            {/* Jira 도메인 */}
            <div className="grid gap-3">
              <Label htmlFor="jira-domain">
                Jira 도메인 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="jira-domain"
                value={formData.jiraUrl}
                onChange={ev => handleInputChange('jiraUrl', ev.target.value)}
                placeholder="https://your-domain.atlassian.net"
                aria-invalid={Boolean(errors.jiraUrl)}
                aria-describedby={
                  errors.jiraUrl ? 'jira-domain-error' : undefined
                }
              />
              {errors.jiraUrl ? (
                <p id="jira-domain-error" className="text-destructive text-sm">
                  {errors.jiraUrl}
                </p>
              ) : null}
            </div>

            {/* 이메일 주소 */}
            <div className="grid gap-3">
              <Label htmlFor="jira-email">
                이메일 주소 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="jira-email"
                type="email"
                value={formData.jiraEmail}
                onChange={ev => handleInputChange('jiraEmail', ev.target.value)}
                placeholder="your-email@example.com"
                aria-invalid={Boolean(errors.jiraEmail)}
                aria-describedby={
                  errors.jiraEmail ? 'jira-email-error' : undefined
                }
              />
              {errors.jiraEmail ? (
                <p id="jira-email-error" className="text-destructive text-sm">
                  {errors.jiraEmail}
                </p>
              ) : null}
            </div>

            {/* API 토큰 */}
            <div className="grid gap-3">
              <Label htmlFor="jira-token">
                API 토큰 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="jira-token"
                type="password"
                value={formData.jiraApiToken}
                onChange={ev =>
                  handleInputChange('jiraApiToken', ev.target.value)
                }
                placeholder="••••••••••••••••"
                aria-invalid={Boolean(errors.jiraApiToken)}
                aria-describedby={
                  errors.jiraApiToken ? 'jira-token-error' : undefined
                }
              />
              {errors.jiraApiToken ? (
                <p id="jira-token-error" className="text-destructive text-sm">
                  {errors.jiraApiToken}
                </p>
              ) : null}
              <a
                href="https://id.atlassian.com/manage-profile/security/api-tokens"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary text-sm hover:underline"
              >
                API 토큰 생성하기 →
              </a>
            </div>

            {/* 프로젝트 키 */}
            <div className="grid gap-3">
              <Label htmlFor="jira-project-key">
                프로젝트 키 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="jira-project-key"
                value={formData.jiraProjectKey}
                onChange={ev =>
                  handleInputChange('jiraProjectKey', ev.target.value)
                }
                placeholder="예: PROJ"
                aria-invalid={Boolean(errors.jiraProjectKey)}
                aria-describedby={
                  errors.jiraProjectKey ? 'jira-project-key-error' : undefined
                }
              />
              {errors.jiraProjectKey ? (
                <p
                  id="jira-project-key-error"
                  className="text-destructive text-sm"
                >
                  {errors.jiraProjectKey}
                </p>
              ) : null}
            </div>
          </div>

          <DialogFooter className="gap-2">
            <Button type="submit" disabled={isConnecting}>
              {isConnecting ? '연결 중...' : '연결하기'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default JiraIntegrationModal;
