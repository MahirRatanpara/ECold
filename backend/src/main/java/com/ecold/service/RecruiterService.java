package com.ecold.service;

import com.ecold.dto.RecruiterContactDto;
import com.ecold.dto.RecruiterImportRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RecruiterService {
    Page<RecruiterContactDto> getRecruiters(int page, int size, String status, String search, String company);

    // Backward compatibility methods
    default Page<RecruiterContactDto> getRecruiters(int page, int size, String status, String search) {
        return getRecruiters(page, size, status, search, null);
    }

    default Page<RecruiterContactDto> getRecruiters(int page, int size, String status) {
        return getRecruiters(page, size, status, null, null);
    }

    RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto);
    RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto, String templateId);
    RecruiterContactDto getRecruiterById(String id);
    RecruiterContactDto updateRecruiter(String id, RecruiterContactDto recruiterDto);
    void deleteRecruiter(String id);

    // Contact tracking
    RecruiterContactDto markAsContacted(String id);

    List<RecruiterContactDto> importFromCsv(MultipartFile file);
    List<RecruiterContactDto> importFromCsv(MultipartFile file, String templateId);
    List<RecruiterContactDto> importFromExcel(MultipartFile file);
    List<RecruiterContactDto> importManual(List<RecruiterContactDto> recruiters);
    List<RecruiterContactDto> importManualFromCsv(String csvData);
    Object getRecruiterStats();
    List<RecruiterContactDto> getUncontactedRecruiters();

    // Bulk operations
    void bulkDeleteRecruiters(List<String> ids);
    List<RecruiterContactDto> bulkUpdateStatus(List<String> ids, String status);
}