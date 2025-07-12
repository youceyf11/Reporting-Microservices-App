package org.project.jirafetchservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.dto.JiraIssueDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JiraMapper {

    // API → DTO simple
    @Mapping(source = "key", target = "issueKey")
    @Mapping(source = "fields.summary", target = "summary")
    @Mapping(source = "fields.issuetype.name", target = "issueType")
    @Mapping(source = "fields.status.name", target = "status")
    @Mapping(source = "fields.priority.name", target = "priority")
    @Mapping(source = "fields.resolution.name", target = "resolution")
    @Mapping(source = "fields.assignee.displayName", target = "assignee")
    @Mapping(source = "fields.reporter.displayName", target = "reporter")
    @Mapping(source = "fields.organization", target = "organization")
    @Mapping(source = "fields.created", target = "created", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.updated", target = "updated", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.resolved", target = "resolved", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
    @Mapping(source = "fields.classification", target = "classification")
    @Mapping(source = "fields.entity", target = "entity")
    @Mapping(source = "fields.issueQuality", target = "issueQuality")
    @Mapping(source = "fields.medium", target = "medium")
    @Mapping(source = "fields.ttsDays", target = "ttsDays")
    @Mapping(source = "fields.site", target = "site")
    @Mapping(source = "fields.month", target = "month")
    @Mapping(source = "fields.quotaPerProject", target = "quotaPerProject")
    IssueSimpleDto toSimpleDtoFromApi(JiraIssueApiResponse apiResponse);

    @Named("parseJiraDate")
    default LocalDateTime parseJiraDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
            return OffsetDateTime.parse(dateString, formatter).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Impossible de parser la date: " + dateString, e);
        }
    }

    // API → DTO complet
    JiraIssueDto toJiraIssueDto(JiraIssueApiResponse apiResponse);

    // API → Entité DB
    JiraIssueDbEntity toDbEntityFromApi(JiraIssueApiResponse apiResponse);

    // Entité DB → DTO simple
    IssueSimpleDto toSimpleDtoFromDb(JiraIssueDbEntity dbEntity);

    // DTO simple → Entité DB
    JiraIssueDbEntity toDbEntityFromSimpleDto(IssueSimpleDto dto);

    // Méthodes pour les listes
    List<IssueSimpleDto> toSimpleDtoListFromApi(List<JiraIssueApiResponse> apiList);
    List<JiraIssueDto> toJiraIssueDtoList(List<JiraIssueApiResponse> apiList);
    List<JiraIssueDbEntity> toDbEntityListFromApi(List<JiraIssueApiResponse> apiList);
    List<IssueSimpleDto> toSimpleDtoListFromDb(List<JiraIssueDbEntity> dbList);
}