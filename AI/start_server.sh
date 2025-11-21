#!/bin/bash

# Load environment variables
set -a
source .env
set +a

# Activate virtual environment
source venv/bin/activate

# Start uvicorn server
uvicorn app.main:app --host 0.0.0.0 --port 8000
