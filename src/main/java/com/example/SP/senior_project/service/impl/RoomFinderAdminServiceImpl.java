package com.example.SP.senior_project.service.impl;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.MatchRequestRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.RoomFinderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomFinderAdminServiceImpl implements RoomFinderAdminService {

    private final RoomFinderRepository roomFinderRepository;

    private final MatchRequestRepository matchRequestRepository;

    @Override
    public Page<RoomFinder> getAll(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return roomFinderRepository.findAll(pageable);
        }
        return roomFinderRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);
    }

    @Override
    public RoomFinder getById(Long id) {
        return roomFinderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public RoomFinder create(RoomFinderUpdateDto dto) {
        if (dto.getEmail() != null && roomFinderRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        RoomFinder u = new RoomFinder();
        apply(u, dto);
        u.setActive(true);
        if (u.getJoinDate() == null) {
            u.setJoinDate(java.time.LocalDate.now());
        }
        return roomFinderRepository.save(u);
    }

    @Override
    public RoomFinder update(Long id, RoomFinderUpdateDto dto) {
        RoomFinder u = getById(id);
        if (dto.getEmail() != null
                && !dto.getEmail().equalsIgnoreCase(u.getEmail())
                && roomFinderRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        apply(u, dto);
        return roomFinderRepository.save(u);
    }

    /* ---- active helpers ---- */
    @Override
    public void setActive(Long id, boolean active) {
        RoomFinder u = getById(id);
        u.setActive(active);
        u.setTokenVersion(u.getTokenVersion() + 1);  // revoke all existing JWTs
        roomFinderRepository.save(u);
    }

    @Override
    public void toggleActive(Long id) {
        RoomFinder u = getById(id);
        u.setActive(!u.isActive());
        u.setTokenVersion(u.getTokenVersion() + 1);  // revoke all existing JWTs
        roomFinderRepository.save(u);
    }


    @Override
    public void delete(Long id) {
        roomFinderRepository.deleteById(id);
    }

    /* ---- mapping helper ---- */
    private void apply(RoomFinder u, RoomFinderUpdateDto dto) {
        if (dto.getName() != null) u.setName(dto.getName());
        if (dto.getEmail() != null) u.setEmail(dto.getEmail());
        if (dto.getPhone() != null) u.setPhone(dto.getPhone());
        if (dto.getGender() != null) u.setGender(dto.getGender());
        if (dto.getAge() != null) u.setAge(dto.getAge()); // Integer -> int
        if (dto.getLocation() != null) u.setLocation(dto.getLocation());
        if (dto.getUniversity() != null) u.setUniversity(dto.getUniversity());
        if (dto.getMajor() != null) u.setMajor(dto.getMajor());
        if (dto.getBio() != null) u.setBio(dto.getBio());
        if (dto.getAlreadyHasRoom() != null) u.setAlreadyHasRoom(dto.getAlreadyHasRoom());
        if (dto.getLocationSharing() != null) u.setLocationSharing(dto.getLocationSharing());
        if (dto.getEmailNotification() != null) u.setEmailNotification(dto.getEmailNotification());
        if (dto.getJoinDate() != null) u.setJoinDate(dto.getJoinDate());
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        RoomFinder u = getById(id);
        u.setActive(false);          // and/or u.setSuspended(true) or u.setDeletedAt(now)
        roomFinderRepository.save(u);
    }

}
