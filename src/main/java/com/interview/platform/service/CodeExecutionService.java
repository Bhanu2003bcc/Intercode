package com.interview.platform.service;

import com.interview.platform.client.PistonClient;
import com.interview.platform.dto.piston.PistonRequestDto;
import com.interview.platform.dto.piston.PistonResponseDto;
import com.interview.platform.enums.Language;
import com.interview.platform.enums.SubmissionStatus;
import com.interview.platform.models.Submission;
import com.interview.platform.models.User;
import com.interview.platform.repository.SubmissionRepository;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.request.ExecutionRequest;
import com.interview.platform.response.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final SubmissionRepository submissionRepository;
    private final InterviewRepository interviewRepository;
    private final PistonClient pistonClient;

    @Value("${app.piston.timeout-seconds:15}")
    private int timeoutSeconds;

    private record LangConfig(String language, String version, String filename) {}

    private LangConfig getConfig(Language lang) {
        return switch (lang) {
            case PYTHON -> new LangConfig("python", "3.10.0", "solution.py");
            case JAVA -> new LangConfig("java", "15.0.2", "Main.java");
            case GO -> new LangConfig("go", "1.16.2", "solution.go");
            case CPP -> new LangConfig("c++", "10.2.0", "solution.cpp");
            case C -> new LangConfig("c", "10.2.0", "solution.c");
        };
    }

    @Transactional
    public ExecutionResult execute(ExecutionRequest request, User submittedBy) {
        Submission submission = Submission.builder()
            .submittedBy(submittedBy)
            .language(request.language())
            .code(request.code())
            .stdin(request.stdin())
            .status(SubmissionStatus.RUNNING)
            .build();

        if (request.interviewId() != null) {
            interviewRepository.findById(request.interviewId())
                .ifPresent(submission::setInterview);
        }
        submissionRepository.save(submission);

        long startTime = System.currentTimeMillis();

        try {
            LangConfig langConfig = getConfig(request.language());
            
            PistonRequestDto.PistonFile pistonFile = PistonRequestDto.PistonFile.builder()
                .name(langConfig.filename())
                .content(request.code())
                .build();

            PistonRequestDto requestDto = PistonRequestDto.builder()
                .language(langConfig.language())
                .version(langConfig.version())
                .files(List.of(pistonFile))
                .stdin(request.stdin() != null ? request.stdin() : "")
                .args(List.of())
                .compileTimeout(timeoutSeconds * 1000)
                .runTimeout(timeoutSeconds * 1000)
                .build();

            PistonResponseDto response = pistonClient.execute(requestDto);
            long elapsed = System.currentTimeMillis() - startTime;

            if (response == null) {
                return finalizeSubmission(submission, SubmissionStatus.ERROR,
                    "", "No response from code execution service", elapsed);
            }

            // Check compile phase (if compilation was performed and failed)
            PistonResponseDto.ExecutionResultDto compile = response.getCompile();
            if (compile != null && compile.getCode() != null && compile.getCode() != 0) {
                String compileStderr = compile.getStderr() != null ? compile.getStderr() : "";
                String compileStdout = compile.getStdout() != null ? compile.getStdout() : "";
                String errorMsg = compileStderr;
                if (!compileStdout.isEmpty()) {
                    errorMsg = compileStdout + "\n" + compileStderr;
                }
                return finalizeSubmission(submission, SubmissionStatus.ERROR,
                    "", errorMsg, elapsed);
            }

            // Check run phase
            PistonResponseDto.ExecutionResultDto run = response.getRun();
            if (run == null) {
                return finalizeSubmission(submission, SubmissionStatus.ERROR,
                    "", "Execution completed, but run results are missing", elapsed);
            }

            boolean timedOut = "SIGKILL".equals(run.getSignal()) || run.getCode() == null;
            SubmissionStatus status;
            if (timedOut) {
                status = SubmissionStatus.TIMEOUT;
            } else if (run.getCode() != 0) {
                status = SubmissionStatus.ERROR;
            } else {
                status = SubmissionStatus.COMPLETED;
            }

            String stdout = run.getStdout() != null ? run.getStdout() : "";
            String stderr = run.getStderr() != null ? run.getStderr() : "";

            return finalizeSubmission(submission, status, stdout, stderr, elapsed);

        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return finalizeSubmission(submission, SubmissionStatus.ERROR, "", e.getMessage(), elapsed);
        }
    }

    private ExecutionResult finalizeSubmission(Submission sub, SubmissionStatus status,
                                               String stdout, String stderr, long elapsed) {
        sub.setStatus(status);
        sub.setStdout(stdout);
        sub.setStderr(stderr);
        sub.setExecutionTimeMs((int) elapsed);
        submissionRepository.save(sub);
        return new ExecutionResult(stdout, stderr, status == SubmissionStatus.COMPLETED ? 0 : 1,
            status == SubmissionStatus.TIMEOUT, sub.getId());
    }
}
