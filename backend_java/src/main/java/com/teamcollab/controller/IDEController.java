package com.teamcollab.controller;

import com.teamcollab.entity.User;
import com.teamcollab.repository.UserRepository;
import com.teamcollab.dto.FileSaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/ide")
public class IDEController {

    @Autowired private UserRepository userRepository;

    private static final String PROJECTS_ROOT_DIR = "project_workspaces";
    private static final Set<String> IGNORE_DIRS = new HashSet<>(Arrays.asList(".git", "node_modules", "venv", "__pycache__", ".vscode", "dist", ".gemini"));
    private static final Set<String> IGNORE_EXTS = new HashSet<>(Arrays.asList(".pyc", ".log", ".sqlite", ".db"));

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    private File getProjectDir(Integer projectId) throws IOException {
        File root = new File(PROJECTS_ROOT_DIR).getAbsoluteFile();
        if (!root.exists()) root.mkdirs();
        
        File projDir = new File(root, "project_" + projectId);
        if (!projDir.exists()) {
            projDir.mkdirs();
            Files.write(new File(projDir, "main.py").toPath(), "print(\"Hello from TeamCollab!\")\n\ndef calculate_sum(a, b):\n    return a + b\n".getBytes("UTF-8"));
            Files.write(new File(projDir, "README.md").toPath(), ("# Project " + projectId + " Workspace\n\nWelcome to your team's isolated code workspace!\n").getBytes("UTF-8"));
            Files.write(new File(projDir, "announcement.md").toPath(), ("# 项目 " + projectId + " 公告板\n\n欢迎来到本项目！").getBytes("UTF-8"));
        }
        return projDir;
    }

