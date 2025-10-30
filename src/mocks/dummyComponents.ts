import type { ComponentListData } from '@/types/component';

export const DUMMY_COMPONENTS_DATA: ComponentListData = {
  projectId: 'pj_12345',
  components: [
    {
      id: 'comp_001',
      name: 'UserController',
      type: 'BE',
      layer: 'CONTROLLER',
      packageName: 'com.example.controller.UserController',
      technology: 'Spring Boot',
      dependencies: {
        upstreamCount: 1,
        downstreamCount: 3,
      },
      status: 'ACTIVE',
    },
    {
      id: 'comp_002',
      name: 'AuthController',
      type: 'BE',
      layer: 'CONTROLLER',
      packageName: 'com.example.controller.AuthController',
      technology: 'Spring Boot',
      dependencies: {
        upstreamCount: 1,
        downstreamCount: 2,
      },
      status: 'ACTIVE',
    },
    {
      id: 'comp_003',
      name: 'OrderController',
      type: 'BE',
      layer: 'CONTROLLER',
      packageName: 'com.example.controller.OrderController',
      technology: 'Spring Boot',
      dependencies: {
        upstreamCount: 1,
        downstreamCount: 4,
      },
      status: 'WARNING',
    },
    {
      id: 'comp_004',
      name: 'UserService',
      type: 'BE',
      layer: 'SERVICE',
      packageName: 'com.example.service.UserService',
      technology: 'Spring Boot',
      dependencies: {
        upstreamCount: 2,
        downstreamCount: 2,
      },
      status: 'ACTIVE',
    },
    {
      id: 'comp_005',
      name: 'UserRepository',
      type: 'BE',
      layer: 'REPOSITORY',
      packageName: 'com.example.repository.UserRepository',
      technology: 'Spring Data JPA',
      dependencies: {
        upstreamCount: 3,
        downstreamCount: 1,
      },
      status: 'ACTIVE',
    },
  ],
  pagination: {
    limit: 50,
    offset: 0,
    total: 5,
    hasNext: false,
  },
  summary: {
    totalComponents: 5,
    byType: {
      FRONTEND: 0,
      BE: 5,
      INFRA: 0,
      EXTERNAL: 0,
    },
    byLayer: {
      CONTROLLER: 3,
      SERVICE: 1,
      REPOSITORY: 1,
      VALIDATOR: 0,
    },
    byStatus: {
      ACTIVE: 4,
      WARNING: 1,
      ERROR: 0,
    },
  },
};
