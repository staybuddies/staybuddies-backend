package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;


@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final RoomFinderRepository userRepo;
    private final QuizInsightsService insights;
    private final FileService fileService;

    public Page<RoomFinderPublicDto> featured(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepo.findByActiveTrue(pageable).map(this::toDto);
    }

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

    private RoomFinderPublicDto toDto(RoomFinder u) {
        var d = new RoomFinderPublicDto();
        d.setId(u.getId());
        d.setName(u.getName());
        d.setUniversity(u.getUniversity());
        d.setLocation(u.getLocation());
        try {
            d.setPhotoUrl(fileService.getFileName(FileType.ROOMFINDER_PROFILE, u.getId()));
        } catch (Exception ex) {
            d.setPhotoUrl(null);
        }
        return d;
    }
}
