import axios from 'axios';

const API_BASE_URL = `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api`;

// Create axios instance with cookie-based authentication
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Send cookies with every request
});

/**
 * Service for making HTTP requests to the backend
 */
const apiService = {
  /**
   * Creates a new game room
   * @param {String} handle The Codeforces handle of the host
   * @returns {Promise} Response with roomId
   */
  createRoom: async (handle) => {
    const response = await axiosInstance.post('/game/create-room', {
      handle
    });
    return response.data;
  },

  /**
   * Gets room information
   * @param {String} roomId The room ID
   * @returns {Promise} Room information
   */
  getRoomInfo: async (roomId) => {
    const response = await axiosInstance.get(`/game/room/${roomId}`);
    return response.data;
  },

  /**
   * Health check
   * @returns {Promise} Health status
   */
  healthCheck: async () => {
    const response = await axiosInstance.get('/game/health');
    return response.data;
  }
};

export default apiService;
