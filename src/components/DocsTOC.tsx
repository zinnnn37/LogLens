import { useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { ROUTE_PATH } from '@/router/route-path';

interface TOCItem {
  id: string;
  title: string;
}

const tocItems: TOCItem[] = [
  { id: 'features', title: '주요 기능' },
  { id: 'installation', title: '설치 방법' },
  { id: 'configuration', title: '기본 설정' },
  { id: 'dependency-collection', title: '의존성 자동 수집' },
  { id: 'method-logging', title: '메서드 로깅' },
  { id: 'annotations', title: '어노테이션 사용법' },
  { id: 'async', title: '비동기 처리' },
  { id: 'trace-id', title: 'Trace ID 추적' },
  { id: 'settings', title: '설정 옵션' },
  { id: 'log-format', title: '로그 출력 형식' },
  { id: 'faq', title: 'FAQ' },
  { id: 'contact', title: '문의하기' },
];

const DocsTOC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isDocsPage = location.pathname === ROUTE_PATH.DOCS;
  const [activeSection, setActiveSection] = useState<string>('');
  const [isScrolling, setIsScrolling] = useState(false);

  // Docs 페이지를 벗어나면 activeSection 초기화
  useEffect(() => {
    if (!isDocsPage) {
      setActiveSection('');
      setIsScrolling(false);
    }
  }, [isDocsPage]);

  useEffect(() => {
    if (!isDocsPage) {
      return;
    }

    const observer = new IntersectionObserver(
      entries => {
        // 수동 스크롤 중이면 업데이트하지 않음
        if (isScrolling) {
          return;
        }

        // 화면에 보이는 섹션들 중 가장 위에 있는 것을 찾기
        const visibleSections = entries
          .filter(entry => entry.isIntersecting)
          .sort((a, b) => {
            return a.boundingClientRect.top - b.boundingClientRect.top;
          });

        if (visibleSections.length > 0) {
          setActiveSection(visibleSections[0].target.id);
        }
      },
      {
        rootMargin: '-100px 0px -66% 0px', // 상단에서 조금 내려온 부분을 기준으로
        threshold: 0,
      },
    );

    // 모든 섹션 관찰 시작
    tocItems.forEach(item => {
      const element = document.getElementById(item.id);
      if (element) {
        observer.observe(element);
      }
    });

    return () => {
      observer.disconnect();
    };
  }, [isDocsPage, isScrolling]);

  // URL hash 변경 감지하여 스크롤
  useEffect(() => {
    if (!isDocsPage) {
      return;
    }

    const hash = window.location.hash.substring(1); // # 제거
    if (hash) {
      // 페이지 로드 후 스크롤 (DOM이 완전히 렌더링될 때까지 대기)
      const scrollToHash = () => {
        const element = document.getElementById(hash);
        if (element) {
          setActiveSection(hash);
          setIsScrolling(true);
          setTimeout(() => {
            element.scrollIntoView({ behavior: 'smooth', block: 'start' });
            setTimeout(() => setIsScrolling(false), 1000);
          }, 100);
        }
      };

      // 여러 번 시도하여 DOM이 준비될 때까지 대기
      const timeouts = [100, 300, 500];
      timeouts.forEach(delay => {
        setTimeout(scrollToHash, delay);
      });
    }
  }, [isDocsPage, location.hash]);

  const scrollToSection = (id: string) => {
    setActiveSection(id); // 클릭 시 즉시 강조
    setIsScrolling(true); // 스크롤 중임을 표시

    if (!isDocsPage) {
      // 문서 페이지가 아니면 hash와 함께 문서 페이지로 이동
      navigate(`${ROUTE_PATH.DOCS}#${id}`);
    } else {
      // 이미 문서 페이지에 있으면 바로 스크롤
      const element = document.getElementById(id);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        // 스크롤 완료 후 isScrolling 해제 (smooth scroll 시간 고려)
        setTimeout(() => setIsScrolling(false), 1000);
      }
    }
  };

  return (
    <nav
      className="mt-2 ml-6 min-h-0 flex-1 overflow-y-auto"
      style={{
        scrollbarWidth: 'thin',
        scrollbarColor: '#d1d5db transparent',
      }}
    >
      <ul className="flex flex-col gap-1">
        {tocItems.map(item => {
          const isActive = activeSection === item.id;
          return (
            <li key={item.id}>
              <button
                onClick={() => scrollToSection(item.id)}
                className={`flex w-full items-center rounded-lg px-3 py-2 text-left text-sm transition-all ${
                  isActive
                    ? 'text-primary bg-primary/10 font-medium'
                    : 'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground text-[#6A6A6A]'
                }`}
              >
                {item.title}
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
};

export default DocsTOC;
