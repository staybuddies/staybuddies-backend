package com.example.SP.senior_project.service.impl;

import com.example.SP.senior_project.model.EmailOtp;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.EmailOtpRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailOtpRepository otpRepo;
    private final RoomFinderRepository roomFinderRepo;

    private static String generate6() {
        int n = new Random().nextInt(900_000) + 100_000;
        return Integer.toString(n);
    }

    @Override
    public void sendCode(Long roomFinderId, String email) {
        // delete old codes for this email
        otpRepo.deleteByEmail(email);

        EmailOtp otp = new EmailOtp();
        otp.setRoomFinderId(roomFinderId);
        otp.setEmail(email);
        otp.setCode(generate6());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otpRepo.save(otp);

        // TODO: send real email. For now, log it.
        log.info("[EMAIL OTP] to {} -> {}", email, otp.getCode());
    }

    @Override
    public void confirmCode(Long roomFinderId, String email, String code) {
        EmailOtp otp = otpRepo.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new RuntimeException("No code issued for this email"));
        if (!otp.getCode().equals(code)) {
            throw new RuntimeException("Invalid code");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expired");
        }
        // persist on the user
        RoomFinder me = roomFinderRepo.findById(roomFinderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        me.setSchoolEmail(email);
        me.setSchoolEmailVerified(true);
        roomFinderRepo.save(me);

        // cleanup
        otpRepo.deleteByEmail(email);
    }
}
