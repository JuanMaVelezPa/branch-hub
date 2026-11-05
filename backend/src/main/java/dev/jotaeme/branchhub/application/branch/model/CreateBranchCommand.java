package dev.jotaeme.branchhub.application.branch.model;

public record CreateBranchCommand(
        String code,
        String name,
        String city) {
}
