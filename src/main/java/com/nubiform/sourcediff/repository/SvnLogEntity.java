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
@Table(name = "svn_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"repository", "sourceType", "revision", "filePath"})
        })
public class SvnLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repository;

    private String sourceType;

    private Long revision;

    private String filePath;

    private String fileType;

    @Column(length = 500)
    private String message;

    private LocalDateTime commitTime;

    private String author;
}
