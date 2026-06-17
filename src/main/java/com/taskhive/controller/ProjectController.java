package com.taskhive.controller;

import com.taskhive.dto.ProjectDto;
import com.taskhive.dto.WorkspaceMemberDto;
import com.taskhive.model.ProjectRole;
import com.taskhive.service.ProjectService;
import com.taskhive.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final WorkspaceService workspaceService;

    @GetMapping("/workspaces/{workspaceId}")
    public String show(@PathVariable Long workspaceId, Model model, Principal principal, HttpSession session) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var workspace = workspaceService.getById(workspaceId);
        var projects = projectService.getProjectsByWorkspace(workspaceId);

        // SESSION: remember last visited workspace
        session.setAttribute("lastWorkspaceId", workspace.getWorkspaceId());
        session.setAttribute("lastWorkspaceName", workspace.getName());

        model.addAttribute("workspace", workspace);
        model.addAttribute("projects", projects);
        model.addAttribute("workspaceId", workspaceId);
        return "workspace-view";
    }

    @GetMapping("/workspaces/{workspaceId}/projects/create")
    public String showCreateForm(@PathVariable Long workspaceId, Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        model.addAttribute("projectDto", new ProjectDto());
        model.addAttribute("workspaceId", workspaceId);
        return "project-create";
    }

    @PostMapping("/workspaces/{workspaceId}/projects/create")
    public String create(@PathVariable Long workspaceId, @Valid @ModelAttribute ProjectDto dto,
                         BindingResult result, Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        if (result.hasErrors()) {
            model.addAttribute("workspaceId", workspaceId);
            return "project-create";
        }

        try {
            projectService.create(dto, workspaceId, principal.getName());
        } catch (RuntimeException e) {
            model.addAttribute("workspaceId", workspaceId);
            model.addAttribute("error", e.getMessage());
            return "project-create";
        }

        return "redirect:/workspaces/" + workspaceId;
    }

    @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/members")
    public String members(@PathVariable Long workspaceId, @PathVariable Long projectId,
                          Model model, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var project = projectService.getById(projectId);
        var members = projectService.getActiveMembers(projectId);
        model.addAttribute("project", project);
        model.addAttribute("members", members);
        model.addAttribute("memberDto", new WorkspaceMemberDto());
        model.addAttribute("workspaceId", workspaceId);
        model.addAttribute("projectId", projectId);
        return "project-members";
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/members")
    public String addMember(@PathVariable Long workspaceId, @PathVariable Long projectId,
                            @Valid @ModelAttribute("memberDto") WorkspaceMemberDto dto,
                            BindingResult result, RedirectAttributes redirectAttributes,
                            Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid email");
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/members";
        }

        try {
            projectService.addMember(projectId, dto.getEmail(), ProjectRole.MEMBER);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/members";
    }

    @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long workspaceId, @PathVariable Long projectId,
                               @PathVariable Long memberId, Principal principal) {
        workspaceService.checkMembership(workspaceId, principal.getName());
        projectService.removeMember(memberId);
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/members";
    }
}
