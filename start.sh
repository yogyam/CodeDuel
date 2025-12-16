#!/bin/bash

# CodeDuel - Complete Application Startup Script
# This script starts PostgreSQL, Backend, and Frontend services

set -e  # Exit on error

echo "🚀 Starting CodeDuel Application..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if PostgreSQL is running
echo -e "${BLUE}1. Checking PostgreSQL...${NC}"
if pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL is running${NC}"
else
    echo -e "${RED}✗ PostgreSQL is not running${NC}"
    echo "Please start PostgreSQL manually:"
    echo "  brew services start postgresql@14"
    exit 1
fi
echo ""

# Check if database exists
echo -e "${BLUE}2. Checking database...${NC}"
if psql -U postgres -lqt | cut -d \| -f 1 | grep -qw codeduel; then
    echo -e "${GREEN}✓ Database 'codeduel' exists${NC}"
else
    echo -e "${BLUE}Creating database 'codeduel'...${NC}"
    createdb -U postgres codeduel
    echo -e "${GREEN}✓ Database created${NC}"
fi
echo ""

# Create logs directory
mkdir -p logs

# Start Backend
echo -e "${BLUE}3. Starting Backend (Spring Boot)...${NC}"
cd backend

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}Error: backend/.env file not found!${NC}"
    echo "Please create it with required variables"
    exit 1
fi

# Load environment variables and start backend
export $(grep -v '^#' .env | grep -v '^$' | xargs)
echo -e "${GREEN}✓ Environment variables loaded${NC}"
echo -e "${BLUE}Starting Spring Boot...${NC}"

nohup mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo -e "${GREEN}✓ Backend starting (PID: $BACKEND_PID)${NC}"

# Wait for backend
echo -e "${BLUE}Waiting for backend...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/api/game/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend is ready!${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Backend failed to start${NC}"
        exit 1
    fi
    echo -n "."
    sleep 1
done
echo ""

cd ..

# Start Frontend  
echo -e "${BLUE}4. Starting Frontend (Vite)...${NC}"
cd frontend

if [ ! -d "node_modules" ]; then
    echo -e "${BLUE}Installing dependencies...${NC}"
    npm install
fi

nohup npm run dev -- --host 0.0.0.0 > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo -e "${GREEN}✓ Frontend starting (PID: $FRONTEND_PID)${NC}"

# Wait for frontend
echo -e "${BLUE}Waiting for frontend...${NC}"
for i in {1..20}; do
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Frontend is ready!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

cd ..
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✓ All services started!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080"
echo ""
