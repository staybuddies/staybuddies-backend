package com.example.SP.senior_project.mapper;

import com.example.SP.senior_project.dto.roomfinder.BehavioralDto;
import com.example.SP.senior_project.dto.roomfinder.PreferencesDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomFinderMapper {

// Never send password to the client
    @Mapping(target = "password", ignore = true)
    RoomFinderDto toDto(RoomFinder entity);

    // Partial update: ignore nulls so we don't overwrite existing fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(RoomFinderUpdateDto dto, @MappingTarget RoomFinder entity);

    // Preferences mapping
    PreferencesDto toPreferencesDto(RoomFinder rf);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPreferences(PreferencesDto dto, @MappingTarget RoomFinder rf);

    // Behavioral mapping
    BehavioralDto toBehavioralDto(RoomFinder rf);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromBehavioral(BehavioralDto dto, @MappingTarget RoomFinder rf);
}

