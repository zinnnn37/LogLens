"""
FastAPI application entry point
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.v1 import router as v1_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager

    Handles startup and shutdown tasks
    """
    # Startup
    print(f"🚀 Starting {settings.APP_NAME} v{settings.APP_VERSION}")
    print(f"📊 Environment: {settings.ENVIRONMENT}")

    # Initialize OpenSearch indices
    try:
        print("🔧 Checking OpenSearch indices...")
        from scripts.create_indices import create_logs_index_template, create_qa_cache_index

        # Create indices if they don't exist
        logs_success = create_logs_index_template()
        qa_success = create_qa_cache_index()

        if logs_success and qa_success:
            print("✅ OpenSearch indices ready")
        else:
            print("⚠️ Some indices may already exist or failed to create")
            print("   (This is normal if indices were created previously)")
    except Exception as e:
        print(f"⚠️ OpenSearch indices check failed: {e}")
        print("   Application will continue, but some features may not work")

    yield

    # Shutdown
    print("🛑 Shutting down...")
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


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "health": "/api/v1/health",
    }
