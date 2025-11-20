import { QueryClient } from '@tanstack/react-query';

import { ApiError } from '@/types/api';

const getErrorStatus = (error: unknown): number | undefined => {
  if (error instanceof ApiError) {
    return error.axiosError.response?.status || error.response?.status;
  }
  return undefined;
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: (error: unknown) => {
        const status = getErrorStatus(error);
        if (status && status >= 400 && status < 500) {
          return false;
        }
        return true;
      },
      staleTime: 1000 * 60 * 5,
      retry: false,
    },
    mutations: {
      throwOnError: (error: unknown) => {
        const status = getErrorStatus(error);
        if (status && status >= 400 && status < 500) {
          return false;
        }
        return true;
      },
      retry: false,
    },
  },
});
