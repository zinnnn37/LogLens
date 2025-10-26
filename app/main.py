"""
FastAPI application entry point
"""

import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.v1 import router as v1_router
from app.consumers.log_consumer import log_consumer


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

    # Start Kafka consumer in background
    consumer_task = asyncio.create_task(log_consumer.start())
    print("✅ Kafka consumer task started")

    yield

    # Shutdown
    print("🛑 Shutting down...")
    log_consumer.stop()
    try:
        await asyncio.wait_for(consumer_task, timeout=5.0)
    except asyncio.TimeoutError:
        consumer_task.cancel()
    print("✅ Shutdown complete")


# Create FastAPI app
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="AI-based log analysis system with LangChain and OpenSearch",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
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
