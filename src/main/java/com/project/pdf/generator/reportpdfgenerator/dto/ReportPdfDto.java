package com.project.pdf.generator.reportpdfgenerator.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ReportPdfDto {
    private String csvFileTitle;
    private String startDate;
    private String endDate;
    private List<String> fileNames;
    private String fileDelimiter;
    private int recordCount;
    private int recordLimitPerPage;
    private int columnLimitPerPage;
}
