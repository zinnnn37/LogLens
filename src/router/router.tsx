import { createBrowserRouter } from 'react-router-dom';

import Layout from '@/components/layout/Layout';
import Home from '@/pages/Home';
import Login from '@/pages/Login';
import NotFound from '@/pages/NotFound';
import { ROUTE_PATH } from '@/router/route-path';
import LoginPage from '@/pages/LoginPage';
import SignupPage from '@/pages/SignupPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: ROUTE_PATH.LOGIN,
        element: <Login />,
      },
      {
        path: ROUTE_PATH.NOT_FOUND,
        element: <NotFound />,
      },
      {
        path: ROUTE_PATH.LOGIN,
        element: <LoginPage />,
      },
      {
        path: ROUTE_PATH.SIGNUP,
        element: <SignupPage />,
      },
    ],
  },
]);
