package com.test.git.process;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.Scanner;

public class GitRepoEditor {

    private static final String WORK_DIR = "./temp-repo";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Git Repo File Editor ===");

        while (true) {
            try {
                System.out.print("\nEnter GitHub repo URL (e.g. https://github.com/user/repo.git): ");
                String repoUrl = scanner.nextLine().trim();

                System.out.print("\nEnter GitHub username:  ");
                String username = scanner.nextLine().trim();

                System.out.print("\nEnter GitHub token: ");
                String token = scanner.nextLine().trim();
                
                // runGitRepoOperation(repoUrl, operation, fileName, fileContent);
                cleanAndCloneRepo(repoUrl, token);

                while (true) { 
                    System.out.print("Enter operation (add/update): ");
                    String operation = scanner.nextLine().trim().toLowerCase();

                    System.out.print("Enter file name (e.g. apple.txt): ");
                    String fileName = scanner.nextLine().trim();

                    System.out.print("Enter file content: ");
                    String fileContent = scanner.nextLine();

                    runGitRepoOperation(repoUrl, token, username, operation, fileName, fileContent);
                }

            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private static void cleanAndCloneRepo(String repoUrl, String token) throws Exception {
        String secureUrl = repoUrl.replace("https://", "https://" + token + "@");

        Path repoDir = Paths.get(WORK_DIR);
        deleteDirectory(repoDir.toFile()); // Clean up previous

        System.out.println("Cloning repo with access token...");
        runCommandWithEnv(null, Map.of(), "git", "clone", secureUrl, WORK_DIR);
    }

    private static void runGitRepoOperation(String repoUrl, String token, String username, String operation, String fileName, String content) throws Exception {
        Path repoDir = Paths.get(WORK_DIR);
        Path filePath = repoDir.resolve(fileName);

        if ((operation.equalsIgnoreCase("add")) && Files.exists(filePath)) {
            System.out.println("File already exists. Use update instead.");
            return;
        }

        if (operation.equals("update") && !Files.exists(filePath)) {
            System.out.println("File does not exist. Use add instead.");
            return;
        }

        Files.writeString(filePath, (operation.equals("update") ? Files.readString(filePath) : "") + content);

        Map<String, String> gitEnv = Map.of(
            "GIT_AUTHOR_NAME", username,
            "GIT_COMMITTER_NAME", username,
            "GIT_AUTHOR_EMAIL", username + "@gmail.com",
            "GIT_COMMITTER_EMAIL", username + "@gmail.com"
        );

        runCommandWithEnv(repoDir, gitEnv, "git", "add", fileName);
        runCommandWithEnv(repoDir, gitEnv, "git", "commit", "-m", operation + " file " + fileName);
        runCommandWithEnv(repoDir, gitEnv, "git", "push");
        System.out.println("Changes pushed successfully.");
    }

    private static void runCommandWithEnv(Path directory, Map<String, String> envVars, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (directory != null) builder.directory(directory.toFile());
        builder.redirectErrorStream(true);
        builder.environment().putAll(envVars);

        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        if (process.waitFor() != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }

    private static void runCommand(String... command) throws Exception {
        runCommandInDir(null, command);
    }

    private static void runCommandInDir(Path directory, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        builder.redirectErrorStream(true);

        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        if (process.waitFor() != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }

    private static void deleteDirectory(File file) throws IOException {
        if (!file.exists()) return;
        Files.walk(file.toPath())
             .sorted((a, b) -> b.compareTo(a)) // reverse; delete children first
             .forEach(path -> {
                 try {
                     Files.delete(path);
                 } catch (IOException e) {
                     System.err.println("Failed to delete " + path);
                 }
             });
    }
}
