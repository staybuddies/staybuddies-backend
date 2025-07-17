package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import com.example.SP.senior_project.model.constant.FileType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "file_storages")
public class FileStorage extends AbstractAuditableEntity {
    private FileType type;
    private Long fileId;
    private String fileName;
    private String key;
    private String contentType;
    private long fileSize;
    private String serviceName;
}

