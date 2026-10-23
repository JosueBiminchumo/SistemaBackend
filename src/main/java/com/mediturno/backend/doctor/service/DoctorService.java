package com.mediturno.backend.doctor.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.exception.ConflictException;
import com.mediturno.backend.common.exception.NotFoundException;
import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.dto.DoctorCreateRequest;
import com.mediturno.backend.doctor.dto.DoctorProfileUpdateRequest;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.dto.DoctorUpdateRequest;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import com.mediturno.backend.specialty.domain.Specialty;
import com.mediturno.backend.specialty.service.SpecialtyService;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import com.mediturno.backend.user.repository.UserRepository;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyService specialtyService;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            SpecialtyService specialtyService,
            PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.specialtyService = specialtyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> list(Long specialtyId, String search, boolean admin, boolean includeInactive) {
        List<Doctor> doctors;
        if (admin && includeInactive) {
            doctors = doctorRepository.findAllWithDetails().stream()
                    .filter(d -> specialtyId == null || d.getSpecialty().getId().equals(specialtyId))
                    .filter(d -> search == null || search.isBlank() || (d.getUser().getFirstName() + " "
                            + d.getUser().getLastName()).toLowerCase(Locale.ROOT)
                            .contains(search.trim().toLowerCase(Locale.ROOT)))
                    .toList();
        } else {
            String q = search == null || search.isBlank()
                    ? "%"
                    : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            doctors = doctorRepository.searchActive(specialtyId, q);
        }
        return doctors.stream().map(DoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse get(Long id, boolean admin) {
        Doctor doctor = find(id);
        if (!doctor.isActive() && !admin) {
            throw new NotFoundException("El médico no existe.");
        }
        return DoctorResponse.from(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getMe(String email) {
        return DoctorResponse.from(findByEmail(email));
    }

    @Transactional
    public DoctorResponse updateMe(String email, DoctorProfileUpdateRequest request) {
        Doctor doctor = findByEmail(email);
        doctor.updateBiography(clean(request.biography()));
        return DoctorResponse.from(doctorRepository.saveAndFlush(doctor));
    }

    @Transactional
    public DoctorResponse create(DoctorCreateRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El correo ya está registrado.");
        }
        String license = request.licenseNumber().trim();
        if (doctorRepository.existsByLicenseNumber(license)) {
            throw new ConflictException("La colegiatura ya está registrada.");
        }
        Specialty specialty = specialtyService.findActive(request.specialtyId());

        User user = userRepository.saveAndFlush(User.create(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.DOCTOR,
                true));
        Doctor doctor = doctorRepository.saveAndFlush(
                new Doctor(user, specialty, license, clean(request.biography())));
        return DoctorResponse.from(doctor);
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorUpdateRequest request) {
        Doctor doctor = find(id);
        String license = request.licenseNumber().trim();
        if (doctorRepository.existsByLicenseNumberAndIdNot(license, id)) {
            throw new ConflictException("La colegiatura ya está registrada.");
        }
        Specialty specialty = doctor.getSpecialty().getId().equals(request.specialtyId())
                ? doctor.getSpecialty()
                : specialtyService.findActive(request.specialtyId());
        doctor.updateByAdmin(
                specialty,
                license,
                clean(request.biography()),
                request.active() == null ? doctor.isActive() : request.active());
        return DoctorResponse.from(doctorRepository.saveAndFlush(doctor));
    }

    @Transactional
    public void deactivate(Long id) {
        Doctor doctor = find(id);
        doctor.deactivate();
        doctorRepository.saveAndFlush(doctor);
    }

    private Doctor find(Long id) {
        return doctorRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("El médico no existe."));
    }

    private Doctor findByEmail(String email) {
        return doctorRepository.findDetailedByUserEmail(email)
                .orElseThrow(() -> new NotFoundException("No se encontró el perfil del médico."));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
