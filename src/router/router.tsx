import { createBrowserRouter } from 'react-router-dom';

import Layout from '@/components/layout/Layout';
import Home from '@/pages/Home';
import NotFound from '@/pages/NotFound';
import { ROUTE_PATH } from '@/router/route-path';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';
import RequestFlowPage from '@/pages/RequestFlowPage';
import LogsPage from '@/pages/LogsPage';
import DashboardPage from '@/pages/DashBoradPage';
import DependencyGraphPage from '@/pages/DependencyGraphPage';
import ChatbotPage from '@/pages/ChatBotPage';

export const router = createBrowserRouter([
  // Layout 적용할 페이지 그룹
  {
    element: <Layout />,
    children: [
      {
        path: '/',
        element: <Home />,
      },
      {
        path: ROUTE_PATH.NOT_FOUND,
        element: <NotFound />,
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
    ],
  },

  // Layout 미적용 그룹
  {
    path: ROUTE_PATH.LOGIN,
    element: <LoginPage />,
  },
  {
    path: ROUTE_PATH.SIGNUP,
    element: <SignupPage />,
  },
]);
