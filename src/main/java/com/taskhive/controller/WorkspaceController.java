package com.taskhive.controller;

import com.taskhive.dto.WorkspaceDto;
import com.taskhive.dto.WorkspaceMemberDto;
import com.taskhive.model.WorkspaceRole;
import com.taskhive.service.WorkspaceService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/workspaces")
    public String list(Model model, Principal principal, HttpSession session,
                       HttpServletRequest request) {
        var workspaces = workspaceService.getWorkspacesByEmail(principal.getName());
        model.addAttribute("workspaces", workspaces);

        // SESSION: show "continue where you left off" if last workspace exists
        var lastId = (Long) session.getAttribute("lastWorkspaceId");
        var lastName = (String) session.getAttribute("lastWorkspaceName");
        if (lastId != null && lastName != null) {
            model.addAttribute("lastWorkspaceId", lastId);
            model.addAttribute("lastWorkspaceName", lastName);
        }

        // COOKIE: read items-per-page preference
        var itemsPerPage = readCookie(request, "itemsPerPage", "10");
        model.addAttribute("itemsPerPage", itemsPerPage);

        return "workspaces";
    }

    @GetMapping("/workspaces/create")
    public String showCreateForm(Model model) {
        model.addAttribute("workspaceDto", new WorkspaceDto());
        return "workspace-create";
    }

    @PostMapping("/workspaces/create")
    public String create(@Valid @ModelAttribute WorkspaceDto dto, BindingResult result, Principal principal) {
        if (result.hasErrors()) {
            return "workspace-create";
        }
        workspaceService.create(dto, principal.getName());
        return "redirect:/workspaces";
    }

    @GetMapping("/workspaces/{workspaceId}/members")
    public String members(@PathVariable Long workspaceId, Model model, Principal principal, HttpSession session) {
        workspaceService.checkMembership(workspaceId, principal.getName());

        var workspace = workspaceService.getById(workspaceId);
        var members = workspaceService.getActiveMembers(workspaceId);
        var currentMembership = workspaceService.getMembership(workspaceId, principal.getName());

        // SESSION: remember last visited workspace
        session.setAttribute("lastWorkspaceId", workspace.getWorkspaceId());
        session.setAttribute("lastWorkspaceName", workspace.getName());

        model.addAttribute("workspace", workspace);
        model.addAttribute("members", members);
        model.addAttribute("memberDto", new WorkspaceMemberDto());
        model.addAttribute("workspaceId", workspaceId);
        model.addAttribute("isAdmin", currentMembership != null
                && currentMembership.getRole() == WorkspaceRole.ADMIN);
        return "workspace-members";
    }

    @PostMapping("/workspaces/{workspaceId}/members")
    public String addMember(@PathVariable Long workspaceId,
                            @Valid @ModelAttribute("memberDto") WorkspaceMemberDto dto,
                            BindingResult result, RedirectAttributes redirectAttributes,
                            Principal principal) {

        var membership = workspaceService.getMembership(workspaceId, principal.getName());
        if (membership == null || membership.getRole() != WorkspaceRole.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Only admins can add members");
            return "redirect:/workspaces/" + workspaceId + "/members";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid email");
            return "redirect:/workspaces/" + workspaceId + "/members";
        }

        try {
            workspaceService.addMember(workspaceId, dto.getEmail());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workspaces/" + workspaceId + "/members";
    }

    @PostMapping("/workspaces/{workspaceId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long workspaceId, @PathVariable Long memberId,
                               Principal principal, RedirectAttributes redirectAttributes) {

        var membership = workspaceService.getMembership(workspaceId, principal.getName());
        if (membership == null || membership.getRole() != WorkspaceRole.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Only admins can remove members");
            return "redirect:/workspaces/" + workspaceId + "/members";
        }

        workspaceService.removeMember(memberId, workspaceId);
        return "redirect:/workspaces/" + workspaceId + "/members";
    }

    // COOKIE: endpoint to set items per page preference
    @PostMapping("/workspaces/preferences")
    public String setPreferences(@RequestParam String itemsPerPage, HttpServletResponse response) {
        var cookie = new Cookie("itemsPerPage", itemsPerPage);
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/workspaces";
    }

    private String readCookie(HttpServletRequest request, String name, String defaultValue) {
        if (request.getCookies() == null) return defaultValue;
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(defaultValue);
    }
}
