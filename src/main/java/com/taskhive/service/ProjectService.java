package com.taskhive.service;

import com.taskhive.dto.ProjectDto;
import com.taskhive.model.*;
import com.taskhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public Project create(ProjectDto dto, Long workspaceId, String email) {
        var creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));

        if (projectRepository.existsByWorkspaceWorkspaceIdAndProjectKey(workspaceId, dto.getProjectKey())) {
            throw new RuntimeException("Project key already exists in this workspace");
        }

        var project = Project.builder()
                .name(dto.getName())
                .projectKey(dto.getProjectKey())
                .description(dto.getDescription())
                .creator(creator)
                .workspace(workspace)
                .build();

        project = projectRepository.save(project);
        addCreatorAsMember(project, creator);

        return project;
    }

    @Transactional(readOnly = true)
    public Project getById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Transactional(readOnly = true)
    public List<Project> getProjectsByWorkspace(Long workspaceId) {
        return projectRepository.findByWorkspaceWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMember> getActiveMembers(Long projectId) {
        var project = getById(projectId);
        return projectMemberRepository.findByProjectAndValidToIsNull(project);
    }

    @Transactional
    @CacheEvict(value = "workspaceMembers", allEntries = true)
    public void addMember(Long projectId, String email, ProjectRole role) {
        var project = getById(projectId);
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Auto-add to workspace if not already a member
        var workspace = project.getWorkspace();
        boolean alreadyWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceAndUserAndValidToIsNull(workspace, user)
                .isPresent();
        if (!alreadyWorkspaceMember) {
            workspaceMemberRepository.save(WorkspaceMember.builder()
                    .workspace(workspace)
                    .user(user)
                    .role(WorkspaceRole.MEMBER)
                    .status(WorkspaceMemberStatus.ACTIVE)
                    .build());
        }

        var alreadyMember = projectMemberRepository
                .findByProjectAndUserAndValidToIsNull(project, user)
                .isPresent();

        if (alreadyMember) {
            throw new RuntimeException("User is already a project member");
        }

        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(user)
                .role(role)
                .status(ProjectMemberStatus.ACTIVE)
                .build());
    }

    @Transactional
    public void removeMember(Long memberId) {
        var member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        member.setStatus(ProjectMemberStatus.REMOVED);
        member.setValidTo(Instant.now());
        projectMemberRepository.save(member);
    }

    private void addCreatorAsMember(Project project, User creator) {
        var member = ProjectMember.builder()
                .project(project)
                .user(creator)
                .role(ProjectRole.ADMIN)
                .status(ProjectMemberStatus.ACTIVE)
                .build();
        projectMemberRepository.save(member);
    }
}
