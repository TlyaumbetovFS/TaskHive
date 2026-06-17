package com.taskhive.controller;

import com.taskhive.dto.CommentDto;
import com.taskhive.dto.TaskDto;
import com.taskhive.service.CommentService;
import com.taskhive.service.ProjectService;
import com.taskhive.service.TaskService;
import com.taskhive.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;
    private final ProjectService projectService;
    private final WorkspaceService workspaceService;

    @GetMapping("/workspaces/{workspaceId}/projects/{projectId}")
    public String list(@PathVariable Long workspaceId, @PathVariable Long projectId,
                       Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var project = projectService.getById(projectId);
        var tasks = taskService.getTasksByProjectId(projectId);
        model.addAttribute("tasks", tasks);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("workspaceId", workspaceId);
        return "task-list";
    }

    @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/create")
    public String showCreateForm(@PathVariable Long workspaceId, @PathVariable Long projectId,
                                 Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        model.addAttribute("taskDto", new TaskDto());
        model.addAttribute("projectId", projectId);
        model.addAttribute("workspaceId", workspaceId);
        return "task-create";
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/create")
    public String create(@PathVariable Long workspaceId, @PathVariable Long projectId,
                         @Valid @ModelAttribute TaskDto dto, BindingResult result,
                         Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        if (result.hasErrors()) {
            model.addAttribute("projectId", projectId);
            model.addAttribute("workspaceId", workspaceId);
            return "task-create";
        }

        taskService.create(dto, projectId, principal.getName());
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId;
    }

    @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}")
    public String detail(@PathVariable Long workspaceId, @PathVariable Long projectId,
                         @PathVariable Long taskId, Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var task = taskService.getById(taskId);
        var comments = commentService.getByTaskId(taskId);
        var allowedStatuses = taskService.getAllowedTransitions(task);

        model.addAttribute("task", task);
        model.addAttribute("comments", comments);
        model.addAttribute("allowedStatuses", allowedStatuses);
        model.addAttribute("commentDto", new CommentDto());
        model.addAttribute("workspaceId", workspaceId);
        model.addAttribute("projectId", projectId);
        return "task-detail";
    }

    @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/edit")
    public String showEditForm(@PathVariable Long workspaceId, @PathVariable Long projectId,
                               @PathVariable Long taskId, Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var task = taskService.getById(taskId);

        var editDto = TaskDto.builder()
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .assigneeEmail(task.getAssignee() != null ? task.getAssignee().getEmail() : null)
                .build();

        model.addAttribute("taskDto", editDto);
        model.addAttribute("task", task);
        model.addAttribute("workspaceId", workspaceId);
        model.addAttribute("projectId", projectId);
        model.addAttribute("taskId", taskId);
        return "task-edit";
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/edit")
    public String update(@PathVariable Long workspaceId, @PathVariable Long projectId,
                         @PathVariable Long taskId, @Valid @ModelAttribute TaskDto dto,
                         BindingResult result, Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        if (result.hasErrors()) {
            model.addAttribute("workspaceId", workspaceId);
            model.addAttribute("projectId", projectId);
            model.addAttribute("taskId", taskId);
            return "task-edit";
        }

        taskService.update(taskId, dto);
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + taskId;
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/status")
    public String changeStatus(@PathVariable Long workspaceId, @PathVariable Long projectId,
                               @PathVariable Long taskId, @RequestParam Long statusId,
                               Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());
        taskService.changeStatus(taskId, statusId);
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + taskId;
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/delete")
    public String delete(@PathVariable Long workspaceId, @PathVariable Long projectId,
                         @PathVariable Long taskId, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());
        taskService.softDelete(taskId);
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId;
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/comments")
    public String addComment(@PathVariable Long workspaceId, @PathVariable Long projectId,
                             @PathVariable Long taskId, @Valid @ModelAttribute CommentDto dto,
                             BindingResult result, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        if (!result.hasErrors()) {
            commentService.create(dto, taskId, principal.getName());
        }
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + taskId;
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks/{taskId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long workspaceId, @PathVariable Long projectId,
                                @PathVariable Long taskId, @PathVariable Long commentId,
                                Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());
        commentService.softDelete(commentId, principal.getName());
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + taskId;
    }
}
