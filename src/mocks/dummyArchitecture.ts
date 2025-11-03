import type { ArchitectureData } from '@/types/architecture';

export const DUMMY_ARCHITECTURE_DATA: ArchitectureData = {
  projectId: 'pj_12345',
  period: {
    startDate: '2025-10-16T00:00:00.000Z',
    endDate: '2025-10-17T23:59:59.000Z',
  },
  nodes: [
    {
      id: 'frontend-main',
      name: 'Frontend App',
      type: 'FRONTEND',
      layer: 'PRESENTATION',
      technology: 'React',
    },
    {
      id: 'nginx-gateway',
      name: 'Nginx Gateway',
      type: 'INFRA',
      layer: 'GATEWAY',
      technology: 'Nginx',
    },
    {
      id: 'backend-api',
      name: 'Backend API',
      type: 'BACKEND',
      layer: 'APPLICATION',
      technology: 'Spring Boot',
    },
    {
      id: 'database-main',
      name: 'PostgreSQL DB',
      type: 'INFRA',
      layer: 'DATA',
      technology: 'PostgreSQL',
    },
  ],
  edges: [
    {
      source: 'frontend-main',
      target: 'nginx-gateway',
      relationship: 'HTTP_REQUEST',
    },
    {
      source: 'nginx-gateway',
      target: 'backend-api',
      relationship: 'HTTP_PROXY',
    },
    {
      source: 'backend-api',
      target: 'database-main',
      relationship: 'DATABASE_QUERY',
    },
  ],
};
