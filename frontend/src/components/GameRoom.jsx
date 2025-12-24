import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import webSocketService from '../services/WebSocketService';
import GameFilters from './GameFilters';
import CodeEditor from './CodeEditor';
import DOMPurify from 'dompurify';
import './GameRoom.css';

/**
 * Game Room Component
 * Displays the game room with users, problem, and status
 */
function GameRoom() {
  const { roomId } = useParams(); // Get roomId from URL
  const navigate = useNavigate();
  const { user, token } = useAuth(); // Get JWT token from auth context
  const codeforcesHandle = user?.codeforcesHandle || 'anonymous';
  const [gameState, setGameState] = useState({
    state: 'WAITING',
    users: [],
    problem: null,
    winnerId: null,
    message: ''
  });
  const [selectedRating, setSelectedRating] = useState(1200);
  const [gameFilters, setGameFilters] = useState({
    description: ''
  });
  const [isGenerating, setIsGenerating] = useState(false);
  const [generationStatus, setGenerationStatus] = useState('');

  // Derive isHost from gameState instead of separate state
  const isHost = useMemo(() => {
    const currentUser = gameState.users?.find(u => u.codeforcesHandle === codeforcesHandle);
    return currentUser?.host || false;
  }, [gameState.users, codeforcesHandle]);

  const [connected, setConnected] = useState(false);
  const [room, setRoom] = useState(null);
  const [currentProblem, setCurrentProblem] = useState(null);
  const [users, setUsers] = useState([]);
  const [code, setCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [timeElapsed, setTimeElapsed] = useState(0);
  const [isTimerRunning, setIsTimerRunning] = useState(false);

  // Difficulty options
  const difficultyOptions = [800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000];

  // Initialize WebSocket connection with JWT token
  useEffect(() => {
    webSocketService.connect(() => {
      console.log('Connected to WebSocket');
      setConnected(true); // Keep this to indicate connection status

      // Subscribe to room updates
      webSocketService.subscribe(`/topic/room/${roomId}`, (update) => {
        // Check if this is a generation status message
        if (update.type === 'GENERATION_STATUS') {
          setGenerationStatus(update.status);
          setIsGenerating(true);
        } else {
          // Normal game state update
          setGameState(update);
          // If problem is now loaded, stop generating
          if (update.problem) {
            setIsGenerating(false);
            setGenerationStatus('');
          }
        }
      });

      // Join the room
      webSocketService.send(`/app/game/${roomId}/join`, {
        roomId: roomId,
        codeforcesHandle: codeforcesHandle
      });
    });

    // Cleanup on unmount
    return () => {
      // Only unsubscribe if we're connected, otherwise the subscription might not exist
      if (webSocketService.isConnected()) {
        webSocketService.unsubscribe(`/topic/room/${roomId}`);
      }
      webSocketService.disconnect();
    };
  }, [roomId, codeforcesHandle]);

  /**
   * Starts the game (host only)
   */
  const handleStartGame = () => {
    if (!isHost) return;

    setIsGenerating(true);
    setGenerationStatus('Preparing to generate problem...');

    webSocketService.send(`/app/game/${roomId}/start`, {
      description: gameFilters.description
    });
  };

  const handleCodeSubmit = async (code, language) => {
    if (!gameState?.problem?.problemId) {
      alert('No problem loaded yet!');
      return;
    }

    console.log('Submitting code:', { language, codeLength: code.length });

    webSocketService.send(`/app/game/${roomId}/submit`, {
      code,
      language,
      problemId: gameState.problem.problemId
    });
  };

  /**
   * Gets badge class based on user status
   */
  const getUserStatusBadge = (status) => {
    switch (status) {
      case 'WAITING':
        return 'badge badge-waiting';
      case 'SOLVING':
        return 'badge badge-solving';
      case 'WON':
        return 'badge badge-won';
      default:
        return 'badge';
    }
  };

  /**
   * Renders the loading screen while AI generates problem
   */
  const renderLoadingScreen = () => (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 p-8 flex items-center justify-center">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-2xl p-8 text-center">
        {/* Animated Spinner */}
        <div className="mb-6 flex justify-center">
          <div className="w-20 h-20 border-8 border-blue-200 border-t-blue-600 rounded-full animate-spin"></div>
        </div>

        {/* Title */}
        <h2 className="text-3xl font-bold text-gray-800 mb-4">
          🤖 AI is Crafting Your Problem
        </h2>

        {/* Status Message */}
        <p className="text-lg text-gray-600 mb-6">
          {generationStatus || 'Preparing to generate...'}
        </p>

        {/* Progress Dots */}
        <div className="flex justify-center gap-2 mb-6">
          <div className="w-3 h-3 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '0s' }}></div>
          <div className="w-3 h-3 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
          <div className="w-3 h-3 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
        </div>

        {/* Info Text */}
        <p className="text-sm text-gray-500">
          This typically takes 5-15 seconds
        </p>
      </div>
    </div>
  );

  /**
   * Renders the waiting lobby
   */
  const renderWaitingLobby = () => (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold mb-2">Waiting for players...</h2>
        <p className="text-gray-600">Share this Room ID with your friends:</p>
        <div className="mt-3 flex items-center justify-center gap-2">
          <span className="text-3xl font-mono font-bold text-coderace-blue">
            {roomId}
          </span>
          <button
            onClick={() => navigator.clipboard.writeText(roomId)}
            className="btn-secondary py-2 px-4 text-sm"
          >
            Copy
          </button>
        </div>
      </div>

      {/* Users List */}
      <div className="card">
        <h3 className="font-semibold mb-3">Players ({gameState.users?.length || 0})</h3>
        <div className="space-y-2">
          {gameState.users?.map((user, index) => (
            <div
              key={index}
              className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-coderace-blue rounded-full flex items-center justify-center text-white font-bold">
                  {user.codeforcesHandle[0].toUpperCase()}
                </div>
                <div>
                  <div className="font-semibold">{user.codeforcesHandle}</div>
                  {user.host && (
                    <span className="text-xs text-coderace-blue">👑 Host</span>
                  )}
                </div>
              </div>
              <span className={getUserStatusBadge(user.status)}>
                {user.status}
              </span>
            </div>
          ))}
        </div>
      </div>


      {/* Game Filters (Host can edit, others view only) */}
      <GameFilters
        onFiltersChange={setGameFilters}
        isHost={isHost}
        initialFilters={gameFilters}
      />

      {/* Start Game Button (Host Only) */}
      {isHost && (
        <div className="card">
          <button
            className="btn-primary w-full"
            onClick={handleStartGame}
            disabled={(gameState.users?.length || 0) < 2}
          >
            {(gameState.users?.length || 0) < 2 ? 'Waiting for more players...' : 'Start Race!'}
          </button>
        </div>
      )}
    </div>
  );

  /**
   * Renders the active game
   */
  const renderActiveGame = () => (
    <div className="space-y-6">
      {/* Problem Card */}
      <div className="card bg-gradient-to-r from-blue-500 to-blue-600 text-white">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold mb-2">
              {gameState.problem?.name}
            </h2>
            <div className="flex gap-2 items-center">
              <span className="badge bg-white text-blue-600">
                Rating: {gameState.problem?.rating}
              </span>
              <span className="badge bg-white text-blue-600">
                {gameState.problem?.contestId}{gameState.problem?.index}
              </span>
            </div>
          </div>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2 mb-4">
          {gameState.problem?.tags?.map((tag, index) => (
            <span key={index} className="badge bg-blue-400 text-white">
              {tag}
            </span>
          ))}
        </div>

        {/* Problem Description */}
        {gameState.problem?.description ? (
          <div className="problem-section bg-white text-gray-900 p-6 rounded-lg mb-4 max-h-[500px] overflow-y-auto">
            <h3>Problem Description</h3>
            <div
              className="problem-content"
              dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(gameState.problem.description) }}
            />
          </div>
        ) : (
          <div className="problem-section bg-white text-gray-900 p-6 rounded-lg mb-4">
            <p className="problem-unavailable">
              ⚠️ Problem description temporarily unavailable.
              <a
                href={`https://codeforces.com/problemset/problem/${gameState.problem?.contestId}/${gameState.problem?.index}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{ marginLeft: '8px' }}
              >
                View on Codeforces →
              </a>
            </p>
          </div>
        )}

        {/* Problem Link */}


        {/* Code Editor - Only show when game is in progress */}
        {gameState.state === 'STARTED' && (
          <div className="code-editor-section">
            <h2 className="text-2xl font-bold mb-4">Code Editor</h2>
            <CodeEditor
              onSubmit={handleCodeSubmit}
              problemId={gameState.problem?.problemId}
              disabled={false}
            />
          </div>
        )}
      </div>

      {/* Status Board */}
      <div className="card">
        <h3 className="font-semibold mb-3">🏁 Race Status</h3>
        <div className="space-y-2">
          {gameState.users?.map((user, index) => (
            <div
              key={index}
              className={`flex items-center justify-between p-3 rounded-lg ${user.status === 'WON' ? 'bg-green-100' : 'bg-gray-50'
                }`}
            >
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold ${user.status === 'WON' ? 'bg-green-500' : 'bg-coderace-blue'
                  }`}>
                  {user.status === 'WON' ? '🏆' : user.codeforcesHandle[0].toUpperCase()}
                </div>
                <div className="font-semibold">{user.codeforcesHandle}</div>
              </div>
              <span className={getUserStatusBadge(user.status)}>
                {user.status === 'SOLVING' ? 'Solving...' : user.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Message */}
      {gameState.message && (
        <div className="card bg-blue-50 border-2 border-blue-200">
          <p className="text-center text-coderace-blue font-semibold">
            {gameState.message}
          </p>
        </div>
      )}
    </div>
  );

  /**
   * Renders the finished game
   */
  const renderFinishedGame = () => {
    const winner = gameState.users?.find(u => u.sessionId === gameState.winnerId);

    return (
      <div className="space-y-6">
        {/* Winner Announcement */}
        <div className="card bg-gradient-to-r from-green-500 to-green-600 text-white text-center py-12">
          <div className="text-6xl mb-4">🏆</div>
          <h2 className="text-3xl font-bold mb-2">Winner!</h2>
          <p className="text-2xl font-semibold">{winner?.codeforcesHandle}</p>
        </div>

        {/* Problem Info */}
        <div className="card">
          <h3 className="font-semibold mb-2">Problem Solved</h3>
          <p className="text-lg">{gameState.problem?.name}</p>
          <a
            href={gameState.problem?.problemUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-coderace-blue hover:underline"
          >
            View Problem →
          </a>
        </div>

        {/* Final Standings */}
        <div className="card">
          <h3 className="font-semibold mb-3">Final Standings</h3>
          <div className="space-y-2">
            {gameState.users?.map((user, index) => (
              <div
                key={index}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <span className="text-xl">{user.status === 'WON' ? '🥇' : '—'}</span>
                  <div className="font-semibold">{user.codeforcesHandle}</div>
                </div>
                <span className={getUserStatusBadge(user.status)}>
                  {user.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        <button className="btn-primary w-full" onClick={() => navigate('/dashboard')}>
          Leave Room
        </button>
      </div>
    );
  };

  return (
    <div className="min-h-screen p-4">
      <div className="max-w-4xl mx-auto py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl font-bold text-coderace-blue">
            🏁 CodeRace
          </h1>
          <button
            className="text-gray-600 hover:text-gray-900"
            onClick={() => navigate('/dashboard')}
          >
            ← Leave Room
          </button>
        </div>

        {/* Connection Status */}
        {!connected && (
          <div className="card bg-yellow-50 border-2 border-yellow-200 mb-4">
            <p className="text-center text-yellow-800">
              Connecting to room...
            </p>
          </div>
        )}

        {/* Render based on game state */}
        {connected && (
          <>
            {isGenerating && renderLoadingScreen()}
            {!isGenerating && gameState.state === 'WAITING' && renderWaitingLobby()}
            {!isGenerating && gameState.state === 'STARTED' && renderActiveGame()}
            {!isGenerating && gameState.state === 'FINISHED' && renderFinishedGame()}
          </>
        )}
      </div>
    </div>
  );
}

export default GameRoom;
