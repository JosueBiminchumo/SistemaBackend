import { Outlet, Link, useLocation } from 'react-router-dom';
import { useState } from 'react';
import './PatientLayout.css';

function getProfile() {
  const storedUser = localStorage.getItem('mediturn_user');
  const storedPatients = localStorage.getItem('patients');

  if (!storedUser) {
    return {
      firstName: 'Paciente',
      lastName: '',
    };
  }

  const currentUser = JSON.parse(storedUser);
  const token = localStorage.getItem('token');
  if (currentUser.isLocal === false || (token && !token.startsWith('local-token-'))) {
    return {
      firstName: currentUser.firstName || currentUser.name?.split(' ')[0] || 'Paciente',
      lastName: currentUser.lastName || currentUser.name?.split(' ').slice(1).join(' ') || '',
    };
  }

  if (!storedPatients) {
    return {
      firstName: 'Paciente',
      lastName: '',
    };
  }

  const patients = JSON.parse(storedPatients);

  return (
    patients.find(
      (patient) => patient.id === currentUser.id
    ) || {
      firstName: 'Paciente',
      lastName: '',
    }
  );
}

function getNotifications() {
  const storedUser = localStorage.getItem('mediturn_user');
  const storedNotifications = localStorage.getItem('notifications');

  if (!storedUser || !storedNotifications) {
    return [];
  }

  const currentUser = JSON.parse(storedUser);
  const notifications = JSON.parse(storedNotifications);

  return notifications.filter(
    (notification) => notification.patientId === currentUser.id
  );
}

export default function PatientLayout() {
  const profile = getProfile();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState(
    getNotifications
  );

  const unreadNotifications = notifications.filter(
    (notification) => !notification.read
  );

  const isActive = (path) => location.pathname === path;

  const closeSidebar = () => {
    setSidebarOpen(false);
  };

  const handleNotificationClick = (notificationId) => {
    const updatedNotifications = notifications.map(
      (notification) =>
        notification.id === notificationId
          ? { ...notification, read: true }
          : notification
    );

    setNotifications(updatedNotifications);

    localStorage.setItem(
      'notifications',
      JSON.stringify(updatedNotifications)
    );
  };

  const initials = `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`;

  return (
    <div className="patient-layout">
      <header className="mobile-header">
        <button
          type="button"
          className="mobile-menu-button"
          onClick={() => setSidebarOpen(true)}
          aria-label="Abrir menú"
        >
          ☰
        </button>

        <span className="mobile-brand-name">MediTurno</span>
      </header>

      {sidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={closeSidebar}
        />
      )}

      <aside
        className={`patient-sidebar ${
          sidebarOpen ? 'sidebar-open' : ''
        }`}
      >
        <div className="sidebar-logo-container">
          <div className="sidebar-logo-icon">🩺</div>

          <div>
            <span className="sidebar-brand-name">MediTurno</span>
            <span className="sidebar-brand-sub">
              Tu salud, a tu tiempo
            </span>
          </div>

          <button
            type="button"
            className="mobile-close-button"
            onClick={closeSidebar}
            aria-label="Cerrar menú"
          >
            ×
          </button>
        </div>

        <div className="sidebar-profile-card">
          <div className="sidebar-avatar">{initials}</div>

          <div>
            <strong>
              {profile.firstName} {profile.lastName}
            </strong>
            <span>Paciente</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          <Link
            to="/paciente"
            onClick={closeSidebar}
            className={`sidebar-nav-item ${
              isActive('/paciente') ? 'active' : ''
            }`}
          >
            <span className="nav-icon">☷</span>
            Inicio
          </Link>

          <Link
            to="/paciente/reservar-cita"
            onClick={closeSidebar}
            className={`sidebar-nav-item ${
              isActive('/paciente/reservar-cita') ? 'active' : ''
            }`}
          >
            <span className="nav-icon">+</span>
            Reservar cita
          </Link>

          <Link
            to="/paciente/mis-citas"
            onClick={closeSidebar}
            className={`sidebar-nav-item ${
              isActive('/paciente/mis-citas') ? 'active' : ''
            }`}
          >
            <span className="nav-icon">📅</span>
            Mis citas
          </Link>

          <Link
            to="/paciente/historial"
            onClick={closeSidebar}
            className={`sidebar-nav-item ${
              isActive('/paciente/historial') ? 'active' : ''
            }`}
          >
            <span className="nav-icon">📋</span>
            Historial
          </Link>

          <Link
            to="/paciente/perfil"
            onClick={closeSidebar}
            className={`sidebar-nav-item ${
              isActive('/paciente/perfil') ? 'active' : ''
            }`}
          >
            <span className="nav-icon">👤</span>
            Mi perfil
          </Link>
        </nav>

        <div className="sidebar-footer">
          <Link
            to="/login"
            onClick={closeSidebar}
            className="sidebar-logout"
          >
            <span>🚪</span>
            Cerrar sesión
          </Link>
        </div>
      </aside>

      <div className="patient-main-content">
        <header className="patient-topbar">
          <div className="patient-search">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.3-4.3" />
            </svg>

            <input
              type="text"
              placeholder="Buscar..."
            />
          </div>

          <div className="patient-topbar-right">
            <button
              type="button"
              className="notification-button"
              onClick={() =>
                setNotificationsOpen((isOpen) => !isOpen)
              }
              aria-label="Notificaciones"
            >
              <svg
                width="19"
                height="19"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>

              {unreadNotifications.length > 0 && (
                <span className="notification-dot" />
              )}
            </button>

            <div className="topbar-user">
              <div className="topbar-avatar">
                {initials}
              </div>

              <span>{profile.firstName}</span>
            </div>

            {notificationsOpen && (
              <div className="notifications-dropdown">
                <h3>Notificaciones</h3>

                {notifications.length > 0 ? (
                  notifications.map((notification) => (
                    <button
                      type="button"
                      className={`notification-item ${
                        !notification.read ? 'unread' : ''
                      }`}
                      key={notification.id}
                      onClick={() =>
                        handleNotificationClick(
                          notification.id
                        )
                      }
                    >
                      <div className="notification-content">
                        <p>{notification.message}</p>

                        <span>
                          {notification.date} ·{' '}
                          {notification.time}
                        </span>
                      </div>

                      {!notification.read && (
                        <span className="notification-unread-dot" />
                      )}
                    </button>
                  ))
                ) : (
                  <div className="notification-item">
                    <p>No tienes notificaciones.</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </header>

        <Outlet />
      </div>
    </div>
  );
}