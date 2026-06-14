package com.interview.platform.service;

import com.interview.platform.enums.Language;
import com.interview.platform.enums.SubmissionStatus;
import com.interview.platform.models.Submission;
import com.interview.platform.models.User;
import com.interview.platform.repository.SubmissionRepository;
import com.interview.platform.request.ExecutionRequest;
import com.interview.platform.response.ExecutionResult;
import com.interview.platform.repository.InterviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final SubmissionRepository submissionRepository;
    private final InterviewRepository interviewRepository;

    @Value("${app.code-exec.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${app.code-exec.work-dir:/tmp/code_exec}")
    private String workDir;

    @Value("${app.code-exec.memory-limit:128m}")
    private String memoryLimit;

    @Value("${app.code-exec.cpu-limit:0.5}")
    private String cpuLimit;

    private record LangConfig(String image, String filename, List<String> compileCmd, List<String> runCmd) {}

    private LangConfig getConfig(Language lang) {
        return switch (lang) {
            case PYTHON -> new LangConfig("python:3.10-alpine", "solution.py",
                null, List.of("python3", "solution.py"));
            case JAVA -> new LangConfig("eclipse-temurin:21-jdk-alpine", "Main.java",
                List.of("javac", "Main.java"), List.of("java", "Main"));
            case GO -> new LangConfig("golang:1.21-alpine", "solution.go",
                null, List.of("go", "run", "solution.go"));
            case CPP -> new LangConfig("gcc:latest", "solution.cpp",
                List.of("g++", "-o", "solution", "solution.cpp"), List.of("./solution"));
            case C -> new LangConfig("gcc:latest", "solution.c",
                List.of("gcc", "-o", "solution", "solution.c"), List.of("./solution"));
        };
    }

    @Transactional
    public ExecutionResult execute(ExecutionRequest request, User submittedBy) {
        String runId = UUID.randomUUID().toString();
        Path runDir = Path.of(workDir, runId);

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

        try {
            Files.createDirectories(runDir);
            LangConfig config = getConfig(request.language());
            Path sourceFile = runDir.resolve(config.filename());
            Files.writeString(sourceFile, request.code());

            // Write stdin if provided
            if (request.stdin() != null && !request.stdin().isBlank()) {
                Files.writeString(runDir.resolve("stdin.txt"), request.stdin());
            }

            long startTime = System.currentTimeMillis();

            // Compile step if needed
            if (config.compileCmd() != null) {
                ExecutionResult compileResult = runInDocker(config.image(), runDir, config.compileCmd(),
                    request.stdin(), runId + "-compile");
                if (compileResult.exitCode() != 0) {
                    return finalizeSubmission(submission, SubmissionStatus.ERROR,
                        "", compileResult.stderr(), System.currentTimeMillis() - startTime);
                }
            }

            // Run step
            ExecutionResult runResult = runInDocker(config.image(), runDir, config.runCmd(),
                request.stdin(), runId);

            long elapsed = System.currentTimeMillis() - startTime;
            SubmissionStatus status = runResult.timedOut() ? SubmissionStatus.TIMEOUT
                : runResult.exitCode() == 0 ? SubmissionStatus.COMPLETED : SubmissionStatus.ERROR;

            return finalizeSubmission(submission, status,
                runResult.stdout(), runResult.stderr(), elapsed);

        } catch (Exception e) {
            log.error("Code execution failed for run {}: {}", runId, e.getMessage(), e);
            return finalizeSubmission(submission, SubmissionStatus.ERROR, "", e.getMessage(), 0);
        } finally {
            deleteDirectory(runDir);
        }
    }

    private ExecutionResult runInDocker(String image, Path runDir, List<String> cmd,
                                        String stdin, String containerId) throws IOException, InterruptedException {
        List<String> dockerCmd = new java.util.ArrayList<>(List.of(
            "docker", "run", "--rm",
            "--name", "exec-" + containerId.substring(0, Math.min(containerId.length(), 20)),
            "-v", runDir.toAbsolutePath() + ":/workspace",
            "-w", "/workspace",
            "--network", "none",
            "--memory", memoryLimit,
            "--cpus", cpuLimit,
            "--read-only",
            "--tmpfs", "/tmp:size=64m",
            image
        ));
        dockerCmd.addAll(cmd);

        ProcessBuilder pb = new ProcessBuilder(dockerCmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // Provide stdin if any
        if (stdin != null && !stdin.isBlank()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(stdin.getBytes());
            }
        } else {
            process.getOutputStream().close();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = executor.submit(() -> new String(process.getInputStream().readAllBytes()));
        Future<String> stderrFuture = executor.submit(() -> new String(process.getErrorStream().readAllBytes()));

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            executor.shutdownNow();
            return new ExecutionResult("", "Execution timed out after " + timeoutSeconds + "s", -1, true, null);
        }

        String stdout = "";
        String stderr = "";
        try {
            stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
            stderr = stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        executor.shutdown();
        return new ExecutionResult(stdout, stderr, process.exitValue(), false, null);
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

    private void deleteDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("Failed to delete run directory: {}", path);
        }
    }
}
