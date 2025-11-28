"""
FastAPI application entry point
"""

import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.v1 import router as v1_router
from app.api.v2 import router as v2_router
from app.api.v2_langgraph.logs import router as v2_langgraph_router
from app.api.v2_langgraph.analysis import router as v2_langgraph_analysis_router
from app.api.v2_langgraph.statistics import router as v2_langgraph_statistics_router
from app.api.v2_langgraph.experiments import router as v2_langgraph_experiments_router


# Global scheduler task reference
_scheduler_task = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager

    Handles startup and shutdown tasks
    """
    global _scheduler_task

    # Startup
    print(f"🚀 Starting {settings.APP_NAME} v{settings.APP_VERSION}")
    print(f"📊 Environment: {settings.ENVIRONMENT}")

    # Initialize OpenSearch indices
    try:
        print("🔧 Checking OpenSearch indices...")
        from scripts.create_indices import create_qa_cache_index

        # NOTE: Log indices are now managed externally in the new format:
        # {project_uuid_with_underscores}_{YYYY}_{MM}
        # Example: 3a73c7d4_8176_3929_b72f_d5b921daae67_2025_11
        # No need to create log indices here - they are pre-created
        print("📋 Log indices are managed externally (new format: {uuid}_{YYYY}_{MM})")

        # Create QA cache index only
        qa_success = create_qa_cache_index()

        if qa_success:
            print("✅ OpenSearch indices ready")
        else:
            print("⚠️ QA cache index may already exist or failed to create")
            print("   (This is normal if index was created previously)")
    except Exception as e:
        print(f"⚠️ OpenSearch indices check failed: {e}")
        print("   Application will continue, but some features may not work")

    # Start Periodic Enrichment Scheduler for ERROR log vectorization
    try:
        print("🔧 Starting Periodic Enrichment Scheduler...")
        import sys
        import os

        # Add project root to path for periodic_enrichment_scheduler import
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        if project_root not in sys.path:
            sys.path.insert(0, project_root)

        from periodic_enrichment_scheduler import PeriodicEnrichmentScheduler

        scheduler = PeriodicEnrichmentScheduler()

        async def run_scheduler():
            """Run scheduler as background task"""
            try:
                await scheduler.start()
            except asyncio.CancelledError:
                print("⚠️ Scheduler task cancelled")
            except Exception as e:
                print(f"🔴 Scheduler error: {e}")

        # Start scheduler as background task
        _scheduler_task = asyncio.create_task(run_scheduler())
        print("✅ Periodic Enrichment Scheduler started in background")
        print("   ERROR logs will be automatically vectorized every 10 seconds")

    except ImportError as e:
        print(f"⚠️ Periodic Enrichment Scheduler not available: {e}")
        print("   ERROR logs will not be auto-vectorized (run scheduler manually)")
    except Exception as e:
        print(f"⚠️ Failed to start Periodic Enrichment Scheduler: {e}")
        print("   ERROR logs will not be auto-vectorized")

    yield

    # Shutdown
    print("🛑 Shutting down...")

    # Stop scheduler if running
    if _scheduler_task is not None:
        print("🔧 Stopping Periodic Enrichment Scheduler...")
        _scheduler_task.cancel()
        try:
            await _scheduler_task
        except asyncio.CancelledError:
            pass
        print("✅ Scheduler stopped")

    print("✅ Shutdown complete")


# OpenAPI tags metadata (한국어)
tags_metadata = [
    {
        "name": "health",
        "description": "애플리케이션 상태 확인 및 서비스 연결 상태 모니터링",
    },
    {
        "name": "logs",
        "description": "AI 기반 로그 분석 - GPT-4o mini를 활용한 근본 원인 분석(RCA), Trace 기반 분석, 캐싱 최적화",
    },
    {
        "name": "chatbot",
        "description": "RAG 기반 대화형 로그 분석 - 자연어로 로그 검색 및 질문, 대화 히스토리 지원, 스트리밍 응답",
    },
    {
        "name": "Chatbot V2 (Agent)",
        "description": "ReAct Agent 기반 챗봇 - LLM이 자율적으로 도구를 선택하여 로그 분석 (시스템 통계, 에러 분석, 상세 조회 등)",
    },
    {
        "name": "Log Analysis V2 (LangGraph)",
        "description": "LangGraph 기반 로그 분석 - 구조화된 워크플로우, 3-tier 캐싱, 동적 전략 선택, 검증 로직",
    },
    {
        "name": "Analysis Documents V2",
        "description": "HTML 문서 생성 - Jinja2 템플릿 기반 프로젝트/에러 분석 HTML 문서 생성, Chart.js 차트 렌더링",
    },
    {
        "name": "Statistics Comparison",
        "description": "AI vs DB 통계 비교 - LLM 기반 통계 추론과 DB 직접 조회의 정확도 검증, AI의 DB 대체 역량 증명",
    },
    {
        "name": "Vector AI Experiments",
        "description": "Vector AI 실험 - Vector KNN 검색 + LLM 추론이 OpenSearch 집계를 대체할 수 있는지 검증, k 값별 정확도/성능 비교",
    },
]

# Create FastAPI app
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="""
## AI 기반 로그 분석 시스템

