#!/bin/bash

# CodeDuel - Stop all services

echo "🛑 Stopping CodeDuel services..."

# Kill backend
pkill -f "spring-boot:run" && echo "✓ Backend stopped"

# Kill frontend
pkill -f "vite" && echo "✓ Frontend stopped"

echo ""
echo "✓ All services stopped"
