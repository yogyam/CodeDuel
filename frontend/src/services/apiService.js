import axios from 'axios';

const API_BASE_URL = `${import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'}/api`;

/**
 * Service for making HTTP requests to the backend
 */
const apiService = {
  /**
   * Creates a new game room
   * @param {String} codeforcesHandle The Codeforces handle of the host
   * @returns {Promise} Response with roomId
   */
  createRoom: async (codeforcesHandle) => {
    const response = await axios.post(`${API_BASE_URL}/game/create-room`, {
      codeforcesHandle
    });
    return response.data;
  },

  /**
   * Gets room information
   * @param {String} roomId The room ID
   * @returns {Promise} Room information
   */
  getRoomInfo: async (roomId) => {
    const response = await axios.get(`${API_BASE_URL}/game/room/${roomId}`);
    return response.data;
  },

  /**
   * Health check
   * @returns {Promise} Health status
   */
  healthCheck: async () => {
    const response = await axios.get(`${API_BASE_URL}/game/health`);
    return response.data;
  }
};

export default apiService;
