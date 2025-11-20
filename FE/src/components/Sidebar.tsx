// src/components/Sidebar.tsx
import { useEffect, useState } from 'react';
import type { ComponentProps } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import {
  PlusSquare,
  LogOut,
  BookOpen,
  Folder,
  GripVertical,
} from 'lucide-react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import type { DragEndEvent } from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import DocsTOC from '@/components/DocsTOC';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { logout } from '@/services/authApi';
import { useAuthStore } from '@/stores/authStore';
import { ROUTE_PATH, createProjectPath } from '@/router/route-path';

import { useProjectStore } from '@/stores/projectStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { fetchProjects, createProject } from '@/services/projectService';
import { getUnreadAlertCount } from '@/services/alertService';

// --- Sidebar Props ---
type SidebarProps = ComponentProps<'aside'> & {
  // 필요 시 확장
};

// 공통 스타일: 링크/버튼 모양 일치
const itemBase =
  'text-[#6A6A6A] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-all';

// 버튼 버전 (모달 트리거/액션 등)
const NavButton = ({
  icon: Icon,
  children,
  onClick,
}: {
  icon: React.ElementType;
  children: React.ReactNode;
  onClick?: () => void;
}) => {
  return (
    <button type="button" onClick={onClick} className={itemBase}>
      <Icon className="text-primary h-4 w-4" />
      {children}
    </button>
  );
};

// 섹션 헤딩
const NavHeading = ({ children }: { children: React.ReactNode }) => {
  return (
    <h2 className="text-muted-foreground mb-2 px-3 text-xs font-semibold tracking-wider uppercase">
      {children}
    </h2>
  );
};

// 드래그 가능한 프로젝트 아이템
const SortableProjectItem = ({
  project,
  isActive,
  onClick,
  hasNotification,
}: {
  project: { projectUuid: string; projectName: string };
  isActive: boolean;
  onClick: () => void;
  hasNotification: boolean;
}) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: project.projectUuid });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <li ref={setNodeRef} style={style}>
      <div
        className={`flex w-full items-center rounded-lg text-left text-sm transition-all ${
          isActive
            ? 'text-primary bg-primary/10 font-medium'
            : 'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground text-[#6A6A6A]'
        }`}
      >
        {/* 드래그 핸들 */}
        <button
          type="button"
          className="cursor-grab px-2 py-2 hover:text-gray-900 active:cursor-grabbing"
          {...attributes}
          {...listeners}
        >
          <GripVertical className="h-4 w-4" />
        </button>
        {/* 프로젝트 클릭 영역 */}
        <button
          type="button"
          onClick={onClick}
          className="flex flex-1 items-center gap-2 px-1 py-2 pr-3"
        >
          <span className="truncate">{project.projectName}</span>
          {/* 알림 인디케이터 */}
          {hasNotification && (
            <span className="ml-auto block h-2 w-2 flex-shrink-0 rounded-full bg-red-500" />
          )}
        </button>
      </div>
    </li>
  );
};

