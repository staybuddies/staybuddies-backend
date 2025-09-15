package com.example.SP.senior_project.mapper;

import com.example.SP.senior_project.dto.roomfinder.BehavioralDto;
import com.example.SP.senior_project.dto.roomfinder.PreferencesDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.model.RoomFinder;
import org.mapstruct.*;

import java.time.LocalTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomFinderMapper {

    /* -------- Read mappers (Entity -> DTO) -------- */

    RoomFinderDto toDto(RoomFinder rf);

    PreferencesDto toPreferencesDto(RoomFinder rf);

    // Convert LocalTime -> "HH:mm" strings for the behavioral view
    @Mappings({
            @Mapping(target = "bedtime",   source = "bedtime",   qualifiedByName = "timeToString"),
            @Mapping(target = "wakeTime",  source = "wakeTime",  qualifiedByName = "timeToString")
            // spend* and flags map 1:1 automatically
    })
    BehavioralDto toBehavioralDto(RoomFinder rf);

    /* -------- Update mappers (DTO -> Entity), ignore nulls so we don't clobber fields -------- */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(RoomFinderUpdateDto src, @MappingTarget RoomFinder dest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPreferences(PreferencesDto src, @MappingTarget RoomFinder dest);

    // Convert "HH:mm" -> LocalTime on update; nulls are ignored
    @Mappings({
            @Mapping(target = "bedtime",  source = "bedtime",  qualifiedByName = "stringToTime"),
            @Mapping(target = "wakeTime", source = "wakeTime", qualifiedByName = "stringToTime")
            // spend* and flags map 1:1 automatically; nulls ignored
    })
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromBehavioral(BehavioralDto src, @MappingTarget RoomFinder dest);

    /* -------- Converters -------- */

    @Named("stringToTime")
    default LocalTime stringToTime(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalTime.parse(s); // expects "HH:mm"
    }

    @Named("timeToString")
    default String timeToString(LocalTime t) {
        return (t == null) ? null : t.toString(); // "HH:mm"
    }
}
