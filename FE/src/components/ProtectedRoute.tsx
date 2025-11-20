import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { ROUTE_PATH } from '@/router/route-path';

/**
 * 인증이 필요한 라우트를 보호하는 컴포넌트
 * accessToken이 없으면 로그인 페이지로 리다이렉트
 */
export const ProtectedRoute = () => {
  const { accessToken } = useAuthStore();

  if (!accessToken) {
    return <Navigate to={ROUTE_PATH.LOGIN} replace />;
  }

  return <Outlet />;
};