const Sidebar = ({ className, ...props }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const { clearAuth } = useAuthStore();

  // 프로젝트 생성 모달 Open 상태 관리
  const [openCreate, setOpenCreate] = useState(false);

  // 프로젝트 목록(store)
  const projects = useProjectStore(state => state.projects);
  const setProjectsInStore = useProjectStore(state => state.setProjects);
  const updateProjects = useProjectStore(state => state.updateProjects);

  // 알림 상태(store)
  const projectNotifications = useNotificationStore(
    state => state.projectNotifications,
  );
  const setProjectNotification = useNotificationStore(
    state => state.setProjectNotification,
  );

  // 드래그 앤 드롭 센서
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  // 초기 프로젝트 목록 로드 (메인페이지 패턴과 동일)
  useEffect(() => {
    const loadProjects = async () => {
      try {
        const response = await fetchProjects();
        setProjectsInStore(response);

        // 각 프로젝트의 알림 상태 확인
        response.content.forEach(async project => {
          try {
            const alertResponse = await getUnreadAlertCount({
              projectUuid: project.projectUuid,
            });
            setProjectNotification(
              project.projectUuid,
              (alertResponse.unreadCount || 0) > 0,
            );
          } catch (error) {
            console.error(
              `프로젝트 ${project.projectName} 알림 조회 실패:`,
              error,
            );
          }
        });
      } catch (error) {
        // 목록 로드 실패 시 콘솔만 (UX 정책은 추후)
        console.error('프로젝트 목록 로드 실패', error);
      }
    };
    loadProjects();
  }, [setProjectsInStore, setProjectNotification]);

  // 프로젝트 선택 핸들러
  const handleProjectSelect = (selectedProjectUuid: string) => {
    navigate(createProjectPath(selectedProjectUuid, 'dashboard'));
  };

  // 로그아웃 핸들러
  const handleLogout = async () => {
    try {
      await logout(); // 서버에 로그아웃 요청
      clearAuth(); // 로컬 토큰 삭제
      navigate(ROUTE_PATH.LOGIN); // 로그인 페이지로 이동
    } catch (error) {
      console.error('로그아웃 실패:', error);
      // 에러가 발생해도 로컬 토큰은 삭제하고 로그인 페이지로 이동
      clearAuth();
      navigate(ROUTE_PATH.LOGIN);
    }
  };

  // 드래그 종료 핸들러
  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = projects.findIndex(p => p.projectUuid === active.id);
      const newIndex = projects.findIndex(p => p.projectUuid === over.id);

      const newProjects = arrayMove(projects, oldIndex, newIndex);
      updateProjects(newProjects);
    }
  };

  return (
    <aside
      className={`border-sidebar-border flex h-[100dvh] w-56 flex-col border-r p-2 ${className || ''}`}
      {...props}
    >
      {/* 로고 - 고정 */}
      <div className="mb-3 flex h-14 flex-shrink-0 items-center px-2">
        <a href="/" className="font-pretendard text-4xl font-bold">
          <span className="text-[#1C1C1C]">Log</span>
          <span className="text-[#6A91BE]">Lens</span>
        </a>
      </div>

      {/* 네비게이션 */}
      <div className="font-godoM flex min-h-0 flex-1 flex-col overflow-y-auto [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <nav className="flex flex-col gap-4">
          {/* Projects */}
          <section className="flex-shrink-0">
            <NavHeading>
              <p className="font-pretendard">projects</p>
            </NavHeading>
            <div className="flex flex-col gap-1">
              <NavButton icon={PlusSquare} onClick={() => setOpenCreate(true)}>
                새 프로젝트 생성
              </NavButton>

              <Accordion type="single" collapsible defaultValue="project-list">
                <AccordionItem value="project-list" className="border-none">
                  <AccordionTrigger
                    className={`${itemBase} py-2 text-base font-normal hover:no-underline`}
                  >
                    <div className="flex items-center gap-3">
                      <Folder className="text-primary h-4 w-4" />
                      <span>프로젝트 목록</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="pb-0">
                    {/* 프로젝트 목록 - 드래그 앤 드롭 */}
                    <DndContext
                      sensors={sensors}
                      collisionDetection={closestCenter}
                      onDragEnd={handleDragEnd}
                    >
                      <SortableContext
                        items={projects.map(p => p.projectUuid)}
                        strategy={verticalListSortingStrategy}
                      >
                        <ul className="flex flex-col gap-1 pl-2 text-[#6A6A6A]">
                          {projects.map(p => {
                            const isActive = projectUuid === p.projectUuid;
                            const hasProjectNotification =
                              projectNotifications[p.projectUuid] ?? false;
                            return (
                              <SortableProjectItem
                                key={p.projectUuid}
                                project={p}
                                isActive={isActive}
                                hasNotification={hasProjectNotification}
                                onClick={() =>
                                  handleProjectSelect(p.projectUuid)
                                }
                              />
                            );
                          })}
                        </ul>
                      </SortableContext>
                    </DndContext>
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </div>
          </section>

          {/* Support - 목차만 스크롤 */}
          <section className="flex-shrink-0">
            <NavHeading>
              <p className="font-pretendard">Support</p>
            </NavHeading>
            <div className="flex flex-col gap-1">
              <Accordion type="single" collapsible defaultValue="user-guide">
                <AccordionItem value="user-guide" className="border-none">
                  <AccordionTrigger
                    className={`${itemBase} py-2 text-base font-normal hover:no-underline`}
                  >
                    <div className="flex items-center gap-3">
                      <BookOpen className="text-primary h-4 w-4" />
                      <span>사용자 가이드</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="pb-0">
                    {/* Docs 목차 - 스크롤 영역 */}
                    <DocsTOC />
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </div>
          </section>
        </nav>
      </div>

      {/* 하단 그룹 */}
      <div className="flex-shrink-0">
        <hr className="border-sidebar-border my-2" />
        <nav className="font-godoM flex flex-col">
          <NavButton icon={LogOut} onClick={handleLogout}>
            Log out
          </NavButton>
        </nav>
      </div>

      {/* 프로젝트 생성 모달 */}
      <ProjectCreateModal
        open={openCreate}
        onOpenChange={setOpenCreate}
        onCreate={createProject}
        onComplete={() => {
          setOpenCreate(false);
          // 현재 페이지가 메인이 아니면 메인으로 리다이렉트
          if (location.pathname !== ROUTE_PATH.MAIN) {
            navigate(ROUTE_PATH.MAIN);
          }
        }}
      />
    </aside>
  );
};

export default Sidebar;
