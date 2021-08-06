package com.nubiform.sourcediff.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "files",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"repository", "filePath"})
        })
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repository;

    private String filePath;

    private String fileType;

    private String fileName;

    private Long parentId;

    private String devFilePath;

    private String prodFilePath;

    private Integer diffCount;

    private LocalDateTime devModified;

    private LocalDateTime prodModified;

    private LocalDateTime diffModified;
}
