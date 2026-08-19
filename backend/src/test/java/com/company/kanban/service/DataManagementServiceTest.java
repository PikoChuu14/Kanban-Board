package com.company.kanban.service;

import org.junit.jupiter.api.*; import org.junit.jupiter.api.io.TempDir; import org.springframework.jdbc.datasource.DriverManagerDataSource; import org.springframework.web.server.ResponseStatusException; import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*; import static org.junit.jupiter.api.Assertions.*;

class DataManagementServiceTest {
    @TempDir Path temp;
    private DataManagementService service(){return new DataManagementService(JsonMapper.builder().findAndAddModules().build(),new DriverManagerDataSource("jdbc:h2:mem:data-management-test","sa",""),temp.toString(),"jdbc:postgresql://localhost:5432/kanban_db","user","secret","","",false);}
    @Test void configurableDirectoryListsBackupMetadata()throws Exception{Path file=temp.resolve("flowops_20260819_104200.backup");Files.write(file,new byte[]{1,2,3});var backups=service().listBackups();assertEquals(1,backups.size());assertEquals(3,backups.getFirst().sizeBytes());assertEquals("PRE_NEW_DATABASE",backups.getFirst().backupType());}
    @Test void traversalAndMissingBackupAreRejected(){DataManagementService service=service();assertEquals(400,assertThrows(ResponseStatusException.class,()->service.download("../secret.txt")).getStatusCode().value());assertEquals(404,assertThrows(ResponseStatusException.class,()->service.download("missing.backup")).getStatusCode().value());}
    @Test void restoreRequiresExplicitConfirmation(){var ex=assertThrows(ResponseStatusException.class,()->service().queueRestore(new com.company.kanban.dto.RestoreRequest("anything.backup","NO")));assertEquals(400,ex.getStatusCode().value());}
}
