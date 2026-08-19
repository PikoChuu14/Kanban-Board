package com.company.kanban.controller;

import com.company.kanban.dto.*; import com.company.kanban.service.DataManagementService;
import org.springframework.core.io.FileSystemResource; import org.springframework.http.ResponseEntity; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("/api/admin/data-management") @PreAuthorize("hasRole('ADMIN')")
public class AdminDataManagementController {
    private final DataManagementService service; public AdminDataManagementController(DataManagementService service){this.service=service;}
    @GetMapping("/backups") public List<BackupResponse> backups(){return service.listBackups();}
    @PostMapping("/backups") public BackupResponse create(){return service.createBackup("MANUAL","Created from Admin Data Management");}
    @GetMapping("/backups/{id:.+}/download") public ResponseEntity<FileSystemResource> download(@PathVariable String id){return service.download(id);}
    @DeleteMapping("/backups/{id:.+}") public void delete(@PathVariable String id){service.delete(id);}
    @GetMapping("/archives") public List<ArchiveResponse> archives(){return service.listArchives();}
    @PostMapping("/restore") public DataManagementStatusResponse restore(@RequestBody RestoreRequest request){return service.queueRestore(request);}
    @GetMapping("/status") public DataManagementStatusResponse status(){return service.status();}
    @GetMapping("/location") public Map<String,Object> location(HttpServletRequest request){return Map.of("backupDirectory",service.backupDirectory(),"canOpenFolder",service.canOpenBackupFolder(request.getRemoteAddr()));}
    @PostMapping("/open-folder") public void openFolder(HttpServletRequest request){service.openBackupFolder(request.getRemoteAddr());}
}
