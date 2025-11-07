// src/App.tsx
import { RouterProvider } from 'react-router-dom';
import { router } from '@/router/router';
import { Toaster } from '@/components/ui/sonner';

const App = () => {
  return (
    <>
      <RouterProvider router={router} />
      <Toaster
        theme="system"
        position="top-center"
        richColors
        duration={1500}
      />
    </>
  );
};

export default App;
