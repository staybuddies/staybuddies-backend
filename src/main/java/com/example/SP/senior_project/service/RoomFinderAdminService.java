package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomFinderAdminService {
    Page<RoomFinder> getAll(String keyword, Pageable pageable);
    RoomFinder getById(Long id);
    RoomFinder create(RoomFinderUpdateDto dto);
    RoomFinder update(Long id, RoomFinderUpdateDto dto);
    void toggleActive(Long id);
    void delete(Long id);
    void setActive(Long id, boolean active);

    void softDelete(Long id);
}
