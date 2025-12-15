# CodeDuel MVP - Quick Start

A real-time competitive coding platform where users race to solve Codeforces problems!

## 🚀 Quick Deploy (Production)

Full deployment guide: [DEPLOYMENT.md](./DEPLOYMENT.md)

**TL;DR:**
1. Push to GitHub
2. Deploy frontend to [Vercel](https://vercel.com)
3. Deploy backend to [Railway](https://railway.app)
4. Set environment variables
5. Done!

## 🏃 Run Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- Node.js 18+

### Backend
```bash
cd backend
mvn spring-boot:run
```
Server runs on http://localhost:8080

### Frontend
```bash
cd frontend
npm install
npm run dev
```  
UI available at http://localhost:5173

## 📖 How It Works

1. **Create a room** with your Codeforces handle
2. **Share Room ID** with friends
3. **Host selects** problem difficulty
4. **Race begins!** First to solve wins
5. **Winner detected** automatically via Codeforces API

## 🏗️ Architecture

- **Frontend:** React + Vite + Tailwind CSS
- **Backend:** Spring Boot + WebSocket (STOMP)
- **Real-time:** WebSocket for live updates
- **Integration:** Codeforces API for problems & submissions

## 📁 Project Structure

```
CodeDuel/
├── backend/          # Spring Boot backend
│   ├── src/main/java/com/coderace/
│   │   ├── config/   # CORS, WebSocket, retry config
│   │   ├── controller/  # REST & WebSocket endpoints
│   │   ├── service/  # Game logic & Codeforces API
│   │   ├── model/    # Room, User, Problem
│   │   └── dto/      # Request/Response objects
│   └── pom.xml
├── frontend/         # React frontend  
│   ├── src/
│   │   ├── components/  # React components
│   │   ├── services/    # API & WebSocket services
│   │   └── index.css    # Tailwind styles
│   └── package.json
└── DEPLOYMENT.md    # Full deployment guide
```

## 🔧 Recent Fixes

✅ RestTemplate bean configuration  
✅ Circular dependency resolution  
✅ API retry logic with exponential backoff  
✅ Race condition fix in winner detection  
✅ WebSocket cleanup improvements  
✅ Environment configuration support  
✅ SockJS compatibility fix for Vite  

See [BUILD_ERRORS_ANALYSIS.md](./BUILD_ERRORS_ANALYSIS.md) for details.

## 🌐 Production URLs

After deployment:
- Frontend: `https://your-app.vercel.app`
- Backend: `https://your-backend.railway.app`

## 📝 License

This is a personal project for learning purposes.
