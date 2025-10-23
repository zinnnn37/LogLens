import { Outlet } from 'react-router-dom';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';

const Layout = () => {
  return (
    <div className="bg-background flex h-screen">
      {/* 사이드바 */}
      <Sidebar />

      <div className="flex flex-1 flex-col">
        {/* 헤더 */}
        <Header />

        {/* 메인 콘텐츠 영역 */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
