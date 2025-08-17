package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final RoomFinderRepository userRepo;
    private final QuizInsightsService insights;

    @Transactional(readOnly = true)
    public RoomFinderPublicDto view(String myEmailOrNull, Long otherId) {
        RoomFinder u = userRepo.findById(otherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var dto = new RoomFinderPublicDto();
        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setAge(u.getAge());
        dto.setGender(u.getGender());
        dto.setLocation(u.getLocation());
        dto.setUniversity(u.getUniversity());
        dto.setBio(u.getBio());
        dto.setMajor(u.getMajor());

        dto.setLifestyleTags(insights.tagsForUser(otherId));
        if (myEmailOrNull != null) {
            dto.setWhyYouMatch(insights.whyYouMatch(myEmailOrNull, otherId));
        }
        return dto;
    }
}
