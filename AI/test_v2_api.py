"""
Test script for V2 Chatbot API
"""
import asyncio
import sys
import os
from pathlib import Path

# Load environment variables from .env.test
from dotenv import load_dotenv
env_path = Path(__file__).parent / '.env.test'
load_dotenv(env_path)

from app.services.chatbot_service_v2 import chatbot_service_v2

async def test_v2_chatbot():
    """Test V2 chatbot with a simple question"""

    print("🧪 Testing V2 Chatbot API")
    print("=" * 60)

    # Test case 1: Simple statistics question
    test_question = "최근 1시간 동안 발생한 로그 통계를 알려줘"
    project_uuid = "test-project"

    print(f"\n📝 Question: {test_question}")
    print(f"🏢 Project UUID: {project_uuid}")
    print("\n⏳ Calling V2 Agent...\n")

    try:
        result = await chatbot_service_v2.ask(
            question=test_question,
            project_uuid=project_uuid,
            chat_history=None
        )

        print("✅ SUCCESS!")
        print(f"\n💬 Answer:\n{result.answer}")
        print(f"\n📊 Metadata:")
        print(f"   - from_cache: {result.from_cache}")
        print(f"   - related_logs count: {len(result.related_logs)}")
        print(f"   - answered_at: {result.answered_at}")

        return True

    except Exception as e:
        print(f"❌ ERROR: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = asyncio.run(test_v2_chatbot())
    sys.exit(0 if success else 1)
