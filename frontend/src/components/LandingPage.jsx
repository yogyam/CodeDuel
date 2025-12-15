import { useState } from 'react';
import apiService from '../services/apiService';

/**
 * Landing Page Component
 * Users enter their Codeforces handle and either create or join a room
 */
function LandingPage({ onRoomCreated, onRoomJoined }) {
  const [codeforcesHandle, setCodeforcesHandle] = useState('');
  const [roomIdToJoin, setRoomIdToJoin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /**
   * Creates a new room
   */
  const handleCreateRoom = async () => {
    if (!codeforcesHandle.trim()) {
      setError('Please enter your Codeforces handle');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await apiService.createRoom(codeforcesHandle);
      onRoomCreated(response.roomId, codeforcesHandle);
    } catch (err) {
      console.error('Error creating room:', err);
      setError('Failed to create room. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Joins an existing room
   */
  const handleJoinRoom = () => {
    if (!codeforcesHandle.trim()) {
      setError('Please enter your Codeforces handle');
      return;
    }

    if (!roomIdToJoin.trim()) {
      setError('Please enter a room ID');
      return;
    }

    setError('');
    onRoomJoined(roomIdToJoin.toUpperCase(), codeforcesHandle);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card max-w-md w-full">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-coderace-blue mb-2">
            🏁 CodeRace
          </h1>
          <p className="text-gray-600">
            Race against your friends to solve Codeforces problems!
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        {/* Codeforces Handle Input */}
        <div className="mb-6">
          <label className="block text-sm font-semibold mb-2 text-gray-700">
            Codeforces Handle
          </label>
          <input
            type="text"
            className="input-field w-full"
            placeholder="Enter your handle (e.g., tourist)"
            value={codeforcesHandle}
            onChange={(e) => setCodeforcesHandle(e.target.value)}
            disabled={loading}
          />
        </div>

        {/* Create Room Button */}
        <button
          className="btn-primary w-full mb-4"
          onClick={handleCreateRoom}
          disabled={loading}
        >
          {loading ? 'Creating...' : 'Create New Room'}
        </button>

        {/* Divider */}
        <div className="flex items-center my-6">
          <div className="flex-1 border-t border-gray-300"></div>
          <span className="px-4 text-gray-500 text-sm">OR</span>
          <div className="flex-1 border-t border-gray-300"></div>
        </div>

        {/* Join Room Section */}
        <div className="mb-4">
          <label className="block text-sm font-semibold mb-2 text-gray-700">
            Room ID
          </label>
          <input
            type="text"
            className="input-field w-full"
            placeholder="Enter room ID (e.g., ABC123)"
            value={roomIdToJoin}
            onChange={(e) => setRoomIdToJoin(e.target.value.toUpperCase())}
            disabled={loading}
          />
        </div>

        <button
          className="btn-secondary w-full"
          onClick={handleJoinRoom}
          disabled={loading}
        >
          Join Room
        </button>

        {/* Instructions */}
        <div className="mt-8 p-4 bg-blue-50 rounded-lg">
          <h3 className="font-semibold text-sm mb-2 text-coderace-blue">
            How it works:
          </h3>
          <ul className="text-sm text-gray-700 space-y-1">
            <li>• Create a room and share the Room ID with friends</li>
            <li>• Host selects problem difficulty and starts the race</li>
            <li>• First to solve the problem on Codeforces wins!</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default LandingPage;
