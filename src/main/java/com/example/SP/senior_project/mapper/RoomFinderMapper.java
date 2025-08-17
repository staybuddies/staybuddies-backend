package com.example.SP.senior_project.mapper;

import com.example.SP.senior_project.dto.roomfinder.BehavioralDto;
import com.example.SP.senior_project.dto.roomfinder.PreferencesDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.model.RoomFinder;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Qualifier;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomFinderMapper {

    @Qualifier("roomFinderMapperImpl")
    RoomFinderDto toDto(RoomFinder rf);

    PreferencesDto toPreferencesDto(RoomFinder rf);

    BehavioralDto toBehavioralDto(RoomFinder rf);

    // -------- Update mappers (DTO -> Entity), ignore nulls so we don't clobber fields --------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(RoomFinderUpdateDto src, @MappingTarget RoomFinder dest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPreferences(PreferencesDto src, @MappingTarget RoomFinder dest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromBehavioral(BehavioralDto src, @MappingTarget RoomFinder dest);
}
