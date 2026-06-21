import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      const message = data?.message ?? `Request failed with status ${status}`;
      console.error(`[API Error] ${status}: ${message}`);
      return Promise.reject(new Error(message));
    }

    if (error.request) {
      console.error('[API Error] No response received from server');
      return Promise.reject(new Error('No response received from server'));
    }

    console.error('[API Error]', error.message);
    return Promise.reject(error);
  },
);

export default api;
