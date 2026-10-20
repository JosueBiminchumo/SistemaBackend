import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';
import { getPatientProfile, updatePatientProfile } from '../../services/patientService';
import './PatientProfilePage.css';

function getLocalProfile() {
  const storedUser = localStorage.getItem('mediturn_user');
  const storedPatients = localStorage.getItem('patients');

  if (!storedUser || !storedPatients) {
    return null;
  }

  const currentUser = JSON.parse(storedUser);
  const patients = JSON.parse(storedPatients);

  const profile = patients.find(
    (patient) => patient.id === currentUser.id
  );
  return profile
    ? { ...profile, documentNumber: profile.documentNumber || profile.document || '' }
    : null;
}

function hasApiSession() {
  const token = localStorage.getItem('token');
  return Boolean(token && !token.startsWith('local-token-'));
}

function cacheProfile(profile, saveLocalProfile = true) {
  const cachedProfile = {
    ...profile,
    document: profile.documentNumber,
  };
  if (saveLocalProfile) {
    const patients = JSON.parse(localStorage.getItem('patients') || '[]');
    localStorage.setItem(
      'patients',
      JSON.stringify([
        ...patients.filter((patient) => patient.id !== cachedProfile.id),
        cachedProfile,
      ])
    );
    localStorage.setItem('mediturn_registered_patient', JSON.stringify(cachedProfile));
  }
  return cachedProfile;
}

