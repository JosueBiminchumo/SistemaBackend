import { Link } from 'react-router-dom';
import './PatientDashboard.css';

function getAppointments() {
  const storedUser = localStorage.getItem('mediturn_user');
  const storedAppointments = localStorage.getItem('appointments');

  if (!storedUser || !storedAppointments) {
    return [];
  }

  const currentUser = JSON.parse(storedUser);
  const appointments = JSON.parse(storedAppointments);

  return appointments.filter(
    (appointment) => appointment.patientId === currentUser.id
  );
}

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

function formatDate(date) {
  if (!date) {
    return '';
  }

  if (date.includes(' de ')) {
    return date;
  }

  const parts = date.split('-');

  if (parts.length !== 3) {
    return date;
  }

  const [year, month, day] = parts;

  const months = [
    'enero',
    'febrero',
    'marzo',
    'abril',
    'mayo',
    'junio',
    'julio',
    'agosto',
    'septiembre',
    'octubre',
    'noviembre',
    'diciembre',
  ];

  return `${day} de ${months[Number(month) - 1]} de ${year}`;
}

export default function PatientDashboard() {
  const profile = getProfile();
  const appointments = getAppointments();
  const recentAppointments = [...appointments].reverse();

  const activeAppointments = recentAppointments.filter(
    (appointment) => appointment.status !== 'cancelled'
  );

  const nextAppointment = activeAppointments[0];

  const completedAppointments = recentAppointments.filter(
    (appointment) =>
      appointment.status === 'completed' ||
      appointment.status === 'atendida'
  );

  return (
    <main className="patient-dashboard">
      <section className="patient-dashboard-container">
        <div className="patient-dashboard-header">
          <div>
            <h1>¡Hola, {profile.firstName}! 👋</h1>
            <p>Aquí tienes un resumen de tu salud.</p>
          </div>
        </div>

        <div className="dashboard-summary">
          <div className="summary-card">
            <div className="summary-card-icon calendar-icon">📅</div>

            <div>
              <span>CITAS PROGRAMADAS</span>
              <strong>{activeAppointments.length}</strong>
              <small>Próximas citas</small>
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-icon check-icon">✅</div>

            <div>
              <span>CITAS COMPLETADAS</span>
              <strong>{completedAppointments.length}</strong>
              <small>Total histórico</small>
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-icon pulse-icon">🫀</div>

            <div>
              <span>PRÓXIMA CITA</span>

              <strong>
                {nextAppointment
                  ? nextAppointment.date
                  : 'Sin programar'}
              </strong>

              <small>
                {nextAppointment
                  ? nextAppointment.time || '09:00'
                  : '--:--'}
              </small>
            </div>
          </div>
        </div>

        {nextAppointment ? (
          <section className="next-appt">
            <div className="next-appt-content">
              <p className="next-appt-label">Próxima cita</p>

              <h3>{nextAppointment.doctor}</h3>

              <p className="next-appt-specialty">
                {nextAppointment.specialty}
              </p>

              <div className="next-appt-datetime">
                <span>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <rect
                      x="3"
                      y="4"
                      width="18"
                      height="18"
                      rx="2"
                    />
                    <path d="M16 2v4M8 2v4M3 10h18" />
                  </svg>

                  {formatDate(nextAppointment.date)}
                </span>

                <span>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <path d="M12 6v6l4 2" />
                  </svg>

                  {nextAppointment.time || '09:00'}
                </span>
              </div>

              <div className="next-appt-actions">
                <Link
                  to={`/paciente/cita/${nextAppointment.id}`}
                  className="next-appt-detail"
                >
                  Ver detalle
                </Link>

                {nextAppointment.status !== 'completed' &&
                  nextAppointment.status !== 'atendida' && (
                    <Link
                      to={`/paciente/cita/${nextAppointment.id}/reprogramar`}
                      className="next-appt-reschedule"
                    >
                      Reprogramar
                    </Link>
                  )}
              </div>
            </div>

            <div className="next-appt-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M11 2v6" />
                <path d="M15 2v6" />
                <path d="M9 8h8a2 2 0 0 1 2 2v2a5 5 0 0 1-5 5v0" />
                <path d="M14 17v2a2 2 0 0 1-2 2h0a2 2 0 0 1-2-2v-2" />
                <circle cx="20" cy="10" r="2" />
              </svg>
            </div>
          </section>
        ) : (
          <section className="next-appt">
            <div className="next-appt-content">
              <p className="next-appt-label">Próxima cita</p>

              <h3>No tienes citas próximas</h3>

              <p className="next-appt-specialty">
                Puedes reservar una nueva cita cuando lo necesites.
              </p>

              <div className="next-appt-actions">
                <Link
                  to="/paciente/reservar-cita"
                  className="next-appt-detail"
                >
                  Reservar cita
                </Link>
              </div>
            </div>

            <div className="next-appt-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M11 2v6" />
                <path d="M15 2v6" />
                <path d="M9 8h8a2 2 0 0 1 2 2v2a5 5 0 0 1-5 5v0" />
                <path d="M14 17v2a2 2 0 0 1-2 2h0a2 2 0 0 1-2-2v-2" />
                <circle cx="20" cy="10" r="2" />
              </svg>
            </div>
          </section>
        )}

        <section className="patient-dashboard-section">
          <div className="section-title-row">
            <h2>Citas recientes</h2>

            <Link
              to="/paciente/mis-citas"
              className="ver-todas-link"
            >
              Ver todas &gt;
            </Link>
          </div>

          <div className="recent-appointments-list">
            {recentAppointments.length > 0 ? (
              recentAppointments
                .slice(0, 3)
                .map((appt) => (
                  <div
                    key={appt.id || appt.date}
                    className="recent-appointment-item"
                  >
                    <div className="appt-info-left">
                      <span className="stethoscope-icon">
                        🩺
                      </span>

                      <div>
                        <strong>{appt.doctor}</strong>

                        <p>
                          {appt.specialty} ·{' '}
                          {formatDate(appt.date)} {appt.time}
                        </p>
                      </div>
                    </div>

                    <div>
                      <span
                        className={`status-badge ${appt.status}`}
                      >
                        •{' '}
                        {appt.status === 'confirmed'
                          ? 'Confirmada'
                          : appt.status === 'completed' ||
                            appt.status === 'atendida'
                          ? 'Completada'
                          : appt.status === 'pending'
                          ? 'Pendiente'
                          : appt.status === 'cancelled'
                          ? 'Cancelada'
                          : 'Pendiente'}
                      </span>
                    </div>
                  </div>
                ))
            ) : (
              <p className="no-appointments-text">
                No hay citas recientes registradas.
              </p>
            )}
          </div>
        </section>

        <div className="dashboard-reminder-banner">
          <span className="reminder-icon">🔔</span>

          <div>
            <strong>Recordatorio</strong>

            <p>
              Llega 10 minutos antes de tu cita y trae tu DNI.
              En caso de cancelación, hazlo con al menos 24
              horas de anticipación.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}