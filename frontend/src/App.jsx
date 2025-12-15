import { useState } from 'react';
import LandingPage from './components/LandingPage';
import GameRoom from './components/GameRoom';

/**
 * Main App Component
 * Manages navigation between Landing Page and Game Room
 */
function App() {
  const [currentView, setCurrentView] = useState('landing');
  const [roomId, setRoomId] = useState('');
  const [codeforcesHandle, setCodeforcesHandle] = useState('');

  /**
   * Called when a room is created
   */
  const handleRoomCreated = (newRoomId, handle) => {
    setRoomId(newRoomId);
    setCodeforcesHandle(handle);
    setCurrentView('gameroom');
  };

  /**
   * Called when joining a room
   */
  const handleRoomJoined = (joinRoomId, handle) => {
    setRoomId(joinRoomId);
    setCodeforcesHandle(handle);
    setCurrentView('gameroom');
  };

  /**
   * Returns to landing page
   */
  const handleLeaveRoom = () => {
    setRoomId('');
    setCodeforcesHandle('');
    setCurrentView('landing');
  };

  return (
    <div className="App">
      {currentView === 'landing' && (
        <LandingPage
          onRoomCreated={handleRoomCreated}
          onRoomJoined={handleRoomJoined}
        />
      )}
      
      {currentView === 'gameroom' && (
        <GameRoom
          roomId={roomId}
          codeforcesHandle={codeforcesHandle}
          onLeave={handleLeaveRoom}
        />
      )}
    </div>
  );
}

export default App;
