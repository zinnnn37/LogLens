import { Outlet } from 'react-router-dom';

const Layout = () => {
  return (
    <div>
      <header>
        <nav>{/* 네비게이션 추가 예정 */}</nav>
      </header>
      <main>
        <Outlet />
      </main>
      <footer>{/* 푸터 추가 예정 */}</footer>
    </div>
  );
};

export default Layout;
