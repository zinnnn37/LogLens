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
    },
    {
      id: 'comp_002',
      name: 'AuthController',
      type: 'BE',
      layer: 'CONTROLLER',
      packageName: 'com.example.controller.AuthController',
      technology: 'Spring Boot',
    },
    {
      id: 'comp_003',
      name: 'OrderController',
      type: 'BE',
      layer: 'CONTROLLER',
      packageName: 'com.example.controller.OrderController',
      technology: 'Spring Boot',
    },
    {
      id: 'comp_004',
      name: 'UserService',
      type: 'BE',
      layer: 'SERVICE',
      packageName: 'com.example.service.UserService',
      technology: 'Spring Boot',
    },
    {
      id: 'comp_005',
      name: 'UserRepository',
      type: 'BE',
      layer: 'REPOSITORY',
      packageName: 'com.example.repository.UserRepository',
      technology: 'Spring Data JPA',
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
      BE: 0,
      BACKEND: 5,
      INFRA: 0,
      EXTERNAL: 0,
    },
    byLayer: {
      PRESENTATION: 0,
      CONTROLLER: 3,
      SERVICE: 1,
      REPOSITORY: 1,
      VALIDATOR: 0,
    },
  },
};
