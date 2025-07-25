package com.example.SP.senior_project.mapper;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomFinderMapper {
    RoomFinderDto toDto(RoomFinder entity);
    RoomFinder toEntity(RoomFinderDto dto);
    // update an existing entity from an update‐DTO
    @Mapping(target = "password", source = "password")
    @Mapping(target = "phone", source = "phone")
    void updateFromDto(RoomFinderUpdateDto dto, @MappingTarget RoomFinder entity);
}

