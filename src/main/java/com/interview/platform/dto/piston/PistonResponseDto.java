package com.interview.platform.dto.piston;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PistonResponseDto {
    private String language;
    private String version;
    private ExecutionResultDto run;
    private ExecutionResultDto compile;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionResultDto {
        private String stdout;
        private String stderr;
        private Integer code;
        private String signal;
        private String output;
    }
}
