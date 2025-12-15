# CodeDuel 🏁

A real-time competitive coding platform where users race to solve Codeforces problems!

**Live Demo:** Deploy to Railway (instructions below)

---

## 🚀 Deploy to Railway (From Scratch)

Follow these steps to deploy CodeDuel to Railway in 15 minutes.

---

### Step 1: Prepare Repository

Make sure your code is pushed to GitHub:
```bash
git add .
git commit -m "Ready for deployment"
git push origin main
```

---

### Step 2: Deploy Backend

1. Go to [Railway Dashboard](https://railway.app/dashboard)
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Choose **yogyam/CodeDuel** repository
5. Railway will auto-detect Spring Boot app

**Configure Backend:**
- Click on the service card
- Go to **Settings** tab
- **Root Directory**: Set to `/backend`
- **Networking**: Port should be `8080` (auto-detected)
- Click **Variables** tab
- Add variable `CORS_ALLOWED_ORIGINS` = `http://localhost:3000` (temporary)

**Wait for deployment** (~2-3 minutes)

**Get Backend URL:**
- Go to **Settings** → **Networking** → **Public Networking**
- Copy the domain (e.g., `codeduel-backend-xyz.up.railway.app`)
- **SAVE THIS URL** - you'll need it!

**Test backend:**
Visit `https://YOUR-BACKEND-URL.railway.app/api/game/health`
Should return: `{"service":"CodeRace Backend","status":"UP"}`

---

### Step 3: Deploy Frontend

**In the same Railway project:**

1. Click **"+ New"** button
2. Select **"GitHub Repo"**
3. Choose **yogyam/CodeDuel** again (same repo!)
4. Railway creates a second service

**Configure Frontend:**
- Click on the new service card
- Go to **Settings** tab
- **Root Directory**: `/frontend`
- Scroll down to **Deploy** section:
  - **Build Command**: `npm install && npm run build`
  - **Start Command**: `npx serve -s dist -l 3000`
- **Networking** → **Port**: `3000`

**Add Environment Variable:**
- Click **Variables** tab
- Add: `VITE_BACKEND_URL` = `https://YOUR-BACKEND-URL.railway.app`
  - ⚠️ Replace with your actual backend URL from Step 2
  - ⚠️ Must include `https://`!

**Wait for deployment** (~3-5 minutes)

**Get Frontend URL:**
- **Settings** → **Networking** → Copy public domain
- Example: `codeduel-frontend-abc.railway.app`

---

### Step 4: Connect Frontend & Backend

**Update Backend CORS:**
1. Go to **backend service** (first service you created)
2. Click **Variables** tab
3. Edit `CORS_ALLOWED_ORIGINS`
4. Change to: `https://YOUR-FRONTEND-URL.railway.app`
   - ⚠️ Use the exact frontend URL from Step 3
   - ⚠️ Must include `https://`!
5. Save - backend will auto-redeploy (~1 minute)

---

### Step 5: Test Your App! 🎉

1. Visit your frontend URL
2. Enter a Codeforces handle (try "tourist")
3. Click **"Create New Room"**
4. You should see a Room ID!
5. Open incognito window, join with the Room ID
6. Start a game and verify everything works

**If it works: Congratulations! � Your app is live!**

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
