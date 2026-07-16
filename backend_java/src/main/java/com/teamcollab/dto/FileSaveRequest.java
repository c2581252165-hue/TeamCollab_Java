package com.teamcollab.dto;
import lombok.Data;
@Data
public class FileSaveRequest {
    private String path;
    private String content;
    private String encoding;
}
