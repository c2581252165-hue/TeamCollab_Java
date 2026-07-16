package com.teamcollab.dto;
import lombok.Data;
@Data
public class PointUpdate {
    private Integer amount;
    private String reason = "队长操作";
}
