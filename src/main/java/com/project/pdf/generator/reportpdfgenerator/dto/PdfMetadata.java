package com.project.pdf.generator.reportpdfgenerator.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PdfMetadata {
    int normalPagesPerSlice;
    int numColumnsPerPage;
    int numColumnsInRemainderPagePerSlice;
    int pageNumber;
}
