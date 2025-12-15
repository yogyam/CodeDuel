# CodeDuel 🏁

A real-time competitive coding platform where users race to solve Codeforces problems!

**Live Demo:** Deploy to Railway (instructions below)

---

## 🚀 Quick Deploy to Railway

Deploy both frontend and backend to Railway in ~10 minutes.

### Prerequisites
- GitHub account with this repo
- [Railway account](https://railway.app) (free)

### Backend Deployment

1. **New Project** on Railway → Deploy from GitHub → Select this repo
2. **Settings** → Root Directory: `/backend`
3. **Variables** → Add: `CORS_ALLOWED_ORIGINS=http://localhost:3000` (update later)
4. Wait for build (~2 min) → Copy backend URL

### Frontend Deployment

1. Same project → **+ New** → GitHub Repo → Select this repo again
2. **Settings** → Root Directory: `/frontend`
3. **Settings** → Build Command: `npm install && npm run build`
4. **Settings** → Start Command: `npx serve -s dist -l 3000`
5. **Settings** → Port: `3000`
6. **Variables** → Add: `VITE_BACKEND_URL=https://YOUR-BACKEND-URL.railway.app`
7. Wait for build (~3 min) → Copy frontend URL

### Connect Services

1. Go back to backend → **Variables** → Update `CORS_ALLOWED_ORIGINS` to frontend URL
2. Done! Visit your frontend URL 🎉

---

## 🏃 Run Locally

### Backend
```bash
cd backend
mvn spring-boot:run
```
Runs on http://localhost:8080

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on http://localhost:5173

---

## 📖 How It Works

1. **Create Room** with your Codeforces handle
2. **Share Room ID** with friends  
3. **Select Difficulty** (800-3500 rating)
4. **Race to solve!** First submission wins
5. **Auto-detection** via Codeforces API

---

## 🏗️ Tech Stack

**Frontend:**
- React + Vite
- Tailwind CSS
- WebSocket (SockJS + STOMP)
- Axios for HTTP

**Backend:**
- Spring Boot
- WebSocket (STOMP)
- Codeforces API integration
- In-memory game state

---

## 📁 Project Structure

```
CodeDuel/
├── backend/              # Spring Boot backend
│   ├── src/main/java/com/coderace/
│   │   ├── config/      # CORS, WebSocket, Retry
│   │   ├── controller/  # REST & WebSocket endpoints
│   │   ├── service/     # Game logic & Codeforces API
│   │   ├── model/       # GameRoom, User, Problem
│   │   └── dto/         # Request/Response objects
│   └── pom.xml
│
├── frontend/            # React frontend
│   ├── src/
│   │   ├── components/  # LandingPage, GameRoom
│   │   ├── services/    # API & WebSocket services
│   │   └── index.css    # Tailwind styles
│   └── package.json
│
└── README.md           # You are here!
```

---

## 🔧 Key Features Implemented

✅ Room creation & joining  
✅ Real-time WebSocket updates  
✅ Codeforces API integration with retry logic  
✅ Winner detection via submission polling  
✅ Race condition prevention  
✅ CORS configuration for production  
✅ Environment variable support  

---

## 🐛 Troubleshooting

### "Failed to create room"
- Check browser console for CORS errors
- Verify `CORS_ALLOWED_ORIGINS` matches frontend URL
- Redeploy backend after changing variables

### Frontend shows 404
- Verify `VITE_BACKEND_URL` includes `https://`
- Redeploy frontend after adding environment variables

### WebSocket won't connect
- Check backend logs for errors
- Try hard refresh (Cmd+Shift+R)
- Verify backend is online

---

## 💰 Railway Free Tier

- $5 credit/month (~500 hours)
- Auto-sleep after 30 min inactivity
- First request wakes service (~10s)

**Monitor usage:** Railway Dashboard → Metrics

---

## 🚧 Future Enhancements

- [ ] Database persistence (PostgreSQL)
- [ ] User authentication
- [ ] Leaderboards
- [ ] Multiple simultaneous rooms
- [ ] Room expiration cleanup
- [ ] Custom problem sets

---

## 📝 License

Personal learning project - feel free to fork and modify!

---

## 🤝 Contributing

This is a personal project, but suggestions welcome via issues!

---

**Built with ❤️ for competitive programmers**

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
