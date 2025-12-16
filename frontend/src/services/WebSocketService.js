import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

/**
 * WebSocket service for real-time communication with the backend
 * Uses SockJS for fallback and STOMP protocol for messaging
 */
class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = {};
  }

  /**
   * Connects to the WebSocket server
   * @param {Function} onConnected Callback when connection is established
   */
  connect(onConnected) {
    // Use environment variable or default to localhost
    const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

    // Create SockJS instance
    const socket = new SockJS(`${backendUrl}/ws`);

    // Create STOMP client
    this.client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        // console.log('STOMP: ' + str); // Removed
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Called when connection is established
    this.client.onConnect = (frame) => {
      // console.log('Connected to WebSocket:', frame); // Removed
      this.connected = true;
      if (onConnected) {
        onConnected();
      }
    };

    // Called on error
    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
    };

    // Activate the connection
    this.client.activate();
  }

  /**
   * Disconnects from the WebSocket server
   */
  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
      this.subscriptions = {};
      // console.log('Disconnected from WebSocket'); // Removed
    }
  }

  /**
   * Subscribes to a topic to receive messages
   * @param {String} topic The topic to subscribe to (e.g., '/topic/room/ROOM123')
   * @param {Function} callback Function to call when message is received
   */
  subscribe(topic, callback) {
    if (!this.connected || !this.client) {
      console.error('Not connected to WebSocket');
      return;
    }

    const subscription = this.client.subscribe(topic, (message) => {
      const body = JSON.parse(message.body);
      callback(body);
    });

    this.subscriptions[topic] = subscription;
    // console.log('Subscribed to:', topic); // Removed
  }

  /**
   * Unsubscribes from a topic
   * @param {String} topic The topic to unsubscribe from
   */
  unsubscribe(topic) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
  }

  /**
   * Sends a message to the server
   * @param {String} destination The destination (e.g., '/app/game/ROOM123/join')
   * @param {Object} body The message body
   */
  send(destination, body) {
    if (!this.connected || !this.client) {
      console.error('Not connected to WebSocket');
      return;
    }

    this.client.publish({
      destination: destination,
      body: JSON.stringify(body),
    });

    // console.log('Sent message to:', destination, body); // Removed
  }

  /**
   * Checks if connected
   */
  isConnected() {
    return this.connected;
  }
}

// Export a singleton instance
export default new WebSocketService();
