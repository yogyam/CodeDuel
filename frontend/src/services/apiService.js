import axios from 'axios';

const API_BASE_URL = `${import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'}/api`;

// Create axios instance with default config
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
});

// Add request interceptor to include JWT token
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

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
    const response = await axiosInstance.post('/game/create-room', {
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
