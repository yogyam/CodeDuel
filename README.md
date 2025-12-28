# CodeDuel 🏁

> A real-time competitive coding platform where developers race to solve AI-generated programming challenges. Built with Spring Boot, React, and Google Gemini AI.

[![Live Demo](https://img.shields.io/badge/demo-live-success)](https://codeduel-frontend.railway.app)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

![CodeDuel Banner](https://img.shields.io/badge/Status-Production%20Ready-success)

---

## 🎯 What is CodeDuel?

CodeDuel is a **real-time multiplayer coding arena** where developers compete to solve algorithmic programming problems. Unlike traditional coding platforms, CodeDuel:

- **Generates unique problems on-demand** using Google Gemini AI
- **Executes code in real-time** across 7+ programming languages
- **Validates solutions instantly** with automated test cases
- **Supports multiplayer racing** with live WebSocket updates

Perfect for coding interviews, competitive programming practice, or friendly developer competitions.

---

## ✨ Key Features

### 🤖 AI-Powered Problem Generation
- **Google Gemini Integration**: Generates unique, high-quality coding problems on demand
- **Two-Step Generation**: Choose from 3 AI-generated title options, then generate full problem
- **8 Problem Categories**: Dynamic Programming, Graphs, Trees, Greedy, Binary Search, Sorting, Arrays/Hashing, Backtracking
- **4 Difficulty Levels**: Easy, Medium, Hard, Expert
- **Category Subtypes**: 40+ specific topics (e.g., "Knapsack Variants", "Shortest Path")
- **Automatic Test Case Generation**: AI creates comprehensive edge cases and validation tests
- **Skeleton Code Support**: Auto-generated starter code in Python, Java, C++, JavaScript

### 🎮 Real-Time Multiplayer
- **WebSocket Communication**: Instant updates using STOMP over WebSockets
- **Room-Based Gameplay**: Create/join rooms with unique 6-character IDs
- **Live Status Updates**: See opponents' progress in real-time (Waiting, Solving, Solved, Failed)
- **Host Controls**: Room creator selects problem filters and starts the game
- **Winner Detection**: Automatic verification and celebration on first correct submission

### 💻 Integrated Code Editor
- **Monaco Editor**: Full-featured code editor (same as VS Code)
- **7 Languages Supported**: Python, Java, C++, C, JavaScript, Go, Rust
- **Syntax Highlighting**: Language-specific color coding
- **Auto-Completion**: IntelliSense-style code suggestions
- **Error Detection**: Real-time syntax validation

### ⚡ Code Execution Engine
- **Piston API Integration**: Secure sandboxed code execution
- **Multi-Language Support**: Execute code in 7 popular languages
- **Test Case Validation**: Runs code against multiple test cases
- **Instant Feedback**: Returns output, errors, and pass/fail status
- **Security**: Sandboxed execution prevents malicious code

### 🔐 Authentication & Security
- **OAuth2 + Google Login**: Sign in with Google account
- **JWT Authentication**: Secure token-based auth with httpOnly cookies
- **Refresh Token Rotation**: Automatic token refresh with revocation
- **Rate Limiting**: Bucket4j-based API rate limiting (100 req/min per IP)
- **CSRF Protection**: WebSocket CSRF validation
- **XSS Prevention**: OWASP HTML sanitization for user inputs
- **Security Headers**: Custom security headers filter (X-Frame-Options, CSP, etc.)

### 💾 Data Persistence
- **PostgreSQL Database**: Production-ready relational data storage
- **JPA/Hibernate**: Type-safe database operations
- **Flyway Migrations**: Version-controlled schema management
- **Problem Caching**: In-memory caching for generated problems
- **User Caching**: Caffeine cache for user session management

### 🛡️ Production-Ready Features
- **Error Handling**: Global exception handling with custom error responses
- **Logging**: Comprehensive SLF4J logging throughout application
- **Health Checks**: Dedicated health endpoint for monitoring
- **CORS Configuration**: Configurable cross-origin support
- **Environment Variables**: Externalized configuration for secrets
- **Database Connection Pooling**: Optimized DB connections

---

## 🏗️ Technical Architecture

### Backend Stack
- **Framework**: Spring Boot 3.2.0 (Java 17)
- **Real-Time Communication**: Spring WebSocket + STOMP protocol
- **Security**: Spring Security + OAuth2 + JWT (jjwt 0.11.5)
- **Database**: PostgreSQL with Spring Data JPA
- **Migrations**: Flyway
- **AI Integration**: Google Gemini API (REST client)
- **Code Execution**: Piston API integration
- **Caching**: Caffeine (in-memory cache)
- **Rate Limiting**: Bucket4j
- **Validation**: Jakarta Validation
- **HTML Parsing**: Jsoup
- **XSS Protection**: OWASP Java HTML Sanitizer

### Frontend Stack
- **Framework**: React 18.2.0
- **Build Tool**: Vite 5.0.8
- **Routing**: React Router DOM 7.10.1
- **Styling**: Tailwind CSS + Custom CSS
- **Code Editor**: Monaco Editor (VS Code editor)
- **WebSocket**: STOMP.js + SockJS Client
- **HTTP Client**: Axios
- **XSS Protection**: DOMPurify

### External APIs
- **Google Gemini API**: AI-powered problem generation
- **Piston API**: Multi-language code execution engine

### Architecture Highlights
- **Microservices Ready**: Modular service-oriented design
- **WebSocket Architecture**: Bidirectional real-time communication
- **RESTful API**: Clean HTTP endpoints for CRUD operations
- **MVC Pattern**: Clear separation of concerns (Model-View-Controller)
- **Repository Pattern**: Data access abstraction layer
- **DTO Pattern**: Request/Response data transfer objects
- **Event-Driven**: WebSocket event broadcasting
- **Stateful Game Rooms**: In-memory ConcurrentHashMap for active games

---

## 📁 Project Structure

```
CodeDuel/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/com/coderace/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── CorsConfig.java       # CORS configuration
│   │   │   ├── RetryConfig.java      # Retry logic for API calls
│   │   │   ├── SecurityConfig.java   # Spring Security setup
│   │   │   └── WebSocketConfig.java  # WebSocket + STOMP config
│   │   ├── controller/               # REST & WebSocket endpoints
│   │   │   ├── AuthController.java         # Login, register, JWT refresh
│   │   │   ├── GameController.java         # Room creation, problem gen
│   │   │   ├── HealthController.java       # Health checks
│   │   │   └── WebSocketController.java    # Join, start, submit via WS
│   │   ├── service/                  # Business logic
│   │   │   ├── AuthService.java            # User authentication
│   │   │   ├── CodeExecutionService.java   # Piston API integration
│   │   │   ├── GameService.java            # Game room management
│   │   │   ├── ProblemGenerationService.java  # Gemini AI integration
│   │   │   ├── ProblemValidationService.java  # Test case validation
│   │   │   ├── RefreshTokenService.java    # Token rotation
│   │   │   └── UserCacheService.java       # User caching
│   │   ├── security/                 # Security filters & utilities
│   │   │   ├── JwtAuthenticationFilter.java   # JWT validation
│   │   │   ├── JwtUtil.java                   # JWT generation/parsing
│   │   │   ├── RateLimitFilter.java           # API rate limiting
│   │   │   ├── SecurityHeadersFilter.java     # Security headers
│   │   │   └── WebSocketAuthInterceptor.java  # WS authentication
│   │   ├── model/                    # Domain models
│   │   │   ├── GameRoom.java         # Game room entity
│   │   │   ├── Problem.java          # Problem entity
│   │   │   ├── ProblemCategory.java  # Category enum with subtypes
│   │   │   ├── TestCase.java         # Test case model
│   │   │   └── User.java             # User model
│   │   ├── entity/                   # JPA entities
│   │   │   └── User.java             # User database entity
│   │   ├── repository/               # Data access layer
│   │   │   ├── UserRepository.java
│   │   │   ├── ProblemRepository.java
│   │   │   └── RefreshTokenRepository.java
│   │   └── dto/                      # Data transfer objects
│   │       ├── CreateRoomRequest.java
│   │       ├── JoinRoomRequest.java
│   │       ├── ProblemFilter.java
│   │       ├── StartGameRequest.java
│   │       └── SubmitCodeRequest.java
│   ├── src/main/resources/
│   │   ├── application.properties    # App configuration
│   │   └── db/migration/             # Flyway SQL migrations
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React Frontend
│   ├── src/
│   │   ├── components/               # React components
│   │   │   ├── Dashboard.jsx         # User dashboard
│   │   │   ├── LoginPage.jsx         # Google OAuth login
│   │   │   ├── GameRoom.jsx          # Game room UI (main component)
│   │   │   ├── GameFilters.jsx       # Problem filter selection
│   │   │   ├── ProblemTitleSelector.jsx  # Title selection UI
│   │   │   ├── CodeEditor.jsx        # Monaco editor wrapper
│   │   │   ├── Navbar.jsx            # Navigation bar
│   │   │   └── ErrorBoundary.jsx     # Error handling
│   │   ├── services/                 # API clients
│   │   │   ├── api.js                # REST API service (Axios)
│   │   │   └── WebSocketService.js   # WebSocket service (STOMP)
│   │   ├── contexts/
│   │   │   └── AuthContext.jsx       # Authentication context
│   │   └── App.jsx                   # Main app & routing
│   ├── package.json                  # npm dependencies
│   └── vite.config.js                # Vite configuration
│
└── README.md                         # This file
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK)
- **Maven 3.6+**
- **Node.js 18+** & npm
- **PostgreSQL 15+** (local or cloud)

### 1️⃣ Clone Repository
```bash
git clone https://github.com/yogyam/CodeDuel.git
cd CodeDuel
```

### 2️⃣ Setup Backend

#### Configure Database
Create a PostgreSQL database:
```sql
CREATE DATABASE codeduel;
```

#### Set Environment Variables
Create `backend/.env` file:
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/codeduel
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password

# Google Gemini API
OPENAI_API_KEY=your_gemini_api_key
OPENAI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent

# JWT Secret (generate a secure random string)
JWT_SECRET=your_jwt_secret_key_here

# Google OAuth2
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your_google_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your_google_client_secret

# CORS (for development)
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

#### Run Backend
```bash
cd backend
mvn spring-boot:run
```

Server runs on **http://localhost:8080**

### 3️⃣ Setup Frontend

#### Install Dependencies
```bash
cd frontend
npm install
```

#### Configure Environment
Create `frontend/.env`:
```bash
VITE_BACKEND_URL=http://localhost:8080
```

#### Run Frontend
```bash
npm run dev
```

UI available at **http://localhost:5173**

### 4️⃣ Test the Application

1. **Open browser** to `http://localhost:5173`
2. **Sign in with Google** via OAuth2
3. **Create a new room** from the dashboard
4. **Share the Room ID** with friends (or open in incognito mode to test multiplayer)
5. **Select problem filters** (category, difficulty, subtype)
6. **Generate titles** and choose your favorite
7. **Start the game** and race to solve!

---

## 🎮 How to Play

### For the Host:
1. **Login** with your Google account
2. **Create a Room** - you'll get a unique 6-character room ID
3. **Wait for Players** to join using your room ID
4. **Select Problem Settings**:
   - Choose category (e.g., Dynamic Programming, Graphs)
   - Select difficulty (Easy, Medium, Hard, Expert)
   - Optionally pick a specific subtype (e.g., "Shortest Path")
5. **Generate Titles** - AI generates 3 problem title options
6. **Pick a Title** - Select your favorite and generate the full problem
7. **Start Game** - Begin the race!
8. **Code & Submit** - Write your solution and submit when ready

### For Participants:
1. **Login** with your Google account
2. **Join Room** - Enter the room ID shared by the host
3. **Wait** - Host will configure the problem
4. **Race** - Once started, solve as fast as you can!
5. **Submit** - Your code runs against test cases automatically
6. **Win** - First correct submission wins! 🎉

---

## 🔧 Deployment

### Deploy to Railway (Recommended)

#### Backend Deployment
1. Push code to GitHub
2. Go to [Railway Dashboard](https://railway.app)
3. **New Project** → **Deploy from GitHub**
4. Select `CodeDuel` repository
5. **Settings** → **Root Directory**: `/backend`
6. Add environment variables (see above)
7. Railway auto-detects Spring Boot and deploys

#### Frontend Deployment
1. Same Railway project → **+ New** → **GitHub Repo**
2. Select `CodeDuel` again (creates second service)
3. **Settings** → **Root Directory**: `/frontend`
4. **Build Command**: `npm install && npm run build`
5. **Start Command**: `npx serve -s dist -l 3000`
6. Add environment variable: `VITE_BACKEND_URL=https://your-backend.railway.app`

#### Update CORS
In backend service, update `CORS_ALLOWED_ORIGINS` to your frontend URL:
```
CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
```

### Alternative: Vercel (Frontend) + Railway (Backend)
- Deploy frontend to Vercel for optimal React performance
- Deploy backend to Railway or Heroku
- Configure CORS and environment variables accordingly

---

## 📊 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login with email/password
- `GET /api/auth/me` - Get current user
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - Logout (clear cookies)

### Game Management
- `POST /api/game/create-room` - Create new game room
- `GET /api/game/room/{roomId}` - Get room info
- `POST /api/game/generate-titles` - Generate 3 title options (AI)
- `POST /api/game/generate-problem-from-title` - Generate full problem (AI)
- `GET /api/game/health` - Health check

### WebSocket Endpoints
- `/app/game/{roomId}/join` - Join room
- `/app/game/{roomId}/start` - Start game (host only)
- `/app/game/{roomId}/submit` - Submit code
- `/topic/room/{roomId}` - Subscribe for room updates

---

## 🧠 Problem Categories & Subtypes

The AI can generate problems across **8 major categories** with **40+ specific subtypes**:

| Category | Subtypes |
|----------|----------|
| **Dynamic Programming** | Knapsack Variants, LCS, String DP, State Machine DP, 2D Grid DP, Interval DP |
| **Graphs** | Shortest Path, MST, Topological Sort, Connected Components, Cycle Detection, BFS/DFS |
| **Trees** | BST, Tree Traversal, LCA, Path Sum Problems, Tree DP |
| **Greedy** | Interval Scheduling, Activity Selection, Huffman Coding, Job Sequencing |
| **Binary Search** | Search in Sorted Array, Binary Search on Answer, Element Finding, Lower/Upper Bound |
| **Sorting & Searching** | Custom Comparators, Two Pointers, Sliding Window, Merge Sort Applications |
| **Arrays & Hashing** | Prefix Sum, Kadane's Algorithm, Hash Map Techniques, Subarray Problems |
| **Backtracking** | N-Queens Variants, Subset Generation, Permutations, Combination Sum |

---

## 🎨 Screenshots

> **Note**: Add screenshots of your deployed application here:
> - Landing page with Google login
> - Room creation interface
> - Problem filter selection
> - Title selection screen
> - Active game with Monaco editor
> - Winner celebration screen

---

## 🤝 Contributing

This is a personal portfolio project, but suggestions and feedback are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 🐛 Troubleshooting

### Backend won't start
- ✅ Verify PostgreSQL is running
- ✅ Check environment variables in `.env`
- ✅ Ensure Java 17+ is installed: `java -version`
- ✅ Check logs: `mvn spring-boot:run` will show errors

### Frontend can't connect
- ✅ Verify backend is running on port 8080
- ✅ Check `VITE_BACKEND_URL` in frontend `.env`
- ✅ Open browser console for CORS errors
- ✅ Check Network tab for failed API calls

### WebSocket connection fails
- ✅ Verify JWT token is valid (check cookies)
- ✅ Ensure CORS is configured for your frontend URL
- ✅ Check browser console for WebSocket errors
- ✅ Try hard refresh (Cmd/Ctrl + Shift + R)

### Problem generation fails
- ✅ Verify Gemini API key is valid
- ✅ Check API quota (Gemini free tier limits)
- ✅ Look for errors in backend logs
- ✅ Test API key with a simple curl request

---

## 📝 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

You are free to:
- ✅ Use this code for personal or commercial projects
- ✅ Modify and distribute
- ✅ Use in your portfolio

---

## 👤 Contact & Links

**Yogya Mehrotra**

- 📧 Email: [yogyamehrotra@gmail.com](mailto:yogyamehrotra@gmail.com)
- 💼 LinkedIn: [linkedin.com/in/yogyamehrotra](https://www.linkedin.com/in/yogyamehrotra)
- 🐙 GitHub: [github.com/yogyam](https://github.com/yogyam)
- 🌐 Portfolio: [your-portfolio-site.com](https://your-portfolio-site.com)

---

## 🙏 Acknowledgments

- **Google Gemini API** for AI-powered problem generation
- **Piston API** for secure code execution
- **Spring Boot** team for the excellent framework
- **Monaco Editor** for the VS Code editing experience
- **Railway** for seamless deployment platform
- **Competitive Programming Community** for inspiration

---

## 🔮 Future Enhancements

- [ ] Leaderboards with persistent rankings
- [ ] User profiles with solve statistics
- [ ] Private rooms with password protection
- [ ] Multiple simultaneous games per user
- [ ] Custom problem sets (user-created)
- [ ] Code playback (watch opponent's code in real-time)
- [ ] Voice/video chat integration
- [ ] Mobile app (React Native)
- [ ] Tournament brackets for multi-round competitions
- [ ] Problem difficulty rating based on solve times
- [ ] Integration with LeetCode/Codeforces APIs
- [ ] AI-powered hints system
- [ ] Code quality analysis (time/space complexity)

---

<div align="center">

**Built with ❤️ by a developer who loves competitive programming**

⭐ Star this repository if you find it useful!

</div>