export default function PatientProfilePage() {
  const navigate = useNavigate();
  const [isRemoteProfile] = useState(hasApiSession);
  const [profile, setProfile] = useState(() =>
    isRemoteProfile ? null : getLocalProfile()
  );
  const [savedProfile, setSavedProfile] = useState(() =>
    isRemoteProfile ? null : getLocalProfile()
  );
  const [isLoading, setIsLoading] = useState(isRemoteProfile);
  const [loadError, setLoadError] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  useEffect(() => {
    if (!isRemoteProfile) {
      return;
    }

    let isMounted = true;
    getPatientProfile()
      .then((response) => {
        if (!isMounted) {
          return;
        }
        const loadedProfile = cacheProfile(response, false);
        setProfile(loadedProfile);
        setSavedProfile(loadedProfile);
      })
      .catch((error) => {
        if (isMounted) {
          setLoadError(error.message || 'No se pudo cargar el perfil.');
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isRemoteProfile]);

  if (isLoading) {
    return (
      <main className="patient-profile-page">
        <section className="patient-profile-container">
          <p role="status">Cargando perfil...</p>
        </section>
      </main>
    );
  }

  if (!profile) {
    return (
      <main className="patient-profile-page">
        <section className="patient-profile-container">
          <h1>Perfil no encontrado</h1>
          <p>{loadError || 'No se encontró la información del paciente actual.'}</p>
        </section>
      </main>
    );
  }

  const fullName = `${profile.firstName} ${profile.lastName}`;
  const initials = `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`;

  const handleChange = (event) => {
    const { name, value } = event.target;

    setProfile((currentProfile) => ({
      ...currentProfile,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!isRemoteProfile && (currentPassword || newPassword)) {
      if (!currentPassword || !newPassword) {
        Swal.fire({
          title: 'Completa los campos',
          text: 'Ingresa tu contraseña actual y la nueva contraseña.',
          icon: 'warning',
          confirmButtonText: 'Aceptar',
          confirmButtonColor: '#2563eb',
        });
        return;
      }

      if (currentPassword !== profile.password) {
        Swal.fire({
          title: 'Contraseña incorrecta',
          text: 'La contraseña actual no coincide.',
          icon: 'error',
          confirmButtonText: 'Aceptar',
          confirmButtonColor: '#2563eb',
        });
        return;
      }

      if (newPassword.length < 8) {
        Swal.fire({
          title: 'Contraseña inválida',
          text: 'La nueva contraseña debe tener al menos 8 caracteres.',
          icon: 'warning',
          confirmButtonText: 'Aceptar',
          confirmButtonColor: '#2563eb',
        });
        return;
      }
    }

    try {
      const profileToSave = isRemoteProfile
        ? cacheProfile(await updatePatientProfile({
            documentNumber: profile.documentNumber.trim(),
            birthDate: profile.birthDate,
            phone: profile.phone.trim(),
          }), false)
        : cacheProfile({
            ...profile,
            ...(newPassword ? { password: newPassword } : {}),
          });

      setProfile(profileToSave);
      setSavedProfile(profileToSave);
      setCurrentPassword('');
      setNewPassword('');

      await Swal.fire({
        title: 'Perfil actualizado',
        text: 'Tus datos se guardaron correctamente.',
        icon: 'success',
        confirmButtonText: 'Aceptar',
        confirmButtonColor: '#2563eb',
      });
    } catch (error) {
      await Swal.fire({
        title: 'No se pudo actualizar el perfil',
        text: error.message || 'Inténtalo nuevamente.',
        icon: 'error',
        confirmButtonText: 'Aceptar',
        confirmButtonColor: '#2563eb',
      });
    }
  };

  const handleCancel = () => {
    if (isRemoteProfile) {
      setProfile(savedProfile);
    } else {
      setProfile(getLocalProfile());
    }
    setCurrentPassword('');
    setNewPassword('');
  };

  return (
    <main className="patient-profile-page">
      <section className="patient-profile-container">
        <button
          type="button"
          className="back-profile-button"
          onClick={() => navigate(-1)}
        >
          ← Volver
        </button>

        <div className="profile-header">
          <h1>Mi perfil</h1>
          <p>Gestiona tu información personal</p>
        </div>

        <div className="profile-layout">
          <div className="profile-card">
            <div className="profile-avatar">
              {initials}
            </div>

            <p className="profile-name">
              {fullName}
            </p>

            <p className="profile-role">
              Paciente
            </p>

            <button
              type="button"
              className="change-photo"
            >
              Cambiar foto
            </button>
          </div>

          <form
            className="form-card"
            onSubmit={handleSubmit}
          >
            <h2>Información personal</h2>

            <div className="profile-form-row">
              <div className="profile-field">
                <label htmlFor="full-name">
                  Nombre completo
                </label>

                <input
                  id="full-name"
                  type="text"
                  value={fullName}
                  readOnly
                />
              </div>

              <div className="profile-field">
                <label htmlFor="document">
                  DNI
                </label>

                <input
                  id="document"
                  name="documentNumber"
                  inputMode="numeric"
                  maxLength="8"
                  type="text"
                  value={profile.documentNumber || profile.document || ''}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="profile-form-row">
              <div className="profile-field">
                <label htmlFor="birth-date">
                  Fecha de nacimiento
                </label>

                <input
                  id="birth-date"
                  name="birthDate"
                  type="date"
                  value={profile.birthDate || ''}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="profile-field">
                <label htmlFor="phone">
                  Teléfono
                </label>

                <input
                  id="phone"
                  name="phone"
                  inputMode="numeric"
                  maxLength="9"
                  type="tel"
                  value={profile.phone || ''}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="profile-form-row">
              <div className="profile-field">
                <label htmlFor="email">
                  Correo electrónico
                </label>

                <input
                  id="email"
                  name="email"
                  type="email"
                  value={profile.email || ''}
                  onChange={handleChange}
                  required
                  readOnly={isRemoteProfile}
                />
              </div>
            </div>

            {!isRemoteProfile && (
              <>
                <hr className="profile-divider" />

                <h2>Cambiar contraseña</h2>

                <div className="profile-form-row">
                  <div className="profile-field">
                    <label htmlFor="current-password">
                      Contraseña actual
                    </label>

                    <input
                      id="current-password"
                      type="password"
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
                    />
                  </div>

                  <div className="profile-field">
                    <label htmlFor="new-password">
                      Nueva contraseña
                    </label>

                    <input
                      id="new-password"
                      type="password"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                    />
                  </div>
                </div>
              </>
            )}

            <div className="profile-actions">
              <button
                type="submit"
                className="save-profile-button"
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M20 6 9 17l-5-5" />
                </svg>

                Guardar cambios
              </button>

              <button
                type="button"
                className="cancel-profile-button"
                onClick={handleCancel}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </section>
    </main>
  );
}