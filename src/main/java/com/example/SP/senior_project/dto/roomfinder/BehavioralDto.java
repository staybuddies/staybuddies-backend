package com.example.SP.senior_project.dto.roomfinder;

import lombok.Data;


@Data
public class BehavioralDto {
    private Boolean locationSharing;
    private Boolean emailNotification;
    // NEW: strings “HH:mm” to keep client/server simple
    private String bedtime;   // "23:00"
    private String wakeTime;  // "07:00"

    private Integer spendFood;
    private Integer spendEntertainment;
    private Integer spendUtilities;
}
