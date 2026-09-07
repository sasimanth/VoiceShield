FROM python:3.11-slim

WORKDIR /app

# Install system audio libraries (libsndfile1 for soundfile support)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

COPY ml_service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt --extra-index-url https://download.pytorch.org/whl/cpu

COPY ml/ ./ml/
COPY ml_service/ ./ml_service/

EXPOSE 8001

ENV PYTHONPATH=/app

CMD ["uvicorn", "ml_service.main:app", "--host", "0.0.0.0", "--port", "8001"]

