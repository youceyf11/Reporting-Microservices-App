package org.project.excelservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.entity.Issue;
import org.project.excelservice.repository.IssueRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for syncing issues with an Excel file.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelSyncService {

    private final ExcelFileWriter writer;
    private final CheckpointStore checkpointService;
    private final IssueRepository issueRepository;

    /**
     * Syncs issues for the given project key with the Excel file.
     *
     * @param projectKey the project key
     * @return a Mono with a success message or an error
     */
    public Mono<String> sync(String projectKey) {
        // Retrieve the last processed id (defaults to 0 when no checkpoint exists)
        String lastUpdated = checkpointService.getLastUpdated(projectKey); 
        
    
        return issueRepository.findByProjectKeyAndUpdatedAfter(projectKey, lastUpdated)
                // Collect results into a list so we can determine the max id afterward
                .collectList()
                .flatMap(list -> {
                    // Short-circuit and return if no new issues were found
                    if (list.isEmpty()) return Mono.just("Nothing new");

                    return Mono.fromCallable(() -> {
                            // ajouter de nouvelles lignes à la fin du classeur Excel existant, sans toucher aux lignes déjà présentes
                            writer.append(projectKey, list);  
                            // Determine and store the new maximum id for the next run
                            String newMax = list.stream().map(Issue::getUpdated).max(String::compareTo).orElse(lastUpdated);
                            checkpointService.writeLastUpdated(projectKey, newMax);
                            return "Sync completed successfully";
                        })
                        // File I/O is blocking; execute it on a dedicated scheduler
                        .subscribeOn(Schedulers.boundedElastic());
                })
                .doOnError(e -> log.error("Error during sync", e));
    }
}
