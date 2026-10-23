package com.mediturno.backend.specialty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediturno.backend.specialty.dto.SpecialtyRequest;
import com.mediturno.backend.specialty.dto.SpecialtyResponse;
import com.mediturno.backend.specialty.entity.Specialty;
import com.mediturno.backend.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    private SpecialtyService specialtyService;

    @BeforeEach
    void setUp() {
        specialtyService = new SpecialtyService(specialtyRepository);
    }

    @Test
    void listReturnsAllWhenActiveIsNull() {
        Specialty s1 = new Specialty("Cardiología", "Desc 1", true);
        Specialty s2 = new Specialty("Pediatría", "Desc 2", false);
        when(specialtyRepository.findAllByOrderByNameAsc()).thenReturn(List.of(s1, s2));

        List<SpecialtyResponse> result = specialtyService.list(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Cardiología");
        assertThat(result.get(1).name()).isEqualTo("Pediatría");
        verify(specialtyRepository).findAllByOrderByNameAsc();
    }

    @Test
    void listReturnsFilteredWhenActiveIsProvided() {
        Specialty s1 = new Specialty("Cardiología", "Desc 1", true);
        when(specialtyRepository.findByActiveOrderByNameAsc(true)).thenReturn(List.of(s1));

        List<SpecialtyResponse> result = specialtyService.list(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Cardiología");
        verify(specialtyRepository).findByActiveOrderByNameAsc(true);
    }

    @Test
    void getByIdReturnsSpecialtyWhenFound() {
        Specialty s1 = new Specialty("Cardiología", "Desc 1", true);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(s1));

        SpecialtyResponse response = specialtyService.getById(1L);

        assertThat(response.name()).isEqualTo("Cardiología");
        assertThat(response.description()).isEqualTo("Desc 1");
        verify(specialtyRepository).findById(1L);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.getById(99L))
                .isInstanceOf(SpecialtyNotFoundException.class)
                .hasMessage("La especialidad no existe");

        verify(specialtyRepository).findById(99L);
    }

    @Test
    void createNormalizesNameAndSavesSuccessfully() {
        SpecialtyRequest request = new SpecialtyRequest("   Cardiología    Clínica   ", "  Atención médica  ", null);
        when(specialtyRepository.existsByNameIgnoreCase("Cardiología Clínica")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyResponse response = specialtyService.create(request);

        assertThat(response.name()).isEqualTo("Cardiología Clínica");
        assertThat(response.description()).isEqualTo("Atención médica");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);
        verify(specialtyRepository).save(captor.capture());
        Specialty saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Cardiología Clínica");
        assertThat(saved.getDescription()).isEqualTo("Atención médica");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void createThrowsWhenNameAlreadyExists() {
        SpecialtyRequest request = new SpecialtyRequest("Cardiología", "Desc", true);
        when(specialtyRepository.existsByNameIgnoreCase("Cardiología")).thenReturn(true);

        assertThatThrownBy(() -> specialtyService.create(request))
                .isInstanceOf(SpecialtyNameAlreadyExistsException.class)
                .hasMessage("Ya existe una especialidad con ese nombre");

        verify(specialtyRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenSpecialtyDoesNotExist() {
        SpecialtyRequest request = new SpecialtyRequest("Neurología", "Desc", true);
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.update(99L, request))
                .isInstanceOf(SpecialtyNotFoundException.class)
                .hasMessage("La especialidad no existe");

        verify(specialtyRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenNameAlreadyExistsForAnotherSpecialty() {
        Specialty existing = new Specialty("Cardiología", "Desc", true);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(specialtyRepository.existsByNameIgnoreCaseAndIdNot("Neurología", 1L)).thenReturn(true);

        SpecialtyRequest request = new SpecialtyRequest("Neurología", "Nueva desc", true);

        assertThatThrownBy(() -> specialtyService.update(1L, request))
                .isInstanceOf(SpecialtyNameAlreadyExistsException.class)
                .hasMessage("Ya existe una especialidad con ese nombre");

        verify(specialtyRepository, never()).save(any());
    }

    @Test
    void updateModifiesFieldsSuccessfully() {
        Specialty existing = new Specialty("Cardiología", "Desc", true);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(specialtyRepository.existsByNameIgnoreCaseAndIdNot("Cardiología Avanzada", 1L)).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyRequest request = new SpecialtyRequest("Cardiología Avanzada", "Nueva descripción", false);
        SpecialtyResponse response = specialtyService.update(1L, request);

        assertThat(response.name()).isEqualTo("Cardiología Avanzada");
        assertThat(response.description()).isEqualTo("Nueva descripción");
        assertThat(response.active()).isFalse();
        verify(specialtyRepository).save(existing);
    }

    @Test
    void deactivatePerformsSoftDelete() {
        Specialty existing = new Specialty("Oftalmología", "Ojos", true);
        when(specialtyRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpecialtyResponse response = specialtyService.deactivate(5L);

        assertThat(response.active()).isFalse();
        assertThat(existing.isActive()).isFalse();
        verify(specialtyRepository).save(existing);
        verify(specialtyRepository, never()).delete(any());
        verify(specialtyRepository, never()).deleteById(any());
    }

    @Test
    void deactivateThrowsWhenSpecialtyDoesNotExist() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specialtyService.deactivate(99L))
                .isInstanceOf(SpecialtyNotFoundException.class)
                .hasMessage("La especialidad no existe");

        verify(specialtyRepository, never()).save(any());
    }
}