    private List<Map<String, Object>> buildTree(File dir, File baseDir) {
        List<Map<String, Object>> tree = new ArrayList<>();
        File[] entries = dir.listFiles();
        if (entries == null) return tree;

        Arrays.sort(entries, Comparator.comparing(File::getName));

        for (File entry : entries) {
            if (IGNORE_DIRS.contains(entry.getName())) continue;

            String relPath = baseDir.toURI().relativize(entry.toURI()).getPath();
            if (relPath.endsWith("/")) relPath = relPath.substring(0, relPath.length() - 1);

            if (entry.isDirectory()) {
                List<Map<String, Object>> children = buildTree(entry, baseDir);
                Map<String, Object> node = new HashMap<>();
                node.put("name", entry.getName());
                node.put("path", relPath);
                node.put("type", "directory");
                node.put("children", children);
                tree.add(node);
            } else {
                String name = entry.getName();
                String ext = "";
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) ext = name.substring(dotIndex);
                if (!IGNORE_EXTS.contains(ext.toLowerCase()) && !name.endsWith(".sqlite3")) {
                    Map<String, Object> node = new HashMap<>();
                    node.put("name", name);
                    node.put("path", relPath);
                    node.put("type", "file");
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @GetMapping("/files")
    public Map<String, Object> getFilesTree() throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("name", "No Project Selected");
            res.put("path", "");
            res.put("type", "directory");
            res.put("children", new ArrayList<>());
            return res;
        }

        File projDir = getProjectDir(user.getCurrentProjectId());
        List<Map<String, Object>> tree = buildTree(projDir, projDir);
        
        Map<String, Object> res = new HashMap<>();
        res.put("name", "Project_" + user.getCurrentProjectId() + "_Workspace");
        res.put("path", "");
        res.put("type", "directory");
        res.put("children", tree);
        return res;
    }

    @GetMapping("/content")
    public Map<String, String> getFileContent(@RequestParam("path") String path, @RequestParam(value = "encoding", defaultValue = "UTF-8") String encoding) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        if (path.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File target = new File(projDir, path);
        if (!target.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");
        if (!target.exists() || !target.isFile()) throw new RuntimeException("File not found");

        try {
            String content = new String(Files.readAllBytes(target.toPath()), encoding);
            return Collections.singletonMap("content", content);
        } catch (Exception e) {
            return Collections.singletonMap("content", "/* Binary or non-text file cannot be displayed with encoding " + encoding + " */");
        }
    }

    @PostMapping("/content")
    public Map<String, String> saveFileContent(@RequestBody FileSaveRequest req) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        if (req.getPath().contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File target = new File(projDir, req.getPath());
        if (!target.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");

        if (target.getParentFile() != null && !target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }

        String encoding = req.getEncoding() != null && !req.getEncoding().isEmpty() ? req.getEncoding() : "UTF-8";
        Files.write(target.toPath(), req.getContent().getBytes(encoding));
        return Collections.singletonMap("message", "Saved successfully");
    }

    @PostMapping("/directory")
    public Map<String, String> createDirectory(@RequestBody Map<String, String> req) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        String path = req.get("path");
        if (path == null || path.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File target = new File(projDir, path);
        if (!target.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");

        if (target.exists()) throw new RuntimeException("Directory already exists");
        target.mkdirs();
        return Collections.singletonMap("message", "Directory created successfully");
    }

    @PutMapping("/rename")
    public Map<String, String> renameFile(@RequestBody Map<String, String> req) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        String oldPath = req.get("old_path");
        String newPath = req.get("new_path");
        if (oldPath == null || newPath == null || oldPath.contains("..") || newPath.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File oldTarget = new File(projDir, oldPath);
        File newTarget = new File(projDir, newPath);
        if (!oldTarget.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");
        if (!newTarget.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");

        if (!oldTarget.exists()) throw new RuntimeException("Original file not found");
        if (newTarget.exists()) throw new RuntimeException("Target name already exists");

        oldTarget.renameTo(newTarget);
        return Collections.singletonMap("message", "Renamed successfully");
    }

    @DeleteMapping("/file")
    public Map<String, String> deleteFile(@RequestParam("path") String path) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        if (path == null || path.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File target = new File(projDir, path);
        if (!target.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");
        if (target.getCanonicalPath().equals(projDir.getCanonicalPath())) throw new RuntimeException("Cannot delete root directory");
        if (!target.exists()) throw new RuntimeException("File/Directory not found");

        org.springframework.util.FileSystemUtils.deleteRecursively(target);
        return Collections.singletonMap("message", "Deleted successfully");
    }

    @PostMapping("/upload")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("path") String path) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        if (path == null || path.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File targetDir = new File(projDir, path);
        if (!targetDir.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");

        if (!targetDir.exists()) targetDir.mkdirs();
        
        Path targetPath = new File(targetDir, file.getOriginalFilename()).getAbsoluteFile().toPath();
        Files.write(targetPath, file.getBytes());
        
        return Collections.singletonMap("message", "File uploaded successfully");
    }

    @GetMapping("/download-file")
    public ResponseEntity<FileSystemResource> downloadSingleFile(@RequestParam("path") String path) throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");
        if (path == null || path.contains("..")) throw new RuntimeException("Invalid path");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File target = new File(projDir, path);
        if (!target.getCanonicalPath().toLowerCase().startsWith(projDir.getCanonicalPath().toLowerCase())) throw new RuntimeException("Path outside project bounds");
        if (!target.exists() || target.isDirectory()) throw new RuntimeException("File not found");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + target.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(target.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(target));
    }

    @GetMapping("/download")
    public ResponseEntity<FileSystemResource> downloadSourceCode() throws IOException {
        User user = getCurrentUser();
        if (user.getCurrentProjectId() == null) throw new RuntimeException("No project selected");

        File projDir = getProjectDir(user.getCurrentProjectId());
        File zipFile = new File("Project_" + user.getCurrentProjectId() + "_Source.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Path projPath = projDir.toPath();
            Files.walk(projPath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                String relPath = projPath.relativize(path).toString();
                // Simple ignore check
                boolean skip = false;
                for (String dir : IGNORE_DIRS) {
                    if (relPath.contains(dir + File.separator) || relPath.startsWith(dir)) skip = true;
                }
                for (String ext : IGNORE_EXTS) {
                    if (relPath.endsWith(ext)) skip = true;
                }
                if (relPath.endsWith(".sqlite3")) skip = true;

                if (!skip) {
                    ZipEntry entry = new ZipEntry(relPath.replace("\\", "/"));
                    try {
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(zipFile));
    }
}
