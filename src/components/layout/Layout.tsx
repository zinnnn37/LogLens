import { Outlet } from "react-router-dom";
import Sidebar from "@/components/Sidebar";
import Header from "@/components/Header";

const Layout = () => {
  return (
    <div className="flex h-screen bg-background">
      {/* 사이드바 */}
      <Sidebar />

      <div className="flex flex-1 flex-col">
        {/* 헤더 */}
        <Header />

        {/* 메인 콘텐츠 영역 */}
        <main className="flex-1 p-6 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
