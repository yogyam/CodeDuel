#!/bin/bash

echo "🏁 CodeRace - Quick Start Script"
echo "================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6+."
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+."
    exit 1
fi

echo "✅ All prerequisites found!"
echo ""

# Start backend
echo "🚀 Starting Backend..."
cd backend
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    echo "✅ Backend build successful!"
    mvn spring-boot:run &
    BACKEND_PID=$!
    echo "Backend PID: $BACKEND_PID"
else
    echo "❌ Backend build failed!"
    exit 1
fi

cd ..

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 10

# Start frontend
echo "🚀 Starting Frontend..."
cd frontend
npm install
if [ $? -eq 0 ]; then
    echo "✅ Frontend dependencies installed!"
    npm run dev &
    FRONTEND_PID=$!
    echo "Frontend PID: $FRONTEND_PID"
else
    echo "❌ Frontend setup failed!"
    kill $BACKEND_PID
    exit 1
fi

cd ..

echo ""
echo "================================"
echo "🎉 CodeRace is now running!"
echo "================================"
echo ""
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop all servers"
echo ""

# Wait for user interrupt
wait
