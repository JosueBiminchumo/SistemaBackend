import api from './api';

function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export const login = async (credentials) => {
  try {
    const { data } = await api.post('/auth/login', credentials);
    if (data?.accessToken) {
      localStorage.setItem('token', data.accessToken);
    }
    return data;
  } catch (error) {
    throw new Error(error.message || 'Error al iniciar sesión', { cause: error });
  }
};

export const register = async (userData) => {
  try {
    const { data } = await api.post('/auth/register', userData);
    if (data?.accessToken) {
      localStorage.setItem('token', data.accessToken);
    }
    return data;
  } catch (error) {
    throw new Error(error.message || 'Error al registrar usuario', { cause: error });
  }
};

export const logout = () => {
  localStorage.removeItem('token');
};

export const getToken = () => localStorage.getItem('token');

export const isAuthenticated = () => {
  const token = getToken();
  if (!token) return false;

  const payload = parseJwt(token);
  if (!payload?.exp) return true; 

  return payload.exp * 1000 > Date.now();
};
export const getUserRole = () => {
  const token = getToken();
  if (!token) return null;
  const payload = parseJwt(token);
  return payload?.role || payload?.rol || payload?.authorities?.[0] || null;
};

export const roleToHomeRoute = (role) => {
  switch (role) {
    case 'MEDICO':
    case 'DOCTOR':
      return '/medico/dashboard';
    case 'ADMIN':
      return '/admin/dashboard';
    case 'PACIENTE':
    case 'PATIENT':
    default:
      return '/paciente/inicio';
  }
};
