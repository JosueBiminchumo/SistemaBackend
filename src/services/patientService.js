import api from './api';

export async function getPatientProfile() {
  try {
    const { data } = await api.get('/patients/me');
    return data;
  } catch (error) {
    throw new Error(error.message || 'No se pudo cargar el perfil.', { cause: error });
  }
}

export async function updatePatientProfile(profile) {
  try {
    const { data } = await api.put('/patients/me', profile);
    return data;
  } catch (error) {
    throw new Error(error.message || 'No se pudo actualizar el perfil.', { cause: error });
  }
}
