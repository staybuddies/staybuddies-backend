package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.model.constant.VerificationStatus;
import com.example.SP.senior_project.repository.QuizResponseRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.repository.StudentIdVerificationRepository;
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
    private final FileService fileService;
    private final QuizResponseRepository quizRepo;

    // NEW: use IDV repo to confirm identity verification
    private final StudentIdVerificationRepository idvRepo;

    private static boolean truthy(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private boolean computeIdentityVerified(RoomFinder u) {

        return truthy(u.getIdVerified())
                || idvRepo.existsByUser_IdAndStatus(u.getId(), VerificationStatus.VERIFIED);
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

        // booleans mapped directly
        dto.setEmailVerified(u.isSchoolEmailVerified());
        dto.setIdentityVerified(computeIdentityVerified(u));
        dto.setAlreadyHasRoom(u.isAlreadyHasRoom());
        dto.setSchoolEmail(u.getSchoolEmail());
        dto.setStatus(u.getStatus());

        // Lifestyle: prefer latest quiz answers -> tags; else fallback to service tags
        try {
            // use whichever repo method you actually have:
            // var qr = quizRepo.findTopByRoomFinder_IdOrderByIdDesc(otherId).orElse(null);
            var qr = quizRepo.findByRoomFinder_Id(otherId).orElse(null);
            if (qr != null && qr.getAnswers() != null && !qr.getAnswers().isEmpty()) {
                var answers = qr.getAnswers();
                dto.setLifestyleAnswers(answers);
                dto.setLifestyleTags(insights.tagsFromAnswers(answers));
            } else {
                dto.setLifestyleTags(insights.tagsForUser(otherId));
            }
        } catch (Exception e) {
            dto.setLifestyleTags(insights.tagsForUser(otherId));
        }

        if (myEmailOrNull != null) {
            dto.setWhyYouMatch(insights.whyYouMatch(myEmailOrNull, otherId));
        }

        return dto; // <-- you were missing this
    }

    /**
     * Card mapper for the featured list
     */
    public RoomFinderPublicDto toPublicDto(RoomFinder u) {
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
        d.setEmailVerified(u.isSchoolEmailVerified());
        d.setIdentityVerified(computeIdentityVerified(u));
        d.setAlreadyHasRoom(u.isAlreadyHasRoom());
        d.setStatus(u.getStatus());
        return d;
    }
}