LangChain과 OpenSearch를 활용한 실시간 로그 분석 및 대화형 검색 시스템입니다.

### 주요 기능

- 🔍 **AI 로그 분석**: GPT-4o mini를 활용한 자동 근본 원인 분석(RCA)
- 💬 **RAG 챗봇**: 자연어로 로그 검색 및 질문 응답
- ⚡ **고성능 캐싱**: Trace 기반 캐싱으로 97-99% 비용 절감
- 🔒 **멀티테넌시**: project_uuid 기반 완전한 데이터 격리
- 📊 **Vector 검색**: OpenSearch KNN을 활용한 유사 로그 검색

### 기술 스택

- **Framework**: FastAPI
- **AI**: LangChain + OpenAI GPT-4o mini
- **Storage**: OpenSearch (로그 저장 + Vector DB)
- **Embedding**: text-embedding-3-large (1536차원)
    """,
    openapi_tags=tags_metadata,
    lifespan=lifespan,
    contact={
        "name": "AI Team",
        "email": "support@example.com",
    },
    servers=[
        {
            "url": "https://ai.loglens.store",
            "description": "프로덕션 환경 (EC2)",
        },
        {
            "url": "http://localhost:8000",
            "description": "로컬 개발 환경",
        },
        {
            "url": "http://localhost:8001",
            "description": "로컬 테스트 환경 (Blue-Green)",
        },
    ],
)

# CORS middleware
# Note: allow_origins=["*"] with allow_credentials=True is not allowed by browsers
# Use specific origins list from settings
cors_origins = settings.cors_origins_list
allow_credentials = cors_origins != ["*"]  # Only allow credentials if not using wildcard

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routes
app.include_router(v1_router, prefix="/api/v1")
app.include_router(v2_router, prefix="/api/v2")
app.include_router(v2_langgraph_router, prefix="/api")
app.include_router(v2_langgraph_analysis_router, prefix="/api")
app.include_router(v2_langgraph_statistics_router, prefix="/api")
app.include_router(v2_langgraph_experiments_router, prefix="/api")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "health": "/api/v1/health",
        "apis": {
            "v1": "/api/v1",
            "v2": "/api/v2 (Chatbot Agent 기반)",
            "v2-langgraph": "/api/v2-langgraph (Log Analysis LangGraph 기반)",
            "v2-langgraph-analysis": "/api/v2-langgraph/analysis (HTML Document 생성)",
            "v2-langgraph-statistics": "/api/v2-langgraph/statistics (AI vs DB 통계 비교)",
            "v2-langgraph-experiments": "/api/v2-langgraph/experiments (Vector AI vs DB 실험)"
        }
    }
