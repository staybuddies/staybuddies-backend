package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.FileStorage;
import com.example.SP.senior_project.model.constant.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {
    List<FileStorage> findByTypeAndFileId(FileType type, Long id);
}
