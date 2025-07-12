package org.project.jirafetchservice.service;

import org.project.jirafetchservice.client.JiraWebClient;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.repository.JiraIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class JiraIssueService {

    private final JiraWebClient jiraWebClient;
    private final JiraIssueRepository jiraIssueRepository;
    private final JiraMapper jiraMapper;

    @Autowired
    public JiraIssueService(JiraWebClient jiraWebClient,
                            JiraIssueRepository jiraIssueRepository,
                            JiraMapper jiraMapper) {
        this.jiraWebClient = jiraWebClient;
        this.jiraIssueRepository = jiraIssueRepository;
        this.jiraMapper = jiraMapper;
    }

    // ================== MÉTHODES DE CONSULTATION (API uniquement) ==================

    public Mono<JiraIssueApiResponse> getIssue(String issueKey) {
        return jiraWebClient.getIssue(issueKey)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Issue non trouvée : " + issueKey)));
    }


    public Flux<JiraIssueApiResponse> getProjectIssues(String projectKey) {
        return jiraWebClient.searchProjectIssues(projectKey)
                .switchIfEmpty(Flux.error(new IllegalArgumentException("Aucune issue pour le projet : " + projectKey)));
    }


    public Flux<JiraIssueApiResponse> getIssuesAssignedTo(String assigneeEmail) {
        String jql = "assignee = '" + assigneeEmail + "'";
        return searchIssues(jql);
    }


    public Flux<JiraIssueApiResponse> searchIssues(String jql) {
        return jiraWebClient.searchIssues(jql)
                .flatMapMany(response -> Flux.fromIterable(response.getIssues()));
    }

    // ================== MÉTHODES DE SYNCHRONISATION OPTIMISÉES ==================

    /* public Mono<IssueSimpleDto> synchroniserIssueAvecJira(String issueKey) {
        return jiraIssueRepository.findByIssueKey(issueKey)
                .next()
                .flatMap(existingEntity -> {
                    // Vérifie si l'issue a été modifiée récemment dans Jira
                    if (existingEntity.getUpdated() != null &&
                        isRecentlyUpdatedInJira(existingEntity.getUpdated())) {
                        return Mono.just(jiraMapper.toSimpleDtoFromDb(existingEntity));
                    }
                    return synchroniserDepuisJira(issueKey);
                })
                .switchIfEmpty(synchroniserDepuisJira(issueKey));
    } */

    public Mono<IssueSimpleDto> synchroniserIssueAvecJira(String issueKey) {
        return jiraIssueRepository.findByIssueKey(issueKey)
                .next()
                .flatMap(existingEntity -> {
                    if (existingEntity.getUpdated() != null &&
                            isRecentlyUpdatedInJira(existingEntity.getUpdated())) {
                        return Mono.just(jiraMapper.toSimpleDtoFromDb(existingEntity));
                    }
                    return synchroniserDepuisJira(issueKey);
                })
                .switchIfEmpty(synchroniserDepuisJira(issueKey))
                .doOnError(error -> {
                    System.err.println(" Erreur sync " + issueKey + ": " + error.getMessage());
                    error.printStackTrace();
                })
                .onErrorResume(error -> {
                    // Fallback : essayer de récupérer depuis Jira directement
                    return jiraWebClient.getIssue(issueKey)
                            .map(jiraMapper::toSimpleDtoFromApi)
                            .onErrorReturn(createErrorDto(issueKey, error.getMessage()));
                });
    }

    private IssueSimpleDto createErrorDto(String issueKey, String errorMessage) {
        IssueSimpleDto errorDto = new IssueSimpleDto();
        errorDto.setIssueKey(issueKey);
        errorDto.setSummary("ERREUR: " + errorMessage);
        errorDto.setStatus("ERROR");
        return errorDto;
    }

    private Mono<IssueSimpleDto> synchroniserDepuisJira(String issueKey) {
        return jiraWebClient.getIssue(issueKey)
                .map(jiraMapper::toSimpleDtoFromApi)
                .flatMap(issueDto -> {
                    JiraIssueDbEntity entity = jiraMapper.toDbEntityFromSimpleDto(issueDto);
                    return jiraIssueRepository.save(entity)
                            .map(savedEntity -> issueDto);
                });
    }


    public Flux<IssueSimpleDto> synchroniserProjetAvecJira(String projectKey, int batchSize) {
        return jiraWebClient.searchProjectIssues(projectKey)
                .buffer(batchSize)
                .flatMap(issueBatch -> {
                    List<IssueSimpleDto> issueDtos = issueBatch.stream()
                            .map(jiraMapper::toSimpleDtoFromApi)
                            .toList();

                    return filtrerIssuesObsoletes(issueDtos)
                            .collectList()
                            .flatMapMany(issuesToSync -> {
                                if (issuesToSync.isEmpty()) {
                                    return Flux.empty();
                                }

                                List<JiraIssueDbEntity> entities = issuesToSync.stream()
                                        .map(jiraMapper::toDbEntityFromSimpleDto)
                                        .toList();

                                return jiraIssueRepository.saveAll(entities)
                                        .map(jiraMapper::toSimpleDtoFromDb);
                            });
                })
                .switchIfEmpty(Flux.error(new IllegalArgumentException("Aucune issue pour le projet : " + projectKey)));
    }

    public Flux<IssueSimpleDto> synchroniserSearchAvecJira(String jql) {
        return jiraWebClient.searchIssues(jql)
                .flatMapMany(response -> Flux.fromIterable(response.getIssues()))
                .map(jiraMapper::toSimpleDtoFromApi)
                .flatMap(issueDto -> {
                    JiraIssueDbEntity entity = jiraMapper.toDbEntityFromSimpleDto(issueDto);
                    return jiraIssueRepository.save(entity)
                            .map(savedEntity -> issueDto);
                });
    }

    // Méthode utilitaire pour vérifier si une issue Jira est récente
    private boolean isRecentlyUpdatedInJira(String updatedStr) {
        try {
            // Parse le format Jira : "2024-01-15T10:30:45.000+0000"
            LocalDateTime updated = LocalDateTime.parse(
                updatedStr.substring(0, 19),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            return updated.isAfter(LocalDateTime.now().minusHours(1));
        } catch (Exception e) {
            return false; // Si parsing échoue, considère comme obsolète
        }
    }

    // Filtre les issues qui ne nécessitent pas de mise à jour
    private Flux<IssueSimpleDto> filtrerIssuesObsoletes(List<IssueSimpleDto> issues) {
        List<String> issueKeys = issues.stream()
                .map(IssueSimpleDto::getIssueKey)
                .toList();

        return jiraIssueRepository.findByIssueKeyIn(issueKeys)
                .collectList()
                .flatMapMany(existingEntities -> {
                    List<String> recentKeys = existingEntities.stream()
                            .filter(entity -> entity.getUpdated() != null &&
                                    isRecentlyUpdatedInJira(entity.getUpdated()))
                            .map(JiraIssueDbEntity::getIssueKey)
                            .toList();

                    return Flux.fromIterable(issues)
                            .filter(issue -> !recentKeys.contains(issue.getIssueKey()));
                });
    }


    public Flux<String> getAllLocalProjectKeys() {
        return jiraIssueRepository.findByProjectKeyIsNotNull()
                .map(JiraIssueDbEntity::getProjectKey)
                .distinct()
                .onErrorResume(error -> {
                    System.err.println("Erreur lors de la récupération des projets locaux: " + error.getMessage());  // ✅ Un seul paramètre
                    return Flux.empty();
                });
    }

    public Mono<String> getProjectKeyFromIssue(String issueKey) {
        if (issueKey == null || !issueKey.contains("-")) {
            return Mono.error(new IllegalArgumentException("Format d'issue key invalide"));
        }
        return Mono.just(issueKey.split("-")[0]);
    }

    // ================== MÉTHODES DB LOCALE ==================

    public Mono<IssueSimpleDto> recupererIssueLocale(String issueKey) {
        return jiraIssueRepository.findByIssueKey(issueKey)
                .next()
                .map(jiraMapper::toSimpleDtoFromDb)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Issue non trouvée en base : " + issueKey)));
    }
}