package com.interview.platform.dto.piston;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PistonRequestDto {
    private String language;
    private String version;
    private List<PistonFile> files;
    private String stdin;
    private List<String> args;

    @JsonProperty("compile_timeout")
    private Integer compileTimeout;

    @JsonProperty("run_timeout")
    private Integer runTimeout;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PistonFile {
        private String name;
        private String content;
    }
}
