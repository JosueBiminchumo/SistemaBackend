package com.mediturno.backend.appointment.adapter;

import java.lang.reflect.Method;

/**
 * Lectura tolerante de entidades de otros modulos (User, Patient) sin depender de
 * sus getters exactos. Temporal: se reemplaza por llamadas tipadas cuando los
 * duenos de esos modulos confirmen su API publica.
 */
final class EntityReader {

    private static final String[][] NAME_PAIRS = {
            {"getFirstName", "getLastName"},
            {"getNombres", "getApellidos"},
            {"getNombre", "getApellido"},
            {"getName", "getSurname"}
    };
    private static final String[] SINGLE_NAME_GETTERS = {"getName", "getNombre", "getEmail", "getUsername"};

    private EntityReader() {
    }

    /** Invoca un getter sin argumentos; devuelve null si no existe o falla. */
    static Object call(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    static String text(Object value) {
        if (value == null) {
            return null;
        }
        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /** Nombre completo del usuario, probando los nombres de campo mas habituales. */
    static String fullName(Object user) {
        if (user == null) {
            return null;
        }
        String full = text(call(user, "getFullName"));
        if (full != null) {
            return full;
        }
        for (String[] pair : NAME_PAIRS) {
            String first = text(call(user, pair[0]));
            String last = text(call(user, pair[1]));
            if (first != null || last != null) {
                return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            }
        }
        for (String getter : SINGLE_NAME_GETTERS) {
            String value = text(call(user, getter));
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}