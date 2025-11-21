import { AlertCircle, Link2, Copy, CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useState } from 'react';

interface ProjectNotConnectedPageProps {
  projectName?: string;
  projectUuid: string;
}

const ProjectNotConnectedPage = ({
  projectName = '프로젝트',
  projectUuid,
}: ProjectNotConnectedPageProps) => {
  const [copied, setCopied] = useState(false);

  // 설치 가이드 URL (실제 문서 URL로 변경 필요)
  const docsUrl = '/docs';

  const handleCopyUuid = () => {
    navigator.clipboard.writeText(projectUuid);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex h-full items-center justify-center">
      <div className="w-full max-w-2xl px-6">
        {/* 메인 카드 */}
        <div className="rounded-2xl border-2 border-orange-200 bg-white p-8 shadow-lg">
          {/* 아이콘과 헤더 */}
          <div className="mb-6 flex flex-col items-center text-center">
            <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-orange-100">
              <Link2 className="h-10 w-10 text-orange-600" />
            </div>
            <h1 className="text-foreground mb-2 text-2xl font-bold">
              프로젝트가 연결되지 않았습니다
            </h1>
            <p className="text-muted-foreground text-base">
              <span className="font-semibold text-orange-600">
                {projectName}
              </span>{' '}
              프로젝트에 아직 LogLens 라이브러리가 연결되지 않았습니다.
            </p>
          </div>

          {/* 안내 섹션 */}
          <div className="mb-6 space-y-4">
            <div className="rounded-lg bg-blue-50 p-4">
              <div className="mb-2 flex items-start gap-3">
                <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-600" />
                <div className="flex-1">
                  <h3 className="mb-2 font-semibold text-blue-900">
                    LogLens를 사용하려면:
                  </h3>
                  <ol className="space-y-2 text-sm text-blue-800">
                    <li className="flex gap-2">
                      <span className="font-semibold">1.</span>
                      <span>프로젝트에 LogLens 라이브러리를 설치하세요</span>
                    </li>
                    <li className="flex gap-2">
                      <span className="font-semibold">2.</span>
                      <span>아래 프로젝트 UUID를 설정 파일에 입력하세요</span>
                    </li>
                    <li className="flex gap-2">
                      <span className="font-semibold">3.</span>
                      <span>애플리케이션을 재시작하고 로그를 생성하세요</span>
                    </li>
                  </ol>
                </div>
              </div>
            </div>

            {/* UUID 복사 섹션 */}
            <div className="rounded-lg border-2 border-gray-200 bg-gray-50 p-4">
              <label className="mb-2 block text-sm font-semibold text-gray-700">
                프로젝트 UUID:
              </label>
              <div className="flex items-center gap-2">
                <code className="bg-background flex-1 rounded border border-gray-300 px-3 py-2 font-mono text-sm">
                  {projectUuid}
                </code>
                <Button
                  onClick={handleCopyUuid}
                  variant="outline"
                  size="sm"
                  className="gap-2"
                >
                  {copied ? (
                    <>
                      <CheckCircle className="h-4 w-4" />
                      복사됨
                    </>
                  ) : (
                    <>
                      <Copy className="h-4 w-4" />
                      복사
                    </>
                  )}
                </Button>
              </div>
            </div>
          </div>

          {/* 액션 버튼 */}
          <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
            <Button
              onClick={() => window.open(docsUrl, '_blank')}
              className="gap-2"
              size="lg"
            >
              <svg
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                />
              </svg>
              설치 가이드 보기
            </Button>
            <Button
              onClick={() => window.location.reload()}
              variant="outline"
              size="lg"
              className="gap-2"
            >
              <svg
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              연결 상태 다시 확인
            </Button>
          </div>
        </div>

        {/* 추가 도움말 */}
        <div className="mt-6 text-center">
          <p className="text-muted-foreground text-sm">
            설치에 문제가 있으신가요?{' '}
            <a
              href="/docs#faq"
              className="text-primary font-semibold hover:underline"
            >
              FAQ 확인하기
            </a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default ProjectNotConnectedPage;
