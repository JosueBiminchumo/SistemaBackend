import { useState } from 'react';

const initialValues = {
  firstName: '',
  lastName: '',
  documentNumber: '',
  birthDate: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  acceptedTerms: false,
};

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function RegisterForm({ onRegister }) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (event) => {
    const { name, type, checked, value } = event.target;

    setValues((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }));

    setErrors((current) => ({ ...current, [name]: '', form: '' }));
    setSuccess('');
  };

  const validate = () => {
    const nextErrors = {};

    if (!values.firstName.trim()) {
      nextErrors.firstName = 'Ingresa tus nombres.';
    }

    if (!values.lastName.trim()) {
      nextErrors.lastName = 'Ingresa tus apellidos.';
    }

    if (!/^\d{8}$/.test(values.documentNumber.trim())) {
      nextErrors.documentNumber = 'El DNI debe tener 8 digitos.';
    }

    if (!values.birthDate) {
      nextErrors.birthDate = 'Ingresa tu fecha de nacimiento.';
    }

    if (!/^\d{9}$/.test(values.phone.trim())) {
      nextErrors.phone = 'El celular debe tener 9 digitos.';
    }

    if (!values.email.trim()) {
      nextErrors.email = 'Ingresa tu correo.';
    } else if (!isValidEmail(values.email)) {
      nextErrors.email = 'Ingresa un correo valido.';
    }

    if (values.password.length < 8) {
      nextErrors.password =
        'La contrasena debe tener al menos 8 caracteres.';
    }

    if (values.confirmPassword !== values.password) {
      nextErrors.confirmPassword = 'Las contrasenas no coinciden.';
    }

    if (!values.acceptedTerms) {
      nextErrors.acceptedTerms =
        'Debes aceptar el uso de datos para continuar.';
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

    const patient = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      documentNumber: values.documentNumber.trim(),
      birthDate: values.birthDate,
      phone: values.phone.trim(),
      email: values.email.trim().toLowerCase(),
      password: values.password,
    };

    setIsSubmitting(true);
    try {
      await onRegister(patient);
      setValues(initialValues);
      setSuccess('Registro realizado correctamente. Ya puedes iniciar sesión.');
    } catch (error) {
      setErrors((current) => ({
        ...current,
        form: error.message || 'No se pudo completar el registro.',
      }));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      {success && (
        <p className="form-alert form-alert--success">
          {success}
        </p>
      )}
      {errors.form && (
        <p className="form-alert form-alert--error" role="alert">
          {errors.form}
        </p>
      )}

      <div className="form-grid">
        <label className="field" htmlFor="first-name">
          <span>Nombres</span>
          <input
            autoComplete="given-name"
            id="first-name"
            name="firstName"
            onChange={handleChange}
            placeholder="Ana Maria"
            type="text"
            value={values.firstName}
          />
          {errors.firstName && <small>{errors.firstName}</small>}
        </label>

        <label className="field" htmlFor="last-name">
          <span>Apellidos</span>
          <input
            autoComplete="family-name"
            id="last-name"
            name="lastName"
            onChange={handleChange}
            placeholder="Torres Rojas"
            type="text"
            value={values.lastName}
          />
          {errors.lastName && <small>{errors.lastName}</small>}
        </label>
      </div>

      <div className="form-grid">
        <label className="field" htmlFor="document-number">
          <span>DNI</span>
          <input
            id="document-number"
            inputMode="numeric"
            maxLength="8"
            name="documentNumber"
            onChange={handleChange}
            placeholder="72581436"
            type="text"
            value={values.documentNumber}
          />
          {errors.documentNumber && (
            <small>{errors.documentNumber}</small>
          )}
        </label>

        <label className="field" htmlFor="birth-date">
          <span>Fecha de nacimiento</span>
          <input
            id="birth-date"
            name="birthDate"
            type="date"
            onChange={handleChange}
            value={values.birthDate}
          />
          {errors.birthDate && <small>{errors.birthDate}</small>}
        </label>
      </div>

      <label className="field" htmlFor="phone">
        <span>Celular</span>
        <input
          autoComplete="tel"
          id="phone"
          inputMode="numeric"
          maxLength="9"
          name="phone"
          onChange={handleChange}
          placeholder="987654321"
          type="tel"
          value={values.phone}
        />
        {errors.phone && <small>{errors.phone}</small>}
      </label>

      <label className="field" htmlFor="register-email">
        <span>Correo electronico</span>
        <input
          autoComplete="email"
          id="register-email"
          name="email"
          onChange={handleChange}
          placeholder="correo@ejemplo.com"
          type="email"
          value={values.email}
        />
        {errors.email && <small>{errors.email}</small>}
      </label>

      <div className="form-grid">
        <label className="field" htmlFor="register-password">
          <span>Contrasena</span>
          <input
            autoComplete="new-password"
            id="register-password"
            name="password"
            onChange={handleChange}
            placeholder="Minimo 8 caracteres"
            type="password"
            value={values.password}
          />
          {errors.password && <small>{errors.password}</small>}
        </label>

        <label className="field" htmlFor="confirm-password">
          <span>Confirmar contrasena</span>
          <input
            autoComplete="new-password"
            id="confirm-password"
            name="confirmPassword"
            onChange={handleChange}
            placeholder="Repite tu contrasena"
            type="password"
            value={values.confirmPassword}
          />
          {errors.confirmPassword && (
            <small>{errors.confirmPassword}</small>
          )}
        </label>
      </div>

      <label
        className="check-field check-field--stacked"
        htmlFor="accepted-terms"
      >
        <input
          checked={values.acceptedTerms}
          id="accepted-terms"
          name="acceptedTerms"
          onChange={handleChange}
          type="checkbox"
        />
        <span>
          Acepto el uso de mis datos para gestionar citas medicas.
        </span>
      </label>

      {errors.acceptedTerms && (
        <small className="field-error">
          {errors.acceptedTerms}
        </small>
      )}

      <button
        className="button button--primary button--full"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? 'Creando cuenta...' : 'Crear cuenta'}
      </button>
    </form>
  );
}