package org.project.excelservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.excelservice.entity.Issue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;



@Component
@Slf4j
public class ExcelFileWriter {
    
    /** Répertoire cible configurable : application.properties -> excel.report.dir: reports */
    private final Path reportsDir;

    public ExcelFileWriter(@Value("${excel.reports.dir:reports}") String  dir) {
        this.reportsDir = Paths.get(dir);
    }



    public void append(String projectKey, List<Issue> rows) {
        try {
             // S'assurer que le dossier existe
            Files.createDirectories(reportsDir);

            Path file= reportsDir.resolve(projectKey + ".xlsx");
            Workbook workbook;

            // Ouvrir le workbook existant ou en créer un nouveau
            if(Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)){
                    workbook = WorkbookFactory.create(in);
                }
            }else {
                workbook = new XSSFWorkbook();
            }

            // Récupérer (ou créer) la première feuille
            Sheet sheet = workbook.getNumberOfSheets() > 0
            ? workbook.getSheetAt(0)
            : workbook.createSheet("Issues");

            // Première ligne libre
            int rowIndex = Math.max(sheet.getLastRowNum(), 0) + 1;

            // Écrire chaque issue
            for (Issue issue : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(issue.getId());
                row.createCell(1).setCellValue(issue.getIssueKey());
                row.createCell(2).setCellValue(issue.getSummary());
                row.createCell(3).setCellValue(issue.getStatus());
                row.createCell(4).setCellValue(issue.getCreated());
                row.createCell(5).setCellValue(issue.getUpdated());
                row.createCell(6).setCellValue(issue.getResolved());
                row.createCell(7).setCellValue(issue.getTimeSpentSeconds());
                row.createCell(8).setCellValue(issue.getOrganization());
                row.createCell(9).setCellValue(issue.getClassification());
                row.createCell(10).setCellValue(issue.getEntity());
                row.createCell(11).setCellValue(issue.getIssueQuality());
                row.createCell(12).setCellValue(issue.getMedium());
                row.createCell(13).setCellValue(issue.getTtsDays());
                row.createCell(14).setCellValue(issue.getSite());
                row.createCell(15).setCellValue(issue.getMonth());
                row.createCell(16).setCellValue(issue.getQuotaPerProject());
            }

            // Sauvegarder
            try (OutputStream out = Files.newOutputStream(file
            ,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            ){
                workbook.write(out);
            }

            workbook.close();
            log.info("{} issues appended to {}", rows.size(), file.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Failed to append issues to file " + projectKey, e);
        }
    }
}
