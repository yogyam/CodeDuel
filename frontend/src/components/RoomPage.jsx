import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/apiService';
import './RoomPage.css';

/**
 * Room Page - Create or join a coding room
 */
function RoomPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [codeforcesHandle, setCodeforcesHandle] = useState(user?.codeforcesHandle || '');
  const [roomId, setRoomId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /**
   * Creates a new room
   */
  const handleCreateRoom = async (e) => {
    e.preventDefault();
    if (!codeforcesHandle.trim()) {
      setError('Please enter your Codeforces handle');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await apiService.createRoom(codeforcesHandle);
      // Navigate to the game room
      navigate(`/game/${response.roomId}`);
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
  const handleJoinRoom = async (e) => {
    e.preventDefault();
    if (!roomId.trim()) {
      setError('Please enter a room ID');
      return;
    }

    if (!codeforcesHandle.trim()) {
      setError('Please enter your Codeforces handle');
      return;
    }

    setLoading(true);
    setError('');

    try {
      // Verify room exists
      await apiService.getRoomInfo(roomId);
      // Navigate to the game room
      navigate(`/game/${roomId}`);
    } catch (err) {
      console.error('Error joining room:', err);
      setError('Room not found. Please check the room ID.');
    } finally {
      setLoading(false);
    }
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
            value={roomId}
            onChange={(e) => setRoomId(e.target.value.toUpperCase())}
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

export default RoomPage;
