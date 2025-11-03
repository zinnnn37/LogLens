import { createBrowserRouter } from 'react-router-dom';

import Layout from '@/components/layout/Layout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import NotFound from '@/pages/NotFound';
import { ROUTE_PATH } from '@/router/route-path';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import MainPage from '@/pages/MainPage';
import RequestFlowPage from '@/pages/RequestFlowPage';
import LogsPage from '@/pages/LogsPage';
import DashboardPage from '@/pages/DashboradPage';
import DependencyGraphPage from '@/pages/DependencyGraphPage';
import ChatbotPage from '@/pages/ChatbotPage';
import Docs from '@/pages/Docs';

export const router = createBrowserRouter([
  // 인증이 필요한 페이지들
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          {
            path: ROUTE_PATH.NOT_FOUND,
            element: <NotFound />,
          },
          {
            path: ROUTE_PATH.MAIN,
            element: <MainPage />,
          },
          {
            path: ROUTE_PATH.LOGS,
            element: <LogsPage />,
          },
          {
            path: ROUTE_PATH.DASHBOARD,
            element: <DashboardPage />,
          },
          {
            path: ROUTE_PATH.DEPENDENCY_GRAPH,
            element: <DependencyGraphPage />,
          },
          {
            path: ROUTE_PATH.REQUEST_FLOW,
            element: <RequestFlowPage />,
          },
          {
            path: ROUTE_PATH.AI_CHAT,
            element: <ChatbotPage />,
          },
          {
            path: ROUTE_PATH.DOCS,
            element: <Docs />,
          },
        ],
      },
    ],
  },

  // 인증이 필요 없는 페이지들
  {
    path: ROUTE_PATH.LOGIN,
    element: <LoginPage />,
  },
  {
    path: ROUTE_PATH.SIGNUP,
    element: <SignupPage />,
  },
]);
