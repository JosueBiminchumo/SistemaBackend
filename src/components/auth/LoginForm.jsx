import { useMemo, useState } from 'react';
import { login, roleToHomeRoute } from '../../services/authService';

const testUsers = [
  {
    id: 1,
    role: 'paciente',
    name: 'Ana Paciente',
    email: 'paciente@mediturn.pe',
    password: 'Paciente123',
    dashboardPath: '/paciente/inicio',
  },
  {
    id: 2,
    role: 'medico',
    name: 'Dr. Luis Medina',
    email: 'medico@mediturn.pe',
    password: 'Medico123',
    dashboardPath: '/medico/dashboard',
  },
  {
    id: 3,
    role: 'admin',
    name: 'Admin MediTurn',
    email: 'admin@mediturn.pe',
    password: 'Admin123',
    dashboardPath: '/admin/dashboard',
  },
];

const initialValues = {
  email: '',
  password: '',
  remember: true,
};

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function LoginForm({ onLogin }) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedUser = useMemo(
    () => testUsers.find((user) => user.email === values.email),
    [values.email]
  );

  const handleChange = (event) => {
    const { name, type, checked, value } = event.target;
    setValues((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }));
    setErrors((current) => ({ ...current, [name]: '' }));
    setMessage('');
  };

  const fillTestUser = (user) => {
    setValues({
      email: user.email,
      password: user.password,
      remember: true,
    });
    setErrors({});
    setMessage(`Usuario ${user.role} seleccionado.`);
  };

  const validate = () => {
    const nextErrors = {};

    if (!values.email.trim()) {
      nextErrors.email = 'Ingresa tu correo.';
    } else if (!isValidEmail(values.email)) {
      nextErrors.email = 'Ingresa un correo valido.';
    }

    if (!values.password.trim()) {
      nextErrors.password = 'Ingresa tu contrasena.';
    } else if (values.password.length < 6) {
      nextErrors.password = 'La contrasena debe tener al menos 6 caracteres.';
    }

    return nextErrors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validate();

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    const email = values.email.trim().toLowerCase();

    const localUser = testUsers.find(
      (testUser) => testUser.email === email && testUser.password === values.password
    );

    setIsSubmitting(true);
    try {
      if (localUser) {
        onLogin({
          ...localUser,
          remember: values.remember,
          isLocal: true,
        });
        return;
      }

      const response = await login({ email, password: values.password });
      const user = response.user;
      onLogin({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        name: `${user.firstName} ${user.lastName}`,
        email: user.email,
        role: user.role,
        dashboardPath: roleToHomeRoute(user.role),
        remember: values.remember,
        isLocal: false,
      });
    } catch (error) {
      setErrors({
        password: error.message || 'Credenciales incorrectas. Revisa tus datos.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <div className="auth-test">
        <span className="auth-test__label">Usuarios de prueba</span>
        <div className="auth-test__buttons" aria-label="Seleccionar usuario de prueba">
          {testUsers.map((user) => (
            <button
              className="auth-test__button"
              key={user.email}
              type="button"
              onClick={() => fillTestUser(user)}
            >
              {user.role}
            </button>
          ))}
        </div>
      </div>

      {message && <p className="form-alert form-alert--info">{message}</p>}

      <label className="field" htmlFor="login-email">
        <span>Correo electronico</span>
        <input
          autoComplete="email"
          id="login-email"
          name="email"
          onChange={handleChange}
          placeholder="paciente@mediturn.pe"
          type="email"
          value={values.email}
        />
        {errors.email && <small>{errors.email}</small>}
      </label>

      <label className="field" htmlFor="login-password">
        <span>Contrasena</span>
        <input
          autoComplete="current-password"
          id="login-password"
          name="password"
          onChange={handleChange}
          placeholder="Ingresa tu contrasena"
          type="password"
          value={values.password}
        />
        {errors.password && <small>{errors.password}</small>}
      </label>

      <div className="form-row form-row--between">
        <label className="check-field" htmlFor="remember">
          <input
            checked={values.remember}
            id="remember"
            name="remember"
            onChange={handleChange}
            type="checkbox"
          />
          <span>Recordarme</span>
        </label>
        <span className="muted-link">Acceso seguro</span>
      </div>

      <button
        className="button button--primary button--full"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? 'Ingresando...' : 'Iniciar sesión'}
      </button>

      {selectedUser && (
        <p className="form-hint">
          Esta cuenta ingresa como <strong>{selectedUser.role}</strong>.
        </p>
      )}
    </form>
  );
}
