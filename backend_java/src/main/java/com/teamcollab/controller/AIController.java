package com.teamcollab.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamcollab.dto.AIRequests.*;
import com.teamcollab.entity.*;
import com.teamcollab.repository.*;
import com.teamcollab.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private StandupRepository standupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogRepository logRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    @PostMapping("/breakdown-task")
    public List<Task> aiBreakdownTask(@RequestBody Breakdown req) {
        String prompt = "You are an expert software engineering manager. Break down the following macro-goal into a list of specific, executable subtasks for a software development team.\n" +
                "Goal: " + req.getGoal() + "\n" +
                "请务必使用全中文输出。对于简单的目标，生成的任务数量尽量少（只精简出最核心的步骤即可，不要过度拆分）。\n" +
                "Provide the output strictly as a JSON array of objects, with each object having:\n" +
                "- \"title\": a short task title (in Chinese)\n" +
                "- \"description\": a brief description (in Chinese)\n" +
                "- \"phase\": one of [\"requirement\", \"design\", \"coding\", \"test\"]\n" +
                "- \"weight\": an integer from 1 to 5 representing difficulty.\n" +
                "Do not include any other text besides the JSON array.";

        String response = aiService.callAI(null, prompt);

        List<Task> createdTasks = new ArrayList<>();
        try {
            String cleanContent = response.replace("```json", "").replace("```", "").trim();
            Matcher matcher = Pattern.compile("\\[.*?\\]", Pattern.DOTALL).matcher(cleanContent);
            
            List<Map<String, Object>> tasksData;
            if (matcher.find()) {
                tasksData = objectMapper.readValue(matcher.group(0), new TypeReference<List<Map<String, Object>>>() {});
            } else {
                tasksData = objectMapper.readValue(cleanContent, new TypeReference<List<Map<String, Object>>>() {});
            }

            for (Map<String, Object> t : tasksData) {
                Task task = new Task();
                task.setTitle(String.valueOf(t.getOrDefault("title", "未命名任务")));
                task.setDescription(String.valueOf(t.getOrDefault("description", "")));
                String phase = String.valueOf(t.getOrDefault("phase", "none")).toLowerCase();
                if (!Arrays.asList("requirement", "design", "coding", "test", "none").contains(phase)) {
                    phase = "none";
                }
                task.setPhase(phase);
                
                try {
                    task.setWeight(Integer.parseInt(String.valueOf(t.getOrDefault("weight", "1"))));
                } catch (Exception e) {
                    task.setWeight(1);
                }
                
                task.setProjectId(req.getProjectId());
                task.setStatus("todo");
                task.setIsDeleted(false);
                taskRepository.save(task);
                createdTasks.add(task);
            }
            
            if (!createdTasks.isEmpty()) {
                Log log = new Log();
                log.setUserId(getCurrentUser().getId());
                log.setProjectId(req.getProjectId());
                log.setAction("create");
                log.setTargetType("Task");
                log.setTargetId(createdTasks.get(0).getId());
                log.setDetails("通过 AI 智能拆解了一键目标: " + req.getGoal() + "，共生成了 " + createdTasks.size() + " 个子任务");
                logRepository.save(log);
            }
        } catch (Exception e) {
            throw new RuntimeException("AI 解析任务拆解失败: " + e.getMessage());
        }

        return createdTasks;
    }

    @PostMapping("/weekly-report")
    public Map<String, String> generateWeeklyReport(@RequestBody WeeklyReport req) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(req.getProjectId()).stream()
                .filter(t -> "done".equals(t.getStatus()))
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            return Collections.singletonMap("report", "本周没有任何已完成的任务来生成周报。");
        }

        StringBuilder tasksText = new StringBuilder();
        for (Task t : tasks) {
            String assigneeName = (t.getAssignedTo() != null) ? t.getAssignedTo().getUsername() : "Unassigned";
            tasksText.append(String.format("- %s (Phase: %s, Completed by: %s)\n", t.getTitle(), t.getPhase(), assigneeName));
        }

        String prompt = "You are a project manager. Create a VERY SHORT and concise weekly progress report based on the following completed tasks.\n" +
                "请务必使用全中文撰写这份报告。请将字数严格控制在 200 字以内，简明扼要地列出核心进展即可，切忌长篇大论。\n" +
                "Completed Tasks:\n" + tasksText.toString() + "\n" +
                "Format the output nicely using Markdown.";

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("report", content);
    }

    @PostMapping("/chat")
    public Map<String, String> aiChat(@RequestBody Chat req) {
        String prompt = "You are an AI assistant for a software development project management tool.\n" +
                "Be helpful, concise, and friendly. Answer the user's question or respond to their input.\n" +
                "Always reply in Chinese.\n" +
                "IMPORTANT: If the user asks you to create files (or projects involving new files), you MUST output a special XML block for each file you want to create.\n" +
                "Format EXACTLY like this (with NO markdown code blocks around it):\n" +
                "<create_file path=\"filename.py\">\n" +
                "your code here\n" +
                "</create_file>\n\n" +
                "User: " + req.getMessage();
        
        String content = aiService.callAI("You are a helpful software engineering assistant.", prompt);
        return Collections.singletonMap("reply", content);
    }

    @PostMapping("/standup-summary")
    public Map<String, String> generateStandupSummary(@RequestBody WeeklyReport req) {
        LocalDate today = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDate();
        List<Standup> standups = standupRepository.findByProjectId(req.getProjectId());
        
        List<Standup> todayStandups = standups.stream()
                .filter(s -> s.getDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate().equals(today))
                .collect(Collectors.toList());

        if (todayStandups.isEmpty()) {
            return Collections.singletonMap("reply", "今天团队里还没有人提交站会记录哦，无法为您生成总结。");
        }

        StringBuilder standupContext = new StringBuilder();
        for (Standup s : todayStandups) {
            String username = (s.getUser() != null) ? s.getUser().getUsername() : "未知成员";
            standupContext.append(String.format("【%s】昨日完成: %s | 今日计划: %s | 遇到困难: %s\n", username, s.getYesterday(), s.getToday(), s.getBlockers()));
        }

        String prompt = "你是团队的敏捷教练。请基于以下团队成员今天的站会记录，用一段全中文的简短文字（不超过150字）总结团队目前的推进状态、核心聚焦的任务，并重点圈出哪些人遇到了阻碍（Blockers）。语气要轻松活泼。\n\n" +
                "站会记录：\n" + standupContext.toString();

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("reply", content);
    }

    @PostMapping("/risk-assessment")
    public Map<String, String> generateRiskAssessment(@RequestBody WeeklyReport req) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(req.getProjectId()).stream()
                .filter(t -> !"done".equals(t.getStatus()))
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            return Collections.singletonMap("reply", "当前项目没有正在进行中的任务，看起来一切都尽在掌握！");
        }

        StringBuilder taskContext = new StringBuilder();
        for (Task t : tasks) {
            String assignee = (t.getAssignedTo() != null) ? t.getAssignedTo().getUsername() : "未分配";
            String deadline = (t.getDeadline() != null) ? t.getDeadline().toString() : "无截止日期";
            taskContext.append(String.format("任务名: %s | 状态: %s | 负责人: %s | 截止日期: %s | 难度权重: %d\n", t.getTitle(), t.getStatus(), assignee, deadline, t.getWeight()));
        }

        String prompt = "你是项目的风险控制专家。请基于以下未完成的任务列表，简要分析目前项目是否存在延期风险（重点关注没有分配负责人的任务、或者权重很大但尚未开始的任务）。用全中文回答，字数控制在200字以内，直接给出结论和预警。\n\n" +
                "未完成任务列表：\n" + taskContext.toString();

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("reply", content);
    }

    @PostMapping("/workload-analysis")
    public Map<String, String> generateWorkloadAnalysis(@RequestBody WeeklyReport req) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(req.getProjectId()).stream()
                .filter(t -> !"done".equals(t.getStatus()))
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            return Collections.singletonMap("reply", "当前项目没有正在进行中的任务，大家都很清闲！");
        }

        Map<String, int[]> workload = new HashMap<>();
        for (Task t : tasks) {
            String assignee = (t.getAssignedTo() != null) ? t.getAssignedTo().getUsername() : "未分配";
            workload.putIfAbsent(assignee, new int[]{0, 0});
            workload.get(assignee)[0] += 1;
            workload.get(assignee)[1] += (t.getWeight() != null ? t.getWeight() : 1);
        }

        StringBuilder workloadContext = new StringBuilder();
        for (Map.Entry<String, int[]> entry : workload.entrySet()) {
            workloadContext.append(String.format("成员: %s | 负责任务数: %d | 总难度权重: %d\n", entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        String prompt = "你是项目的人力资源分配官。基于以下团队成员目前的未完成任务工作量统计，请用一段简短的全中文（150字以内）点评一下团队的劳动力分配情况。指出谁可能是最累的（瓶颈），谁比较轻松（可以分担工作），或者是否有大量未分配的任务。\n\n" +
                "工作量统计：\n" + workloadContext.toString();

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("reply", content);
    }

    @PostMapping("/code-review")
    public Map<String, Object> aiCodeReview(@RequestBody CodeReview req) {
        Task task = taskRepository.findById(req.getTaskId()).orElseThrow(() -> new RuntimeException("Task not found"));
        User currentUser = getCurrentUser();

        // Security check
        ProjectMember membership = projectMemberRepository.findByUserIdAndProjectId(currentUser.getId(), task.getProjectId());
        if (membership == null || !membership.getIsApproved()) {
            throw new RuntimeException("Not authorized");
        }

        String prompt = "You are an expert Senior Software Engineer conducting a Code Review.\n" +
                "Task Title: " + task.getTitle() + "\n" +
                "Task Description: " + task.getDescription() + "\n\n" +
                "Please review the following code snippet. \n" +
                "Provide your output strictly in JSON format with two fields:\n" +
                "- \"score\": an integer from 0 to 100 representing code quality.\n" +
                "- \"comment\": a brief code review comment in Chinese, highlighting bugs, code smells, or praising good practices.\n\n" +
                "Code to review:\n```\n" + req.getCodeSnippet() + "\n```\n\n" +
                "Output exactly JSON and nothing else.";

        String content = aiService.callAI(null, prompt);

        Map<String, Object> result = new HashMap<>();
        try {
            String cleanContent = content.replace("```json", "").replace("```", "").trim();
            Matcher matcher = Pattern.compile("\\{.*?\\}", Pattern.DOTALL).matcher(cleanContent);
            if (matcher.find()) {
                Map<String, Object> reviewData = objectMapper.readValue(matcher.group(0), new TypeReference<Map<String, Object>>() {});
                task.setAiReviewScore(Integer.parseInt(String.valueOf(reviewData.getOrDefault("score", "85"))));
                task.setAiReviewComment(String.valueOf(reviewData.getOrDefault("comment", "无评价")));
            } else {
                task.setAiReviewScore(85);
                task.setAiReviewComment("AI未能正确生成评价格式，但代码看起来可以运行。");
            }
        } catch (Exception e) {
            task.setAiReviewScore(85);
            task.setAiReviewComment("AI 解析失败。");
        }

        task.setStatus("review");
        taskRepository.save(task);

        Log log = new Log();
        log.setUserId(currentUser.getId());
        log.setProjectId(task.getProjectId());
        log.setAction("ai_review");
        log.setTargetType("Task");
        log.setTargetId(task.getId());
        log.setDetails("提交了代码进行AI审查，得分：" + task.getAiReviewScore());
        logRepository.save(log);

        result.put("score", task.getAiReviewScore());
        result.put("comment", task.getAiReviewComment());
        return result;
    }

    @PostMapping("/grade-prediction")
    @Transactional
    public Map<String, String> aiGradePrediction(@RequestBody GradePrediction req) {
        User currentUser = getCurrentUser();
        ProjectMember membership = projectMemberRepository.findByUserIdAndProjectId(currentUser.getId(), req.getProjectId());

        if ((membership == null || !"leader".equals(membership.getRole()) || !membership.getIsApproved()) && !"teacher".equals(currentUser.getRole())) {
            throw new RuntimeException("Only Teacher or Leader can generate grade prediction.");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectIdAndIsApproved(req.getProjectId(), true);
        List<Map<String, Object>> memberStats = new ArrayList<>();

        for (ProjectMember m : members) {
            User user = m.getUser();
            long tasksDone = taskRepository.findByProjectIdAndIsDeletedFalse(req.getProjectId()).stream()
                    .filter(t -> user.getId().equals(t.getAssignedTo() != null ? t.getAssignedTo().getId() : -1) && "done".equals(t.getStatus()))
                    .count();
            long standupsDone = standupRepository.findByProjectId(req.getProjectId()).stream()
                    .filter(s -> user.getId().equals(s.getUser().getId()))
                    .count();
            List<String> badges = m.getBadges().stream().map(ProjectMemberBadge::getBadgeType).collect(Collectors.toList());

            Map<String, Object> stat = new HashMap<>();
            stat.put("username", user.getUsername());
            stat.put("role", m.getRole());
            stat.put("points", user.getPoints());
            stat.put("focus_minutes", m.getFocusMinutes());
            stat.put("tasks_completed", tasksDone);
            stat.put("standups_submitted", standupsDone);
            stat.put("badges_earned", badges);
            memberStats.add(stat);
        }

        String statsJson;
        try {
            statsJson = objectMapper.writeValueAsString(memberStats);
        } catch (Exception e) {
            statsJson = "[]";
        }

        String prompt = "你是大学软件工程课程的高级教授。请基于以下团队成员在项目中的客观数据，自动生成一份详细的《期末智能评估报告》。\n" +
                "请用全中文回复，并且**必须**使用严格的 Markdown 语法排版，禁止只输出纯文本格式！\n\n" +
                "格式规范要求：\n" +
                "- 主标题必须是一级标题 (例如: # 软件工程课程项目期末智能评估报告)\n" +
                "- 板块标题必须是二级标题 (例如: ## 一、整体团队评价)\n" +
                "- 每位成员的单独点评必须是三级标题 (例如: ### 成员1（团队负责人）)\n" +
                "- 最终给出的成绩必须加粗显示 (例如: **建议成绩等级：C**)\n" +
                "- 理由和分项请使用规范的列表符号 (`-` 或 `1.`)\n\n" +
                "包含以下内容：\n" +
                "1. 整体团队评价（简短评价团队活跃度和健康度）\n" +
                "2. 成员表现逐一点评：对于每个成员，综合他们的积分（Points）、专注时长（Focus Minutes）、完成任务数、站会参与度以及获得的徽章（night_owl, bug_hunter, focus_master等）进行点评。\n" +
                "3. 建议成绩等级（A, B, C, D）：在点评末尾给出你建议的最终成绩等级，并给出理由。重点惩罚那些不打卡站会、积分低、0任务的划水成员（给 C 或 D）。奖励获得徽章且积分高的成员（给 A）。\n\n" +
                "团队成员数据如下：\n" + statsJson;

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("report", content);
    }

    @PostMapping("/daily-risk")
    public Map<String, String> generateDailyRisk(@RequestBody GradePrediction req) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(req.getProjectId()).stream()
                .filter(t -> "in_progress".equals(t.getStatus()))
                .collect(Collectors.toList());

        ZonedDateTime yesterdayTime = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(2);
        List<Standup> standups = standupRepository.findByProjectId(req.getProjectId()).stream()
                .filter(s -> s.getDate().toInstant().atZone(ZoneId.of("UTC")).isAfter(yesterdayTime))
                .collect(Collectors.toList());

        StringBuilder taskInfo = new StringBuilder();
        for (Task t : tasks) {
            String assignee = (t.getAssignedTo() != null) ? t.getAssignedTo().getUsername() : "未分配";
            taskInfo.append(String.format("- 任务: %s | 负责人: %s | 难度: %d\n", t.getTitle(), assignee, t.getWeight()));
        }

        StringBuilder standupInfo = new StringBuilder();
        for (Standup s : standups) {
            standupInfo.append(String.format("- %s (昨天): %s\n  (今天): %s\n  (阻塞): %s\n", s.getUser().getUsername(), s.getYesterday(), s.getToday(), s.getBlockers()));
        }

        String prompt = "你是敏捷开发团队的 AI Scrum Master。请根据以下正在进行中的任务和近期的站会汇报，生成一份《项目每日风险预警早报》。\n" +
                "请用全中文回复，**必须**使用 Markdown 排版！\n\n" +
                "要求：\n" +
                "1. 使用 `# 项目每日风险预警早报` 作为标题\n" +
                "2. 分析当前的潜在风险（例如：某个高难度任务停滞、有人提到被阻塞）。\n" +
                "3. 给出你的改进建议。\n" +
                "4. 语言幽默犀利，直击痛点。\n\n" +
                "当前进行中的任务：\n" + (tasks.isEmpty() ? "暂无进行中任务" : taskInfo.toString()) + "\n\n" +
                "近期站会记录：\n" + (standups.isEmpty() ? "近期无站会打卡" : standupInfo.toString());

        String content = aiService.callAI(null, prompt);
        return Collections.singletonMap("report", content);
    }

    @PostMapping("/format-standup")
    public Map<String, String> aiFormatStandup(@RequestBody FormatStandup req) {
        String prompt = "你是一个站会记录助手。用户随口说了一段语音识别出的文本，请你将它拆解成标准的每日站会三大要素：昨天完成、今天计划、遇到的阻塞。\n" +
                "请直接输出一个 JSON 对象，必须严格包含以下三个 key：\n" +
                "- \"yesterday\": 提取昨天完成的内容，如果没有提到则返回\"无\"\n" +
                "- \"today\": 提取今天计划的内容，如果没有提到则返回\"无\"\n" +
                "- \"blockers\": 提取遇到的阻塞或问题，如果没有提到则返回\"无\"\n\n" +
                "用户的原话是：\n\"" + req.getRawText() + "\"\n\n" +
                "只输出纯 JSON，不要有其他前缀或解释。";

        String content = aiService.callAI(null, prompt);

        Map<String, String> data = new HashMap<>();
        try {
            String cleanContent = content.replace("```json", "").replace("```", "").trim();
            Matcher matcher = Pattern.compile("\\{.*?\\}", Pattern.DOTALL).matcher(cleanContent);
            if (matcher.find()) {
                data = objectMapper.readValue(matcher.group(0), new TypeReference<Map<String, String>>() {});
            } else {
                data.put("yesterday", "解析失败");
                data.put("today", "解析失败");
                data.put("blockers", "解析失败");
            }
        } catch (Exception e) {
            data.put("yesterday", "解析失败");
            data.put("today", "解析失败");
            data.put("blockers", "解析失败");
        }
        return data;
    }

    @PostMapping("/generate-code")
    public Map<String, String> aiGenerateCode(@RequestBody GenerateCode req) {
        String prompt = "You are an expert programmer. You are writing code for a file named \"" + req.getFilename() + "\".\n" +
                "The user wants you to implement the following functionality:\n" +
                "\"" + req.getPrompt() + "\"\n\n" +
                "Output ONLY the raw code. Do NOT output any markdown code blocks (like ```python or ```javascript).\n" +
                "Do NOT output any explanations or conversational text. ONLY raw, valid code that can be directly embedded into the file.";

        String content = aiService.callAI(null, prompt);

        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            String[] lines = cleanContent.split("\n");
            if (lines.length > 1) {
                cleanContent = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
            } else {
                cleanContent = "";
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
            }
        }
        return Collections.singletonMap("code", cleanContent.trim());
    }
}
